package com.refinex.dbflow.mcp;

import com.refinex.dbflow.capacity.model.CapacityDecision;
import com.refinex.dbflow.capacity.model.CapacityReasonCode;
import com.refinex.dbflow.capacity.model.CapacityRequest;
import com.refinex.dbflow.capacity.service.CapacityGuardService;
import com.refinex.dbflow.capacity.support.CapacityPermit;
import com.refinex.dbflow.executor.dto.SchemaInspectRequest;
import com.refinex.dbflow.executor.dto.SchemaInspectResult;
import com.refinex.dbflow.executor.dto.SqlExecutionRequest;
import com.refinex.dbflow.executor.dto.SqlExecutionResult;
import com.refinex.dbflow.executor.service.SchemaInspectService;
import com.refinex.dbflow.executor.service.SqlExecutionService;
import com.refinex.dbflow.executor.service.SqlExplainService;
import com.refinex.dbflow.mcp.auth.McpAccessBoundaryService;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContext;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContextResolver;
import com.refinex.dbflow.mcp.auth.McpAuthorizationBoundary;
import com.refinex.dbflow.mcp.dto.DbflowMcpSkeletonResponse;
import com.refinex.dbflow.mcp.support.McpTargetPolicyProjectionService;
import com.refinex.dbflow.mcp.tool.DbflowMcpTools;
import com.refinex.dbflow.observability.service.DbflowMetricsService;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;
import com.refinex.dbflow.sqlpolicy.service.TruncateConfirmationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MCP 容量治理接入测试。
 *
 * @author refinex
 */
class DbflowMcpCapacityTests {

    /**
     * 验证容量拒绝时不会继续触达 SQL 执行服务。
     */
    @Test
    void shouldRejectExecuteBeforeCallingSqlExecutionService() {
        CapacityGuardService capacityGuardService = mock(CapacityGuardService.class);
        SqlExecutionService sqlExecutionService = mock(SqlExecutionService.class);
        when(capacityGuardService.evaluate(any(CapacityRequest.class))).thenReturn(
                CapacityDecision.rejected(CapacityReasonCode.TOOL_BULKHEAD_FULL, Duration.ofMillis(100))
        );
        DbflowMcpTools tools = tools(capacityGuardService, sqlExecutionService, mock(SchemaInspectService.class));

        DbflowMcpSkeletonResponse response = tools.executeSql(
                "demo",
                "dev",
                "select 1",
                null,
                false,
                "capacity test"
        );

        assertThat(response.data()).containsEntry("status", "CAPACITY_REJECTED");
        assertThat(error(response)).containsEntry("code", "CAPACITY_REJECTED");
        assertThat(error(response)).containsEntry("reasonCode", "TOOL_BULKHEAD_FULL");
        verify(sqlExecutionService, never()).execute(any(SqlExecutionRequest.class));
    }

    /**
     * 验证重型只读降级时会把服务端 maxItems 覆盖传给 schema inspect。
     */
    @Test
    void shouldApplyHeavyReadMaxItemsOverrideWhenInspectSchemaIsDegraded() {
        CapacityGuardService capacityGuardService = mock(CapacityGuardService.class);
        SchemaInspectService schemaInspectService = mock(SchemaInspectService.class);
        when(capacityGuardService.evaluate(any(CapacityRequest.class))).thenReturn(
                CapacityDecision.degraded(
                        CapacityReasonCode.TARGET_PRESSURE,
                        List.of("当前处于压力态，重型只读结果已降低返回上限"),
                        CapacityPermit.none(),
                        50
                )
        );
        when(schemaInspectService.inspect(any(SchemaInspectRequest.class))).thenReturn(schemaResult(50));
        DbflowMcpTools tools = tools(capacityGuardService, mock(SqlExecutionService.class), schemaInspectService);

        DbflowMcpSkeletonResponse response = tools.inspectSchema("demo", "dev", "app", null, 200);

        ArgumentCaptor<SchemaInspectRequest> requestCaptor = ArgumentCaptor.forClass(SchemaInspectRequest.class);
        verify(schemaInspectService).inspect(requestCaptor.capture());
        assertThat(requestCaptor.getValue().maxItems()).isEqualTo(50);
        assertThat(response.data()).containsEntry("maxItems", 50);
        assertThat(capacity(response)).containsEntry("degraded", true);
        assertThat(response.data().get("notices").toString()).contains("TARGET_PRESSURE");
    }

    /**
     * 验证 SQL 执行结束后释放容量 permit。
     */
    @Test
    void shouldReleaseCapacityPermitAfterSqlExecution() {
        AtomicInteger releases = new AtomicInteger();
        CapacityGuardService capacityGuardService = mock(CapacityGuardService.class);
        SqlExecutionService sqlExecutionService = mock(SqlExecutionService.class);
        when(capacityGuardService.evaluate(any(CapacityRequest.class))).thenReturn(
                CapacityDecision.allow(CapacityPermit.of(List.of(releases::incrementAndGet)))
        );
        when(sqlExecutionService.execute(any(SqlExecutionRequest.class))).thenReturn(sqlResult());
        DbflowMcpTools tools = tools(capacityGuardService, sqlExecutionService, mock(SchemaInspectService.class));

        DbflowMcpSkeletonResponse response = tools.executeSql(
                "demo",
                "dev",
                "select 1",
                null,
                false,
                "release test"
        );

        assertThat(response.data()).containsEntry("status", "SUCCEEDED");
        assertThat(releases).hasValue(1);
    }

    /**
     * 创建 MCP tools 测试对象。
     *
     * @param capacityGuardService 容量治理服务
     * @param sqlExecutionService  SQL 执行服务
     * @param schemaInspectService schema inspect 服务
     * @return MCP tools 测试对象
     */
    private DbflowMcpTools tools(
            CapacityGuardService capacityGuardService,
            SqlExecutionService sqlExecutionService,
            SchemaInspectService schemaInspectService
    ) {
        McpAuthenticationContext context = new McpAuthenticationContext(
                true,
                "alice",
                1L,
                10L,
                "tok",
                "BEARER",
                "codex",
                "JUnit",
                "127.0.0.1",
                "request-1"
        );
        McpAuthorizationBoundary boundary = new McpAuthorizationBoundary(true, true, "AUTHORIZED", "允许访问");
        McpAuthenticationContextResolver contextResolver = mock(McpAuthenticationContextResolver.class);
        when(contextResolver.currentContext()).thenReturn(context);
        McpAccessBoundaryService accessBoundaryService = mock(McpAccessBoundaryService.class);
        when(accessBoundaryService.metadataBoundary(eq(context), any(String.class))).thenReturn(boundary);
        when(accessBoundaryService.targetBoundary(eq(context), any(String.class), any(String.class), any(String.class)))
                .thenReturn(boundary);
        return new DbflowMcpTools(
                contextResolver,
                accessBoundaryService,
                capacityGuardService,
                mock(McpTargetPolicyProjectionService.class),
                mock(TruncateConfirmationService.class),
                sqlExecutionService,
                mock(SqlExplainService.class),
                schemaInspectService,
                metricsProvider()
        );
    }

    /**
     * 创建 schema inspect 结果。
     *
     * @param maxItems 最大条目数
     * @return schema inspect 结果
     */
    private SchemaInspectResult schemaResult(int maxItems) {
        return new SchemaInspectResult(
                "demo",
                "dev",
                true,
                "SUCCEEDED",
                "app",
                null,
                maxItems,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1L,
                null,
                null
        );
    }

    /**
     * 创建 SQL 执行结果。
     *
     * @return SQL 执行结果
     */
    private SqlExecutionResult sqlResult() {
        return new SqlExecutionResult(
                "demo",
                "dev",
                SqlOperation.SELECT,
                SqlRiskLevel.LOW,
                true,
                List.of("1"),
                List.of(Map.of("1", 1)),
                false,
                0L,
                List.of(),
                1L,
                "SELECT",
                "hash",
                false,
                null,
                null,
                "SUCCEEDED"
        );
    }

    /**
     * 创建空指标 provider。
     *
     * @return 空指标 provider
     */
    @SuppressWarnings("unchecked")
    private ObjectProvider<DbflowMetricsService> metricsProvider() {
        ObjectProvider<DbflowMetricsService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    /**
     * 提取响应错误数据。
     *
     * @param response MCP 响应
     * @return 错误数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> error(DbflowMcpSkeletonResponse response) {
        return (Map<String, Object>) response.data().get("error");
    }

    /**
     * 提取响应容量数据。
     *
     * @param response MCP 响应
     * @return 容量数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> capacity(DbflowMcpSkeletonResponse response) {
        return (Map<String, Object>) response.data().get("capacity");
    }
}
