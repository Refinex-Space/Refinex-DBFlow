package com.refinex.dbflow.capacity.service;

import com.refinex.dbflow.capacity.model.*;
import com.refinex.dbflow.capacity.properties.CapacityProperties;
import com.refinex.dbflow.capacity.support.CapacityPermit;
import com.refinex.dbflow.capacity.support.InMemoryWindowRateLimiter;
import com.refinex.dbflow.capacity.support.SemaphoreBulkheadRegistry;
import com.refinex.dbflow.capacity.support.SemaphoreBulkheadRegistry.BulkheadRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 容量治理编排服务，统一执行限流、并发舱壁、压力态降级和容量指标记录。
 *
 * @author refinex
 */
@Service
public class CapacityGuardService {

    /**
     * 容量治理配置。
     */
    private final CapacityProperties properties;

    /**
     * 固定窗口限流器。
     */
    private final InMemoryWindowRateLimiter rateLimiter;

    /**
     * 信号量并发舱壁注册表。
     */
    private final SemaphoreBulkheadRegistry bulkheadRegistry;

    /**
     * 系统压力判断服务。
     */
    private final SystemPressureService pressureService;

    /**
     * 容量指标服务，部分测试中允许不存在。
     */
    private final CapacityMetricsService metricsService;

    /**
     * 创建容量治理编排服务。
     *
     * @param properties             容量治理配置
     * @param rateLimiter            固定窗口限流器
     * @param bulkheadRegistry       信号量并发舱壁注册表
     * @param pressureService        系统压力判断服务
     * @param metricsServiceProvider 容量指标服务 provider
     */
    public CapacityGuardService(
            CapacityProperties properties,
            InMemoryWindowRateLimiter rateLimiter,
            SemaphoreBulkheadRegistry bulkheadRegistry,
            SystemPressureService pressureService,
            ObjectProvider<CapacityMetricsService> metricsServiceProvider
    ) {
        this.properties = Objects.requireNonNull(properties);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
        this.bulkheadRegistry = Objects.requireNonNull(bulkheadRegistry);
        this.pressureService = Objects.requireNonNull(pressureService);
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    /**
     * 评估容量请求。
     *
     * @param request 容量请求
     * @return 容量决策
     */
    public CapacityDecision evaluate(CapacityRequest request) {
        Objects.requireNonNull(request, "request");
        long started = System.nanoTime();
        if (!properties.isEnabled()) {
            return record(request.toolClass(), CapacityDecision.disabled(), started);
        }
        boolean localPressure = pressureService.localPressure();
        boolean targetPressure = pressureService.targetPressure(request.projectKey(), request.environmentKey());
        CapacityDecision pressureDecision = pressureDecision(request, localPressure, targetPressure);
        if (pressureDecision != null && !pressureDecision.allowed()) {
            pressureService.recordRejectionSignal();
            return record(request.toolClass(), pressureDecision, started);
        }
        CapacityDecision rateLimitDecision = rateLimit(request);
        if (!rateLimitDecision.allowed()) {
            pressureService.recordRejectionSignal();
            recordRateLimitExhausted(rateLimitDecision.reasonCode());
            return record(request.toolClass(), rateLimitDecision, started);
        }
        SemaphoreBulkheadRegistry.BulkheadAcquireResult acquireResult = bulkheadRegistry.acquireAll(
                bulkheadRequests(request));
        if (!acquireResult.acquired()) {
            pressureService.recordRejectionSignal();
            return record(request.toolClass(), CapacityDecision.rejected(acquireResult.reasonCode(),
                    Duration.ofMillis(100)), started);
        }
        CapacityDecision allowed = allowedDecision(request, pressureDecision, acquireResult.permit());
        return record(request.toolClass(), allowed, started);
    }

    /**
     * 根据压力态创建立即拒绝或降级占位决策。
     *
     * @param request        容量请求
     * @param localPressure  是否存在本地压力
     * @param targetPressure 是否存在目标压力
     * @return 决策；无需提前拒绝时返回 null
     */
    private CapacityDecision pressureDecision(
            CapacityRequest request,
            boolean localPressure,
            boolean targetPressure
    ) {
        if (!properties.getDegradation().isEnabled() || (!localPressure && !targetPressure)) {
            return null;
        }
        CapacityReasonCode reasonCode = targetPressure ? CapacityReasonCode.TARGET_PRESSURE
                : CapacityReasonCode.LOCAL_PRESSURE;
        if (request.toolClass() == McpToolClass.EXECUTE && properties.getDegradation().isRejectExecuteUnderPressure()) {
            return CapacityDecision.rejected(reasonCode, Duration.ofSeconds(1));
        }
        if (request.toolClass() == McpToolClass.EXPLAIN && properties.getDegradation().isRejectExplainUnderPressure()) {
            return CapacityDecision.rejected(reasonCode, Duration.ofSeconds(1));
        }
        if (request.toolClass() == McpToolClass.HEAVY_READ) {
            return CapacityDecision.degraded(reasonCode, List.of("当前处于压力态，重型只读结果已降低返回上限"));
        }
        if (request.toolClass() == McpToolClass.LIGHT_READ) {
            return CapacityDecision.degraded(reasonCode, List.of("当前处于压力态，轻量只读请求继续服务"));
        }
        return null;
    }

    /**
     * 执行 Token、用户、工具和目标维度限流。
     *
     * @param request 容量请求
     * @return 容量决策
     */
    private CapacityDecision rateLimit(CapacityRequest request) {
        CapacityDecision tokenDecision = rateLimitIfPresent("token:" + request.tokenId(), request.tokenId() != null,
                properties.getRateLimit().getPerToken(), CapacityReasonCode.TOKEN_RATE_LIMITED);
        if (!tokenDecision.allowed()) {
            return tokenDecision;
        }
        CapacityDecision userDecision = rateLimitIfPresent("user:" + request.userId(), request.userId() != null,
                properties.getRateLimit().getPerUser(), CapacityReasonCode.USER_RATE_LIMITED);
        if (!userDecision.allowed()) {
            return userDecision;
        }
        CapacityProperties.RateLimitRule toolRule = properties.getRateLimit().getPerTool().get(request.toolClass());
        CapacityDecision toolDecision = rateLimitIfPresent("tool:" + request.toolClass(), request.toolClass() != null,
                toolRule, CapacityReasonCode.TOOL_RATE_LIMITED);
        if (!toolDecision.allowed()) {
            return toolDecision;
        }
        return rateLimitIfPresent("target:" + request.projectKey() + "/" + request.environmentKey(),
                hasTarget(request), toolRule, CapacityReasonCode.TARGET_RATE_LIMITED);
    }

    /**
     * 在 key 存在时执行一次限流。
     *
     * @param key        限流 key
     * @param enabled    是否执行限流
     * @param rule       限流规则
     * @param reasonCode 拒绝原因码
     * @return 容量决策
     */
    private CapacityDecision rateLimitIfPresent(
            String key,
            boolean enabled,
            CapacityProperties.RateLimitRule rule,
            CapacityReasonCode reasonCode
    ) {
        if (!enabled) {
            return CapacityDecision.allow();
        }
        InMemoryWindowRateLimiter.RateLimitResult result = rateLimiter.allow(key, rule);
        if (result.allowed()) {
            return CapacityDecision.allow();
        }
        return CapacityDecision.rejected(reasonCode, result.retryAfter());
    }

    /**
     * 创建需要按顺序获取的并发舱壁请求。
     *
     * @param request 容量请求
     * @return 并发舱壁请求列表
     */
    private List<BulkheadRequest> bulkheadRequests(CapacityRequest request) {
        CapacityProperties.Bulkhead bulkhead = properties.getBulkhead();
        CapacityProperties.BulkheadClassRule classRule = bulkhead.getClasses().get(request.toolClass());
        Duration classTimeout = classRule.getAcquireTimeout();
        List<BulkheadRequest> requests = new ArrayList<>();
        requests.add(new BulkheadRequest(CapacityScope.GLOBAL, "global", bulkhead.getGlobalMaxConcurrent(),
                bulkhead.getAcquireTimeout(), CapacityReasonCode.GLOBAL_BULKHEAD_FULL));
        requests.add(new BulkheadRequest(CapacityScope.TOOL_CLASS, request.toolClass().name(),
                classRule.getMaxConcurrent(), classTimeout, CapacityReasonCode.TOOL_BULKHEAD_FULL));
        if (request.tokenId() != null) {
            requests.add(new BulkheadRequest(CapacityScope.TOKEN, String.valueOf(request.tokenId()),
                    bulkhead.getPerTokenMaxConcurrent(), classTimeout, CapacityReasonCode.TOKEN_BULKHEAD_FULL));
        }
        if (request.userId() != null) {
            requests.add(new BulkheadRequest(CapacityScope.USER, String.valueOf(request.userId()),
                    bulkhead.getPerUserMaxConcurrent(), classTimeout, CapacityReasonCode.USER_BULKHEAD_FULL));
        }
        if (hasTarget(request)) {
            requests.add(new BulkheadRequest(CapacityScope.TARGET,
                    request.projectKey() + "/" + request.environmentKey(), bulkhead.getPerTargetMaxConcurrent(),
                    classTimeout, CapacityReasonCode.TARGET_BULKHEAD_FULL));
        }
        return requests;
    }

    /**
     * 创建最终允许或降级决策。
     *
     * @param request          容量请求
     * @param pressureDecision 压力态降级决策
     * @param permit           已获取的容量 permit
     * @return 容量决策
     */
    private CapacityDecision allowedDecision(
            CapacityRequest request,
            CapacityDecision pressureDecision,
            CapacityPermit permit
    ) {
        if (pressureDecision == null || !pressureDecision.degraded()) {
            return CapacityDecision.allow(permit);
        }
        if (request.toolClass() == McpToolClass.HEAVY_READ) {
            return CapacityDecision.degraded(
                    pressureDecision.reasonCode(),
                    pressureDecision.notices(),
                    permit,
                    properties.getDegradation().getHeavyReadMaxItemsUnderPressure()
            );
        }
        return CapacityDecision.degraded(pressureDecision.reasonCode(), pressureDecision.notices(), permit, null);
    }

    /**
     * 判断容量请求是否包含目标项目环境。
     *
     * @param request 容量请求
     * @return 包含目标时返回 true
     */
    private boolean hasTarget(CapacityRequest request) {
        return StringUtils.hasText(request.projectKey()) && StringUtils.hasText(request.environmentKey());
    }

    /**
     * 记录限流耗尽指标。
     *
     * @param reasonCode 拒绝原因码
     */
    private void recordRateLimitExhausted(CapacityReasonCode reasonCode) {
        if (metricsService == null) {
            return;
        }
        CapacityScope scope = switch (reasonCode) {
            case TOKEN_RATE_LIMITED -> CapacityScope.TOKEN;
            case USER_RATE_LIMITED -> CapacityScope.USER;
            case TOOL_RATE_LIMITED -> CapacityScope.TOOL_CLASS;
            case TARGET_RATE_LIMITED -> CapacityScope.TARGET;
            default -> CapacityScope.GLOBAL;
        };
        metricsService.recordRateLimitExhausted(scope);
    }

    /**
     * 记录容量决策指标。
     *
     * @param toolClass    工具类别
     * @param decision     容量决策
     * @param startedNanos 开始时间纳秒
     * @return 原始容量决策
     */
    private CapacityDecision record(McpToolClass toolClass, CapacityDecision decision, long startedNanos) {
        if (metricsService != null) {
            metricsService.recordRequest(toolClass, decision.status(), decision.reasonCode());
            metricsService.recordAcquireDuration(toolClass, decision.status(), startedNanos);
            if (!decision.allowed()) {
                metricsService.recordRejection(toolClass, decision.reasonCode());
            }
            if (decision.degraded()) {
                metricsService.recordDegradation(toolClass, decision.reasonCode());
            }
        }
        return decision;
    }
}
