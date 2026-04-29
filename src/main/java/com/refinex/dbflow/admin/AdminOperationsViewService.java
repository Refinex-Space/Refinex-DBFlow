package com.refinex.dbflow.admin;

import com.refinex.dbflow.access.service.ConfiguredEnvironmentView;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.audit.service.*;
import com.refinex.dbflow.config.DangerousDdlDecision;
import com.refinex.dbflow.config.DangerousDdlOperation;
import com.refinex.dbflow.config.DbflowProperties;
import com.refinex.dbflow.executor.ProjectEnvironmentDataSourceRegistry;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 管理端运维页面视图服务，聚合审计、策略和健康状态的只读展示模型。
 *
 * @author refinex
 */
@Service
public class AdminOperationsViewService {

    /**
     * 管理端分页上限。
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 默认审计分页大小。
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 管理端时间格式。
     */
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * 审计查询服务。
     */
    private final AuditQueryService auditQueryService;

    /**
     * DBFlow 配置属性。
     */
    private final DbflowProperties dbflowProperties;

    /**
     * 元数据库数据源。
     */
    private final DataSource metadataDataSource;

    /**
     * 项目环境目录服务。
     */
    private final ProjectEnvironmentCatalogService catalogService;

    /**
     * 目标库数据源注册表。
     */
    private final ProjectEnvironmentDataSourceRegistry targetRegistry;

    /**
     * Spring 环境属性。
     */
    private final Environment springEnvironment;

    /**
     * 创建管理端运维页面视图服务。
     *
     * @param auditQueryService  审计查询服务
     * @param dbflowProperties   DBFlow 配置属性
     * @param metadataDataSource 元数据库数据源
     * @param catalogService     项目环境目录服务
     * @param targetRegistry     目标库数据源注册表
     * @param springEnvironment  Spring 环境属性
     */
    public AdminOperationsViewService(
            AuditQueryService auditQueryService,
            DbflowProperties dbflowProperties,
            DataSource metadataDataSource,
            ProjectEnvironmentCatalogService catalogService,
            ProjectEnvironmentDataSourceRegistry targetRegistry,
            Environment springEnvironment
    ) {
        this.auditQueryService = Objects.requireNonNull(auditQueryService);
        this.dbflowProperties = Objects.requireNonNull(dbflowProperties);
        this.metadataDataSource = Objects.requireNonNull(metadataDataSource);
        this.catalogService = Objects.requireNonNull(catalogService);
        this.targetRegistry = Objects.requireNonNull(targetRegistry);
        this.springEnvironment = Objects.requireNonNull(springEnvironment);
    }

    /**
     * 创建审计列表页视图。
     *
     * @param criteria 审计查询条件
     * @return 审计列表页视图
     */
    public AuditPageView auditPage(AuditQueryCriteria criteria) {
        AuditQueryCriteria safeCriteria = normalizeCriteria(criteria);
        AuditEventPageResponse<AuditEventSummary> page = auditQueryService.query(safeCriteria);
        AuditFilterView filter = toFilterView(safeCriteria);
        return new AuditPageView(
                page.content().stream().map(this::toAuditSummaryRow).toList(),
                filter,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.sort(),
                page.direction(),
                page.page() > 0,
                page.page() + 1 < page.totalPages(),
                Math.max(0, page.page() - 1),
                page.page() + 1,
                queryForPage(filter, Math.max(0, page.page() - 1)),
                queryForPage(filter, page.page() + 1),
                firstItem(page),
                lastItem(page)
        );
    }

    /**
     * 将审计摘要转换为页面行。
     *
     * @param summary 审计摘要
     * @return 页面行
     */
    private AuditSummaryRow toAuditSummaryRow(AuditEventSummary summary) {
        return new AuditSummaryRow(
                summary.id(),
                displayTime(summary.createdAt()),
                displayText(summary.userId()),
                summary.projectKey(),
                summary.environmentKey(),
                summary.tool(),
                summary.operationType(),
                summary.riskLevel(),
                toneForRisk(summary.riskLevel()),
                summary.status(),
                summary.decision(),
                toneForDecision(summary.decision()),
                summary.sqlHash(),
                displayText(summary.resultSummary()),
                displayText(summary.affectedRows())
        );
    }

    /**
     * 创建审计详情页视图。
     *
     * @param id 审计事件主键
     * @return 审计详情页视图
     */
    public AuditDetailPageView auditDetail(Long id) {
        AuditEventDetail detail = auditQueryService.getDetail(id);
        return new AuditDetailPageView(
                detail,
                displayTime(detail.createdAt()),
                displayText(detail.userId()),
                displayText(detail.affectedRows()),
                displayText(detail.confirmationId()),
                failureReason(detail)
        );
    }

    /**
     * 创建危险策略只读页视图。
     *
     * @return 危险策略页视图
     */
    public DangerousPolicyPageView dangerousPolicyPage() {
        DbflowProperties.DangerousDdl dangerousDdl = dbflowProperties.getPolicies().getDangerousDdl();
        List<PolicyDefaultRow> defaults = dangerousDdl.getDefaults()
                .entrySet()
                .stream()
                .sorted((left, right) -> left.getKey().name().compareTo(right.getKey().name()))
                .map(entry -> new PolicyDefaultRow(
                        entry.getKey().name(),
                        riskFor(entry.getKey()),
                        entry.getValue().name(),
                        requirementFor(entry.getKey(), entry.getValue()),
                        toneForDecision(entry.getValue())
                ))
                .toList();
        List<PolicyWhitelistRow> whitelist = dangerousDdl.getWhitelist()
                .stream()
                .map(entry -> new PolicyWhitelistRow(
                        entry.getOperation().name(),
                        riskFor(entry.getOperation()),
                        entry.getProjectKey(),
                        entry.getEnvironmentKey(),
                        entry.getSchemaName(),
                        displayText(entry.getTableName()),
                        entry.isAllowProdDangerousDdl() ? "YES" : "NO",
                        entry.isAllowProdDangerousDdl() ? "允许 prod 命中该白名单" : "prod 命中后仍拒绝",
                        entry.isAllowProdDangerousDdl() ? "ok" : "warn"
                ))
                .toList();
        return new DangerousPolicyPageView(
                defaults,
                whitelist,
                List.of(
                        new PolicyRuleRow("DROP 白名单", "ENFORCED", "DROP_DATABASE 与 DROP_TABLE 默认拒绝",
                                "必须命中 YAML/Nacos 白名单；未命中时不执行 SQL，只写 POLICY_DENIED 审计。", "bad"),
                        new PolicyRuleRow("TRUNCATE confirmation", "ENFORCED", "TRUNCATE 默认创建服务端确认挑战",
                                "目标 DML 不在 explain 或确认创建阶段实际执行，确认挑战默认有效期 5 分钟。", "warn"),
                        new PolicyRuleRow("prod 强化", "ENFORCED", "生产环境需要显式放行",
                                "prod/production 环境命中 DROP 白名单后仍要求 allow-prod-dangerous-ddl=true。", "bad")
                ),
                whitelist.isEmpty() ? "当前无 DROP 白名单条目，DROP_DATABASE / DROP_TABLE 将按默认策略拒绝。" : "当前展示 YAML/Nacos 生效白名单，页面为只读。"
        );
    }

    /**
     * 创建系统健康页视图。
     *
     * @return 系统健康页视图
     */
    public HealthPageView healthPage() {
        List<HealthItem> items = new ArrayList<>();
        items.add(applicationHealth());
        items.add(mcpEndpointHealth());
        items.add(metadataHealth());
        items.add(nacosHealth());
        items.addAll(targetPoolHealth());
        long unhealthy = items.stream().filter(item -> !"HEALTHY".equals(item.status())).count();
        String overall = unhealthy == 0 ? "HEALTHY" : "DEGRADED";
        return new HealthPageView(
                overall,
                toneForHealth(overall),
                items.size(),
                unhealthy,
                items
        );
    }

    /**
     * 规范化审计查询条件。
     *
     * @param criteria 原始查询条件
     * @return 安全查询条件
     */
    private AuditQueryCriteria normalizeCriteria(AuditQueryCriteria criteria) {
        AuditQueryCriteria safeCriteria = criteria == null
                ? new AuditQueryCriteria(null, null, null, null, null, null, null, null, null,
                0, DEFAULT_PAGE_SIZE, "createdAt", "desc")
                : criteria;
        int page = safeCriteria.page() == null || safeCriteria.page() < 0 ? 0 : safeCriteria.page();
        int size = safeCriteria.size() == null || safeCriteria.size() <= 0
                ? DEFAULT_PAGE_SIZE
                : Math.min(safeCriteria.size(), MAX_PAGE_SIZE);
        return new AuditQueryCriteria(
                safeCriteria.from(),
                safeCriteria.to(),
                safeCriteria.userId(),
                safeCriteria.projectKey(),
                safeCriteria.environmentKey(),
                safeCriteria.riskLevel(),
                safeCriteria.decision(),
                safeCriteria.sqlHash(),
                safeCriteria.tool(),
                page,
                size,
                textOrDefault(safeCriteria.sort(), "createdAt"),
                textOrDefault(safeCriteria.direction(), "desc")
        );
    }

    /**
     * 将查询条件转换为页面筛选值。
     *
     * @param criteria 查询条件
     * @return 页面筛选值
     */
    private AuditFilterView toFilterView(AuditQueryCriteria criteria) {
        return new AuditFilterView(
                instantText(criteria.from()),
                instantText(criteria.to()),
                criteria.userId() == null ? "" : criteria.userId().toString(),
                textOrEmpty(criteria.projectKey()),
                textOrEmpty(criteria.environmentKey()),
                textOrEmpty(criteria.riskLevel()),
                textOrEmpty(criteria.decision()),
                textOrEmpty(criteria.sqlHash()),
                textOrEmpty(criteria.tool()),
                criteria.size().toString(),
                criteria.sort(),
                criteria.direction(),
                hasAnyFilter(criteria)
        );
    }

    /**
     * 判断是否存在筛选条件。
     *
     * @param criteria 查询条件
     * @return 存在筛选条件时返回 true
     */
    private boolean hasAnyFilter(AuditQueryCriteria criteria) {
        return criteria.from() != null
                || criteria.to() != null
                || criteria.userId() != null
                || StringUtils.hasText(criteria.projectKey())
                || StringUtils.hasText(criteria.environmentKey())
                || StringUtils.hasText(criteria.riskLevel())
                || StringUtils.hasText(criteria.decision())
                || StringUtils.hasText(criteria.sqlHash())
                || StringUtils.hasText(criteria.tool());
    }

    /**
     * 创建指定页码的筛选链接。
     *
     * @param filter 页面筛选值
     * @param page   页码
     * @return 审计列表链接
     */
    private String queryForPage(AuditFilterView filter, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/audit")
                .queryParam("page", page)
                .queryParam("size", filter.size())
                .queryParam("sort", filter.sort())
                .queryParam("direction", filter.direction());
        queryParam(builder, "from", filter.from());
        queryParam(builder, "to", filter.to());
        queryParam(builder, "userId", filter.userId());
        queryParam(builder, "project", filter.project());
        queryParam(builder, "env", filter.env());
        queryParam(builder, "risk", filter.risk());
        queryParam(builder, "decision", filter.decision());
        queryParam(builder, "sqlHash", filter.sqlHash());
        queryParam(builder, "tool", filter.tool());
        return builder.build().toUriString();
    }

    /**
     * 在存在文本时添加 URL 参数。
     *
     * @param builder URL 构造器
     * @param name    参数名
     * @param value   参数值
     */
    private void queryParam(UriComponentsBuilder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.queryParam(name, value);
        }
    }

    /**
     * 计算当前页第一条序号。
     *
     * @param page 分页响应
     * @return 第一条序号
     */
    private long firstItem(AuditEventPageResponse<AuditEventSummary> page) {
        return page.totalElements() == 0 ? 0 : (long) page.page() * page.size() + 1;
    }

    /**
     * 计算当前页最后一条序号。
     *
     * @param page 分页响应
     * @return 最后一条序号
     */
    private long lastItem(AuditEventPageResponse<AuditEventSummary> page) {
        return page.totalElements() == 0 ? 0 : Math.min(page.totalElements(), (long) (page.page() + 1) * page.size());
    }

    /**
     * 创建应用进程健康项。
     *
     * @return 健康项
     */
    private HealthItem applicationHealth() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMb = runtime.maxMemory() / 1024 / 1024;
        return new HealthItem("应用进程", "runtime", "HEALTHY", "Spring Boot 管理端进程可用",
                "JVM memory used=" + usedMb + "MB max=" + maxMb + "MB", "ok");
    }

    /**
     * 创建 MCP endpoint 健康项。
     *
     * @return 健康项
     */
    private HealthItem mcpEndpointHealth() {
        boolean enabled = springEnvironment.getProperty("spring.ai.mcp.server.enabled", Boolean.class, true);
        String endpoint = springEnvironment.getProperty(
                "spring.ai.mcp.server.streamable-http.mcp-endpoint", "/mcp");
        String version = springEnvironment.getProperty("spring.ai.mcp.server.version", "unknown");
        String status = enabled ? "HEALTHY" : "DISABLED";
        return new HealthItem("MCP Streamable HTTP", "mcp", status,
                enabled ? endpoint + " endpoint 已配置" : "MCP server 当前禁用",
                "version=" + version + " endpoint=" + endpoint, toneForHealth(status));
    }

    /**
     * 创建元数据库健康项。
     *
     * @return 健康项
     */
    private HealthItem metadataHealth() {
        try (Connection connection = metadataDataSource.getConnection()) {
            boolean valid = connection.isValid(1);
            DatabaseMetaData metaData = connection.getMetaData();
            String status = valid ? "HEALTHY" : "DEGRADED";
            return new HealthItem("元数据库", "metadata-db", status,
                    metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion(),
                    "schema managed by Flyway; credentials hidden", toneForHealth(status));
        } catch (Exception exception) {
            return new HealthItem("元数据库", "metadata-db", "DOWN", "元数据库连接失败",
                    sanitize(exception.getMessage()), "bad");
        }
    }

    /**
     * 创建 Nacos 健康项。
     *
     * @return 健康项
     */
    private HealthItem nacosHealth() {
        boolean configEnabled = springEnvironment.getProperty("spring.cloud.nacos.config.enabled", Boolean.class, false);
        boolean discoveryEnabled = springEnvironment.getProperty("spring.cloud.nacos.discovery.enabled", Boolean.class, false);
        String status = configEnabled || discoveryEnabled ? "HEALTHY" : "DISABLED";
        String namespace = springEnvironment.getProperty("spring.cloud.nacos.config.namespace", "default");
        return new HealthItem("Nacos", "nacos", status,
                "config=" + enabledText(configEnabled) + " discovery=" + enabledText(discoveryEnabled),
                "namespace=" + namespace + "; credentials hidden", toneForHealth(status));
    }

    /**
     * 创建目标项目环境连接池健康项。
     *
     * @return 健康项列表
     */
    private List<HealthItem> targetPoolHealth() {
        List<HealthItem> items = new ArrayList<>();
        List<ConfiguredEnvironmentView> environments = catalogService.listConfiguredEnvironments();
        if (environments.isEmpty()) {
            items.add(new HealthItem("项目环境连接池", "target-pool", "DISABLED",
                    "当前未配置目标项目环境", "dbflow.projects 为空", "neutral"));
            return items;
        }
        for (ConfiguredEnvironmentView environmentView : environments) {
            items.add(targetPoolHealth(environmentView));
        }
        return items;
    }

    /**
     * 创建单个目标连接池健康项。
     *
     * @param environmentView 项目环境视图
     * @return 健康项
     */
    private HealthItem targetPoolHealth(ConfiguredEnvironmentView environmentView) {
        String name = environmentView.projectKey() + " / " + environmentView.environmentKey();
        try {
            DataSource dataSource = targetRegistry.getDataSource(
                    environmentView.projectKey(),
                    environmentView.environmentKey()
            );
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                String status = hikariDataSource.isClosed() ? "DOWN" : "HEALTHY";
                return new HealthItem(name, "target-pool", status,
                        "Hikari 连接池已注册，driver=" + displayText(environmentView.driverClassName()),
                        hikariDetail(hikariDataSource), toneForHealth(status));
            }
            return new HealthItem(name, "target-pool", "HEALTHY",
                    "目标数据源已注册", "type=" + dataSource.getClass().getSimpleName(), "ok");
        } catch (RuntimeException exception) {
            return new HealthItem(name, "target-pool", "DEGRADED",
                    "目标数据源未就绪或不可用", sanitize(exception.getMessage()), "warn");
        }
    }

    /**
     * 创建 Hikari 连接池详情文本。
     *
     * @param dataSource Hikari 数据源
     * @return 脱敏详情文本
     */
    private String hikariDetail(HikariDataSource dataSource) {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        if (pool == null) {
            return "pool=" + dataSource.getPoolName() + " metrics=not-started";
        }
        return "pool=" + dataSource.getPoolName()
                + " active=" + pool.getActiveConnections()
                + " idle=" + pool.getIdleConnections()
                + " total=" + pool.getTotalConnections()
                + " waiting=" + pool.getThreadsAwaitingConnection();
    }

    /**
     * 解析失败原因展示文本。
     *
     * @param detail 审计详情
     * @return 失败或拒绝原因
     */
    private String failureReason(AuditEventDetail detail) {
        if (StringUtils.hasText(detail.errorMessage())) {
            return detail.errorMessage();
        }
        if (StringUtils.hasText(detail.errorCode())) {
            return detail.errorCode();
        }
        if (StringUtils.hasText(detail.resultSummary())) {
            return detail.resultSummary();
        }
        return "无拒绝或失败原因";
    }

    /**
     * 根据操作类型推导风险级别。
     *
     * @param operation 高危 DDL 操作
     * @return 风险级别
     */
    private String riskFor(DangerousDdlOperation operation) {
        return operation == DangerousDdlOperation.TRUNCATE ? "HIGH" : "CRITICAL";
    }

    /**
     * 创建默认策略说明。
     *
     * @param operation 操作类型
     * @param decision  默认决策
     * @return 策略说明
     */
    private String requirementFor(DangerousDdlOperation operation, DangerousDdlDecision decision) {
        if (operation == DangerousDdlOperation.TRUNCATE) {
            return "默认创建服务端确认挑战，确认前不执行 SQL";
        }
        if (decision == DangerousDdlDecision.DENY) {
            return "默认拒绝，必须命中 DROP YAML/Nacos 白名单";
        }
        return "按默认决策执行：" + decision;
    }

    /**
     * 转换策略决策色调。
     *
     * @param decision 策略决策
     * @return 色调
     */
    private String toneForDecision(DangerousDdlDecision decision) {
        return switch (decision) {
            case ALLOW -> "ok";
            case REQUIRE_CONFIRMATION -> "warn";
            case DENY -> "bad";
        };
    }

    /**
     * 转换审计决策色调。
     *
     * @param decision 审计决策
     * @return 色调
     */
    private String toneForDecision(String decision) {
        if ("EXECUTED".equalsIgnoreCase(decision) || "ALLOWED_EXECUTED".equalsIgnoreCase(decision)) {
            return "ok";
        }
        if ("POLICY_DENIED".equalsIgnoreCase(decision)
                || "DENIED".equalsIgnoreCase(decision)
                || "FAILED".equalsIgnoreCase(decision)) {
            return "bad";
        }
        return "warn";
    }

    /**
     * 转换风险级别色调。
     *
     * @param risk 风险级别
     * @return 色调
     */
    private String toneForRisk(String risk) {
        if ("CRITICAL".equalsIgnoreCase(risk)) {
            return "bad";
        }
        if ("HIGH".equalsIgnoreCase(risk)) {
            return "warn";
        }
        if ("LOW".equalsIgnoreCase(risk)) {
            return "ok";
        }
        return "neutral";
    }

    /**
     * 转换健康状态色调。
     *
     * @param status 健康状态
     * @return 色调
     */
    private String toneForHealth(String status) {
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "HEALTHY" -> "ok";
            case "DISABLED" -> "neutral";
            case "DOWN" -> "bad";
            default -> "warn";
        };
    }

    /**
     * 转换布尔状态文本。
     *
     * @param enabled 是否启用
     * @return 展示文本
     */
    private String enabledText(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    /**
     * 将时间格式化为管理端显示文本。
     *
     * @param instant 时间
     * @return 展示文本
     */
    private String displayTime(Instant instant) {
        return instant == null ? "-" : DISPLAY_TIME_FORMATTER.format(instant);
    }

    /**
     * 将时间转换为 ISO 查询值。
     *
     * @param instant 时间
     * @return 查询值
     */
    private String instantText(Instant instant) {
        return instant == null ? "" : instant.toString();
    }

    /**
     * 转换空文本。
     *
     * @param value 原始文本
     * @return 非空文本或空串
     */
    private String textOrEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 转换默认文本。
     *
     * @param value    原始文本
     * @param fallback 默认文本
     * @return 非空文本或默认文本
     */
    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /**
     * 转换展示文本。
     *
     * @param value 原始对象
     * @return 展示文本
     */
    private String displayText(Object value) {
        return value == null || !StringUtils.hasText(value.toString()) ? "-" : value.toString();
    }

    /**
     * 对健康错误文本做连接串级脱敏。
     *
     * @param message 原始消息
     * @return 脱敏消息
     */
    private String sanitize(String message) {
        if (!StringUtils.hasText(message)) {
            return "无详细错误";
        }
        return message.replaceAll("(?i)jdbc:[^\\s,'\";]+", "[REDACTED]")
                .replaceAll("(?i)((?:password|pwd)\\s*=\\s*)([^\\s&,'\";]+)", "$1[REDACTED]");
    }

    /**
     * 审计列表页面视图。
     *
     * @param rows          当前页审计摘要行
     * @param filter        筛选值
     * @param page          当前页码
     * @param size          每页条数
     * @param totalElements 总记录数
     * @param totalPages    总页数
     * @param sort          排序字段
     * @param direction     排序方向
     * @param hasPrevious   是否存在上一页
     * @param hasNext       是否存在下一页
     * @param previousPage  上一页页码
     * @param nextPage      下一页页码
     * @param previousUrl   上一页链接
     * @param nextUrl       下一页链接
     * @param firstItem     当前页第一条序号
     * @param lastItem      当前页最后一条序号
     */
    public record AuditPageView(
            List<AuditSummaryRow> rows,
            AuditFilterView filter,
            int page,
            int size,
            long totalElements,
            int totalPages,
            String sort,
            String direction,
            boolean hasPrevious,
            boolean hasNext,
            int previousPage,
            int nextPage,
            String previousUrl,
            String nextUrl,
            long firstItem,
            long lastItem
    ) {
    }

    /**
     * 审计列表页面行。
     *
     * @param id           审计事件主键
     * @param createdAt    创建时间展示文本
     * @param user         用户展示文本
     * @param project      项目标识
     * @param env          环境标识
     * @param tool         工具名称
     * @param operation    操作类型
     * @param risk         风险级别
     * @param riskTone     风险色调
     * @param status       审计状态
     * @param decision     决策
     * @param decisionTone 决策色调
     * @param sqlHash      SQL hash
     * @param summary      结果摘要
     * @param affectedRows 影响行数
     */
    public record AuditSummaryRow(
            Long id,
            String createdAt,
            String user,
            String project,
            String env,
            String tool,
            String operation,
            String risk,
            String riskTone,
            String status,
            String decision,
            String decisionTone,
            String sqlHash,
            String summary,
            String affectedRows
    ) {
    }

    /**
     * 审计筛选页面值。
     *
     * @param from             起始时间
     * @param to               结束时间
     * @param userId           用户主键
     * @param project          项目标识
     * @param env              环境标识
     * @param risk             风险级别
     * @param decision         决策
     * @param sqlHash          SQL hash
     * @param tool             工具名称
     * @param size             每页条数
     * @param sort             排序字段
     * @param direction        排序方向
     * @param hasActiveFilters 是否存在筛选条件
     */
    public record AuditFilterView(
            String from,
            String to,
            String userId,
            String project,
            String env,
            String risk,
            String decision,
            String sqlHash,
            String tool,
            String size,
            String sort,
            String direction,
            boolean hasActiveFilters
    ) {
    }

    /**
     * 审计详情页面视图。
     *
     * @param detail           审计详情
     * @param createdAt        创建时间展示文本
     * @param userText         用户展示文本
     * @param affectedRowsText 影响行数展示文本
     * @param confirmationText 确认标识展示文本
     * @param failureReason    失败或拒绝原因
     */
    public record AuditDetailPageView(
            AuditEventDetail detail,
            String createdAt,
            String userText,
            String affectedRowsText,
            String confirmationText,
            String failureReason
    ) {
    }

    /**
     * 危险策略页面视图。
     *
     * @param defaults  默认策略行
     * @param whitelist 白名单行
     * @param rules     固定强化规则
     * @param emptyHint 白名单为空提示
     */
    public record DangerousPolicyPageView(
            List<PolicyDefaultRow> defaults,
            List<PolicyWhitelistRow> whitelist,
            List<PolicyRuleRow> rules,
            String emptyHint
    ) {
    }

    /**
     * 高危 DDL 默认策略行。
     *
     * @param operation   操作类型
     * @param risk        风险级别
     * @param decision    默认决策
     * @param requirement 策略要求
     * @param tone        色调
     */
    public record PolicyDefaultRow(String operation, String risk, String decision, String requirement, String tone) {
    }

    /**
     * 高危 DDL 白名单行。
     *
     * @param operation 操作类型
     * @param risk      风险级别
     * @param project   项目标识
     * @param env       环境标识
     * @param schema    schema 名称
     * @param table     表名
     * @param allowProd 是否允许生产
     * @param prodRule  生产强化说明
     * @param tone      色调
     */
    public record PolicyWhitelistRow(
            String operation,
            String risk,
            String project,
            String env,
            String schema,
            String table,
            String allowProd,
            String prodRule,
            String tone
    ) {
    }

    /**
     * 固定策略规则行。
     *
     * @param name        规则名称
     * @param status      状态
     * @param description 描述
     * @param detail      详情
     * @param tone        色调
     */
    public record PolicyRuleRow(String name, String status, String description, String detail, String tone) {
    }

    /**
     * 系统健康页视图。
     *
     * @param overall        总体状态
     * @param tone           总体色调
     * @param totalCount     健康项总数
     * @param unhealthyCount 非健康项数量
     * @param items          健康项
     */
    public record HealthPageView(
            String overall,
            String tone,
            int totalCount,
            long unhealthyCount,
            List<HealthItem> items
    ) {
    }

    /**
     * 系统健康项。
     *
     * @param name        名称
     * @param component   组件类型
     * @param status      状态
     * @param description 描述
     * @param detail      详情
     * @param tone        色调
     */
    public record HealthItem(String name, String component, String status, String description, String detail,
                             String tone) {
    }
}
