package com.refinex.dbflow.admin.service;

import com.refinex.dbflow.access.dto.ConfiguredEnvironmentView;
import com.refinex.dbflow.access.repository.DbfApiTokenRepository;
import com.refinex.dbflow.access.repository.DbfUserEnvGrantRepository;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.admin.support.AdminDisplayFormatter;
import com.refinex.dbflow.admin.view.*;
import com.refinex.dbflow.audit.dto.AuditEventPageResponse;
import com.refinex.dbflow.audit.dto.AuditEventSummary;
import com.refinex.dbflow.audit.dto.AuditQueryCriteria;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.repository.DbfAuditEventRepository;
import com.refinex.dbflow.audit.repository.DbfConfirmationChallengeRepository;
import com.refinex.dbflow.audit.service.AuditQueryService;
import com.refinex.dbflow.observability.dto.HealthComponent;
import com.refinex.dbflow.observability.dto.HealthSnapshot;
import com.refinex.dbflow.observability.service.DbflowHealthService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * 管理端总览页视图服务，聚合审计、Token、授权、配置和健康状态。
 *
 * @author refinex
 */
@Service
public class AdminOverviewViewService {

    /**
     * 最近数据统计窗口小时数。
     */
    private static final int RECENT_WINDOW_HOURS = 24;

    /**
     * Token 临期窗口天数。
     */
    private static final int TOKEN_EXPIRING_DAYS = 7;

    /**
     * 最近审计展示数量。
     */
    private static final int RECENT_AUDIT_SIZE = 5;

    /**
     * 关注事项展示数量。
     */
    private static final int ATTENTION_SIZE = 5;

    /**
     * pending confirmation 状态。
     */
    private static final String PENDING_STATUS = "PENDING";

    /**
     * active 状态。
     */
    private static final String ACTIVE_STATUS = "ACTIVE";

    /**
     * 审计事件 repository。
     */
    private final DbfAuditEventRepository auditEventRepository;

    /**
     * 确认挑战 repository。
     */
    private final DbfConfirmationChallengeRepository confirmationChallengeRepository;

    /**
     * Token repository。
     */
    private final DbfApiTokenRepository tokenRepository;

    /**
     * 授权 repository。
     */
    private final DbfUserEnvGrantRepository grantRepository;

    /**
     * 项目环境配置目录服务。
     */
    private final ProjectEnvironmentCatalogService catalogService;

    /**
     * 审计查询服务。
     */
    private final AuditQueryService auditQueryService;

    /**
     * 健康服务。
     */
    private final DbflowHealthService healthService;

    /**
     * 创建管理端总览页视图服务。
     *
     * @param auditEventRepository            审计事件 repository
     * @param confirmationChallengeRepository 确认挑战 repository
     * @param tokenRepository                 Token repository
     * @param grantRepository                 授权 repository
     * @param catalogService                  项目环境配置目录服务
     * @param auditQueryService               审计查询服务
     * @param healthService                   健康服务
     */
    public AdminOverviewViewService(
            DbfAuditEventRepository auditEventRepository,
            DbfConfirmationChallengeRepository confirmationChallengeRepository,
            DbfApiTokenRepository tokenRepository,
            DbfUserEnvGrantRepository grantRepository,
            ProjectEnvironmentCatalogService catalogService,
            AuditQueryService auditQueryService,
            DbflowHealthService healthService
    ) {
        this.auditEventRepository = Objects.requireNonNull(auditEventRepository);
        this.confirmationChallengeRepository = Objects.requireNonNull(confirmationChallengeRepository);
        this.tokenRepository = Objects.requireNonNull(tokenRepository);
        this.grantRepository = Objects.requireNonNull(grantRepository);
        this.catalogService = Objects.requireNonNull(catalogService);
        this.auditQueryService = Objects.requireNonNull(auditQueryService);
        this.healthService = Objects.requireNonNull(healthService);
    }

    /**
     * 创建总览页视图。
     *
     * @return 总览页视图
     */
    @Transactional(readOnly = true)
    public OverviewPageView overview() {
        Instant now = Instant.now();
        Instant recentFrom = now.minus(RECENT_WINDOW_HOURS, ChronoUnit.HOURS);
        Instant expiringTo = now.plus(TOKEN_EXPIRING_DAYS, ChronoUnit.DAYS);
        List<ConfiguredEnvironmentView> environments = catalogService.listConfiguredEnvironments();
        HealthSnapshot health = healthService.snapshot();

        long sqlRequests = auditEventRepository.count(recentSqlSpec(recentFrom));
        long policyDenied = auditEventRepository.count(recentDecisionSpec(recentFrom, "POLICY_DENIED"));
        long pendingConfirmations = confirmationChallengeRepository.countByStatus(PENDING_STATUS);
        long activeTokens = tokenRepository.countByStatus(ACTIVE_STATUS);
        long expiringTokens = tokenRepository.countByStatusAndExpiresAtBetween(ACTIVE_STATUS, now, expiringTo);
        long activeGrants = grantRepository.countByStatus(ACTIVE_STATUS);
        long prodEnvironments = environments.stream().filter(this::isProductionEnvironment).count();
        List<HealthComponent> unhealthyTargetPools = unhealthyTargetPools(health);

        return new OverviewPageView(
                metrics(sqlRequests, policyDenied, pendingConfirmations, activeTokens, expiringTokens, activeGrants,
                        environments.size(), prodEnvironments, unhealthyTargetPools.size()),
                recentAuditRows(),
                attentionItems(policyDenied, pendingConfirmations, expiringTokens, unhealthyTargetPools),
                environmentOptions(environments),
                "最近 " + RECENT_WINDOW_HOURS + " 小时网关安全、执行和健康摘要。"
        );
    }

    /**
     * 创建指标卡。
     *
     * @param sqlRequests          SQL 请求数
     * @param policyDenied         策略拒绝数
     * @param pendingConfirmations 待确认数
     * @param activeTokens         active Token 数
     * @param expiringTokens       临期 Token 数
     * @param activeGrants         active 授权数
     * @param configuredEnvs       配置环境数
     * @param prodEnvs             生产环境数
     * @param unhealthyPools       异常连接池数
     * @return 指标卡列表
     */
    private List<MetricCard> metrics(
            long sqlRequests,
            long policyDenied,
            long pendingConfirmations,
            long activeTokens,
            long expiringTokens,
            long activeGrants,
            long configuredEnvs,
            long prodEnvs,
            long unhealthyPools
    ) {
        return List.of(
                new MetricCard("SQL 请求", Long.toString(sqlRequests), "最近 24 小时 execute / explain / inspect", "neutral"),
                new MetricCard("策略拒绝", Long.toString(policyDenied), "最近 24 小时 POLICY_DENIED", policyDenied > 0 ? "bad" : "neutral"),
                new MetricCard("待确认", Long.toString(pendingConfirmations), "PENDING confirmation challenge", pendingConfirmations > 0 ? "warn" : "neutral"),
                new MetricCard("有效 Token", Long.toString(activeTokens), expiringTokens + " 个 7 天内过期", expiringTokens > 0 ? "warn" : "neutral"),
                new MetricCard("已授权环境", Long.toString(activeGrants), configuredEnvs + " 配置 / " + prodEnvs + " 生产", "neutral"),
                new MetricCard("异常数据源", Long.toString(unhealthyPools), "DEGRADED / DOWN target-pool", unhealthyPools > 0 ? "warn" : "neutral")
        );
    }

    /**
     * 查询最近审计行。
     *
     * @return 最近审计行
     */
    private List<RecentAuditRow> recentAuditRows() {
        AuditEventPageResponse<AuditEventSummary> page = auditQueryService.query(new AuditQueryCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                RECENT_AUDIT_SIZE,
                "createdAt",
                "desc"
        ));
        return page.content().stream().map(this::toRecentAuditRow).toList();
    }

    /**
     * 转换最近审计行。
     *
     * @param summary 审计摘要
     * @return 最近审计行
     */
    private RecentAuditRow toRecentAuditRow(AuditEventSummary summary) {
        return new RecentAuditRow(
                summary.id(),
                displayTime(summary.createdAt()),
                displayText(summary.userId()),
                summary.projectKey(),
                summary.environmentKey(),
                summary.operationType(),
                summary.riskLevel(),
                toneForRisk(summary.riskLevel()),
                summary.decision(),
                toneForDecision(summary.decision()),
                summary.sqlHash()
        );
    }

    /**
     * 创建关注事项。
     *
     * @param policyDenied         策略拒绝数
     * @param pendingConfirmations 待确认数
     * @param expiringTokens       临期 Token 数
     * @param unhealthyTargetPools 异常目标连接池
     * @return 关注事项
     */
    private List<AttentionItem> attentionItems(
            long policyDenied,
            long pendingConfirmations,
            long expiringTokens,
            List<HealthComponent> unhealthyTargetPools
    ) {
        List<AttentionItem> items = new java.util.ArrayList<>();
        if (policyDenied > 0) {
            items.add(new AttentionItem("最近 24 小时有 " + policyDenied + " 条策略拒绝", "POLICY_DENIED", "bad",
                    "/admin/audit?decision=POLICY_DENIED"));
        }
        if (pendingConfirmations > 0) {
            items.add(new AttentionItem("有 " + pendingConfirmations + " 个 SQL 等待确认", "REQUIRES_CONFIRMATION", "warn",
                    "/admin/audit?decision=REQUIRES_CONFIRMATION"));
        }
        if (!unhealthyTargetPools.isEmpty()) {
            HealthComponent first = unhealthyTargetPools.getFirst();
            items.add(new AttentionItem(first.name() + " 状态异常", first.status(), first.tone(), "/admin/health"));
        }
        if (expiringTokens > 0) {
            items.add(new AttentionItem(expiringTokens + " 个 Token 7 天内过期", "EXPIRING", "warn", "/admin/tokens?status=ACTIVE"));
        }
        if (items.size() <= ATTENTION_SIZE) {
            return items;
        }
        return List.copyOf(items.subList(0, ATTENTION_SIZE));
    }

    /**
     * 创建环境选项。
     *
     * @param environments 配置环境
     * @return 环境选项
     */
    private List<OverviewEnvironmentOption> environmentOptions(List<ConfiguredEnvironmentView> environments) {
        List<OverviewEnvironmentOption> options = new java.util.ArrayList<>();
        options.add(new OverviewEnvironmentOption("", "全部环境"));
        environments.stream()
                .map(environment -> new OverviewEnvironmentOption(
                        environment.projectKey() + "/" + environment.environmentKey(),
                        environment.projectKey() + " / " + environment.environmentKey()
                ))
                .forEach(options::add);
        return options;
    }

    /**
     * 查询异常目标连接池。
     *
     * @param health 健康快照
     * @return 异常目标连接池
     */
    private List<HealthComponent> unhealthyTargetPools(HealthSnapshot health) {
        return health.components().stream()
                .filter(component -> "target-pool".equals(component.component()))
                .filter(component -> healthService.unhealthyStatus(component.status()))
                .toList();
    }

    /**
     * 创建最近 SQL 统计条件。
     *
     * @param from 起始时间
     * @return 查询条件
     */
    private Specification<DbfAuditEvent> recentSqlSpec(Instant from) {
        return Specification.allOf(
                createdAfter(from),
                (root, query, criteriaBuilder) -> root.get("tool").in(
                        "dbflow_execute_sql",
                        "dbflow_explain_sql",
                        "dbflow_inspect_schema"
                )
        );
    }

    /**
     * 创建最近决策统计条件。
     *
     * @param from     起始时间
     * @param decision 审计决策
     * @return 查询条件
     */
    private Specification<DbfAuditEvent> recentDecisionSpec(Instant from, String decision) {
        return Specification.allOf(
                createdAfter(from),
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("decision"), decision)
        );
    }

    /**
     * 创建创建时间下限条件。
     *
     * @param from 起始时间
     * @return 查询条件
     */
    private Specification<DbfAuditEvent> createdAfter(Instant from) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    /**
     * 判断是否生产环境。
     *
     * @param environment 环境视图
     * @return 生产环境时返回 true
     */
    private boolean isProductionEnvironment(ConfiguredEnvironmentView environment) {
        String key = environment.environmentKey();
        return "prod".equalsIgnoreCase(key) || "production".equalsIgnoreCase(key);
    }

    /**
     * 转换风险色调。
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
     * 转换决策色调。
     *
     * @param decision 审计决策
     * @return 色调
     */
    private String toneForDecision(String decision) {
        if ("EXECUTED".equalsIgnoreCase(decision) || "ALLOWED_EXECUTED".equalsIgnoreCase(decision)) {
            return "ok";
        }
        if ("POLICY_DENIED".equalsIgnoreCase(decision) || "DENIED".equalsIgnoreCase(decision)
                || "FAILED".equalsIgnoreCase(decision)) {
            return "bad";
        }
        return "warn";
    }

    /**
     * 格式化时间。
     *
     * @param instant 时间
     * @return 展示文本
     */
    private String displayTime(Instant instant) {
        return AdminDisplayFormatter.displayTime(instant);
    }

    /**
     * 转换展示文本。
     *
     * @param value 原始值
     * @return 展示文本
     */
    private String displayText(Object value) {
        return AdminDisplayFormatter.displayText(value);
    }
}
