package com.refinex.dbflow.capacity;

import com.refinex.dbflow.capacity.model.CapacityDecision;
import com.refinex.dbflow.capacity.model.CapacityReasonCode;
import com.refinex.dbflow.capacity.model.CapacityRequest;
import com.refinex.dbflow.capacity.model.McpToolClass;
import com.refinex.dbflow.capacity.properties.CapacityProperties;
import com.refinex.dbflow.capacity.service.CapacityGuardService;
import com.refinex.dbflow.capacity.service.CapacityMetricsService;
import com.refinex.dbflow.capacity.service.SystemPressureService;
import com.refinex.dbflow.capacity.support.CapacityPermit;
import com.refinex.dbflow.capacity.support.InMemoryWindowRateLimiter;
import com.refinex.dbflow.capacity.support.SemaphoreBulkheadRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单实例容量治理 smoke 测试。
 *
 * @author refinex
 */
class SingleInstanceCapacitySmokeTests {

    /**
     * 验证执行路径耗尽不会阻塞轻量只读。
     */
    @Test
    void shouldKeepLightReadAvailableWhenExecuteBulkheadIsFull() {
        CapacityGuardService service = service(properties());
        CapacityDecision firstExecute = service.evaluate(request("execute-1", 1L, 1L, McpToolClass.EXECUTE,
                "billing", "prod"));

        try (CapacityPermit ignored = firstExecute.permit()) {
            CapacityDecision secondExecute = service.evaluate(request("execute-2", 2L, 2L, McpToolClass.EXECUTE,
                    "billing", "prod"));
            CapacityDecision lightRead = service.evaluate(request("light-1", 3L, 3L, McpToolClass.LIGHT_READ,
                    null, null));

            assertThat(secondExecute.allowed()).isFalse();
            assertThat(secondExecute.reasonCode()).isEqualTo(CapacityReasonCode.TOOL_BULKHEAD_FULL);
            assertThat(lightRead.allowed()).isTrue();
            lightRead.permit().close();
        }
    }

    /**
     * 验证单个 Token 不能占满全部实例容量。
     */
    @Test
    void shouldLimitOneTokenWithoutBlockingOtherTokens() {
        CapacityGuardService service = service(properties());
        CapacityDecision first = service.evaluate(request("token-1", 1L, 10L, McpToolClass.LIGHT_READ,
                null, null));

        try (CapacityPermit ignored = first.permit()) {
            CapacityDecision sameToken = service.evaluate(request("token-2", 2L, 10L, McpToolClass.LIGHT_READ,
                    null, null));
            CapacityDecision otherToken = service.evaluate(request("token-3", 3L, 11L, McpToolClass.LIGHT_READ,
                    null, null));

            assertThat(sameToken.allowed()).isFalse();
            assertThat(sameToken.reasonCode()).isEqualTo(CapacityReasonCode.TOKEN_BULKHEAD_FULL);
            assertThat(otherToken.allowed()).isTrue();
            otherToken.permit().close();
        }
    }

    /**
     * 验证单个目标库并发耗尽不会阻塞其他目标库。
     */
    @Test
    void shouldIsolateTargetBulkheads() {
        CapacityGuardService service = service(properties());
        CapacityDecision firstTarget = service.evaluate(request("target-1", 1L, 20L, McpToolClass.HEAVY_READ,
                "billing", "prod"));

        try (CapacityPermit ignored = firstTarget.permit()) {
            CapacityDecision sameTarget = service.evaluate(request("target-2", 2L, 21L, McpToolClass.HEAVY_READ,
                    "billing", "prod"));
            CapacityDecision otherTarget = service.evaluate(request("target-3", 3L, 22L, McpToolClass.HEAVY_READ,
                    "crm", "prod"));

            assertThat(sameTarget.allowed()).isFalse();
            assertThat(sameTarget.reasonCode()).isEqualTo(CapacityReasonCode.TARGET_BULKHEAD_FULL);
            assertThat(otherTarget.allowed()).isTrue();
            otherTarget.permit().close();
        }
    }

    /**
     * 验证多组混合请求在阈值内可获得明确决策。
     */
    @Test
    void shouldProduceDeterministicDecisionsForMixedLogicalUsers() {
        CapacityGuardService service = service(properties());
        List<CapacityDecision> decisions = new ArrayList<>();

        for (int index = 0; index < 100; index++) {
            McpToolClass toolClass = switch (index % 4) {
                case 0 -> McpToolClass.LIGHT_READ;
                case 1 -> McpToolClass.HEAVY_READ;
                case 2 -> McpToolClass.EXPLAIN;
                default -> McpToolClass.EXECUTE;
            };
            decisions.add(service.evaluate(request(
                    "mixed-" + index,
                    (long) index,
                    (long) index,
                    toolClass,
                    "project-" + (index % 8),
                    "dev"
            )));
        }

        assertThat(decisions).allSatisfy(decision -> assertThat(decision.reasonCode()).isNotNull());
        decisions.forEach(decision -> decision.permit().close());
    }

    /**
     * 创建容量治理服务。
     *
     * @param properties 容量治理配置
     * @return 容量治理服务
     */
    private CapacityGuardService service(CapacityProperties properties) {
        SystemPressureService pressureService = mock(SystemPressureService.class);
        when(pressureService.localPressure()).thenReturn(false);
        when(pressureService.targetPressure(anyString(), anyString())).thenReturn(false);
        return new CapacityGuardService(
                properties,
                new InMemoryWindowRateLimiter(),
                new SemaphoreBulkheadRegistry(),
                pressureService,
                metricsProvider()
        );
    }

    /**
     * 创建容量治理配置。
     *
     * @return 容量治理配置
     */
    private CapacityProperties properties() {
        CapacityProperties properties = new CapacityProperties();
        properties.getBulkhead().setGlobalMaxConcurrent(20);
        properties.getBulkhead().setPerTokenMaxConcurrent(1);
        properties.getBulkhead().setPerUserMaxConcurrent(20);
        properties.getBulkhead().setPerTargetMaxConcurrent(1);
        properties.getBulkhead().getClasses().get(McpToolClass.LIGHT_READ).setMaxConcurrent(20);
        properties.getBulkhead().getClasses().get(McpToolClass.HEAVY_READ).setMaxConcurrent(20);
        properties.getBulkhead().getClasses().get(McpToolClass.EXPLAIN).setMaxConcurrent(20);
        properties.getBulkhead().getClasses().get(McpToolClass.EXECUTE).setMaxConcurrent(1);
        properties.getBulkhead().getClasses().values()
                .forEach(rule -> rule.setAcquireTimeout(Duration.ZERO));
        properties.getRateLimit().getPerToken().setMaxRequests(1_000);
        properties.getRateLimit().getPerUser().setMaxRequests(1_000);
        properties.getRateLimit().getPerTool().values()
                .forEach(rule -> rule.setMaxRequests(1_000));
        properties.afterPropertiesSet();
        return properties;
    }

    /**
     * 创建容量请求。
     *
     * @param requestId 请求标识
     * @param userId    用户主键
     * @param tokenId   Token 主键
     * @param toolClass 工具类别
     * @param project   项目标识
     * @param env       环境标识
     * @return 容量请求
     */
    private CapacityRequest request(
            String requestId,
            Long userId,
            Long tokenId,
            McpToolClass toolClass,
            String project,
            String env
    ) {
        return new CapacityRequest(requestId, userId, tokenId, "tool-" + toolClass.name(), toolClass, project, env);
    }

    /**
     * 创建空指标 provider。
     *
     * @return 空指标 provider
     */
    @SuppressWarnings("unchecked")
    private ObjectProvider<CapacityMetricsService> metricsProvider() {
        ObjectProvider<CapacityMetricsService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
