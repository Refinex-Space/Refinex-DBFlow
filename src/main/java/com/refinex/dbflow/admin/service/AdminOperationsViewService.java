package com.refinex.dbflow.admin.service;

import com.refinex.dbflow.access.dto.ConfiguredEnvironmentView;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.admin.support.AdminDisplayFormatter;
import com.refinex.dbflow.admin.support.JdbcParts;
import com.refinex.dbflow.admin.support.JdbcUrlSummaryParser;
import com.refinex.dbflow.admin.view.*;
import com.refinex.dbflow.audit.dto.AuditEventDetail;
import com.refinex.dbflow.audit.dto.AuditEventPageResponse;
import com.refinex.dbflow.audit.dto.AuditEventSummary;
import com.refinex.dbflow.audit.dto.AuditQueryCriteria;
import com.refinex.dbflow.audit.service.AuditQueryService;
import com.refinex.dbflow.config.model.DangerousDdlDecision;
import com.refinex.dbflow.config.model.DangerousDdlOperation;
import com.refinex.dbflow.config.properties.DbflowProperties;
import com.refinex.dbflow.observability.dto.HealthComponent;
import com.refinex.dbflow.observability.dto.HealthSnapshot;
import com.refinex.dbflow.observability.service.DbflowHealthService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
     * 审计查询服务。
     */
    private final AuditQueryService auditQueryService;

    /**
     * DBFlow 配置属性。
     */
    private final DbflowProperties dbflowProperties;

    /**
     * 项目环境配置目录服务。
     */
    private final ProjectEnvironmentCatalogService catalogService;

    /**
     * DBFlow 运维健康服务。
     */
    private final DbflowHealthService healthService;

    /**
     * Spring 环境属性。
     */
    private final Environment springEnvironment;

    /**
     * 创建管理端运维页面视图服务。
     *
     * @param auditQueryService 审计查询服务
     * @param dbflowProperties  DBFlow 配置属性
     * @param catalogService    项目环境配置目录服务
     * @param healthService     DBFlow 运维健康服务
     * @param springEnvironment Spring 环境属性
     */
    public AdminOperationsViewService(
            AuditQueryService auditQueryService,
            DbflowProperties dbflowProperties,
            ProjectEnvironmentCatalogService catalogService,
            DbflowHealthService healthService,
            Environment springEnvironment
    ) {
        this.auditQueryService = Objects.requireNonNull(auditQueryService);
        this.dbflowProperties = Objects.requireNonNull(dbflowProperties);
        this.catalogService = Objects.requireNonNull(catalogService);
        this.healthService = Objects.requireNonNull(healthService);
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
     * 创建配置查看页视图。
     *
     * @return 配置查看页视图
     */
    public ConfigPageView configPage() {
        List<ConfiguredEnvironmentView> environments = catalogService.listConfiguredEnvironments();
        return new ConfigPageView(
                configSourceLabel(),
                environments.stream().map(this::toConfigRow).toList(),
                environments.isEmpty() ? "当前未配置 dbflow.projects。" : ""
        );
    }

    /**
     * 将配置环境转换为配置页行。
     *
     * @param environment 环境配置视图
     * @return 配置页行
     */
    private ConfigRow toConfigRow(ConfiguredEnvironmentView environment) {
        JdbcParts jdbcParts = JdbcUrlSummaryParser.parse(environment.jdbcUrl());
        return new ConfigRow(
                environment.projectKey(),
                environment.projectName(),
                environment.environmentKey(),
                environment.environmentName(),
                environment.projectKey() + "/" + environment.environmentKey(),
                jdbcParts.type(),
                jdbcParts.host(),
                jdbcParts.port(),
                jdbcParts.schema(),
                displayText(environment.username()),
                hikariLimits(),
                environment.metadataPresent() ? "已同步" : "未同步"
        );
    }

    /**
     * 创建 Hikari 限制摘要。
     *
     * @return Hikari 限制摘要
     */
    private String hikariLimits() {
        DbflowProperties.Hikari hikari = dbflowProperties.getDatasourceDefaults().getHikari();
        List<String> parts = new ArrayList<>();
        if (hikari.getMaximumPoolSize() != null) {
            parts.add("maxPool=" + hikari.getMaximumPoolSize());
        }
        if (hikari.getMinimumIdle() != null) {
            parts.add("minIdle=" + hikari.getMinimumIdle());
        }
        if (hikari.getConnectionTimeout() != null) {
            parts.add("connectionTimeout=" + hikari.getConnectionTimeout());
        }
        return parts.isEmpty() ? "-" : String.join(" ", parts);
    }

    /**
     * 解析配置来源展示文本。
     *
     * @return 配置来源展示文本
     */
    private String configSourceLabel() {
        boolean configEnabled = springEnvironment.getProperty("spring.cloud.nacos.config.enabled", Boolean.class, false);
        boolean discoveryEnabled = springEnvironment.getProperty("spring.cloud.nacos.discovery.enabled", Boolean.class, false);
        if (!configEnabled && !discoveryEnabled) {
            return "Local application config";
        }
        String namespace = springEnvironment.getProperty("spring.cloud.nacos.config.namespace", "default");
        return "Nacos enabled namespace=" + namespace;
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
        HealthSnapshot snapshot = healthService.snapshot();
        return new HealthPageView(
                snapshot.overall(),
                snapshot.tone(),
                snapshot.totalCount(),
                snapshot.unhealthyCount(),
                snapshot.components().stream().map(this::toHealthItem).toList()
        );
    }

    /**
     * 将共享健康项转换为管理端视图行。
     *
     * @param component 共享健康项
     * @return 管理端健康项
     */
    private HealthItem toHealthItem(HealthComponent component) {
        return new HealthItem(
                component.name(),
                component.component(),
                component.status(),
                component.description(),
                component.detail(),
                component.tone()
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
     * 将时间格式化为管理端显示文本。
     *
     * @param instant 时间
     * @return 展示文本
     */
    private String displayTime(Instant instant) {
        return AdminDisplayFormatter.displayTime(instant);
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
        return AdminDisplayFormatter.displayText(value);
    }
}
