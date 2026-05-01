package com.refinex.dbflow.executor;

import com.refinex.dbflow.audit.dto.AuditEventWriteRequest;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.executor.dto.SqlExecutionOptions;
import com.refinex.dbflow.executor.dto.SqlExecutionRequest;
import com.refinex.dbflow.executor.support.SqlExecutionAuditor;
import com.refinex.dbflow.executor.support.SqlExecutionAuditor.SqlExecutionAuditPayload;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * {@link SqlExecutionAuditor} 单元测试。
 *
 * @author refinex
 */
@ExtendWith(MockitoExtension.class)
class SqlExecutionAuditorTests {

    /**
     * 审计写入器。
     */
    @Mock
    private AuditEventWriter auditEventWriter;

    /**
     * 待测试审计编排器。
     */
    @InjectMocks
    private SqlExecutionAuditor sqlExecutionAuditor;

    /**
     * 验证请求已接收事件可以正确构造审计写入请求。
     */
    @Test
    void shouldWriteRequestReceivedAudit() {
        sqlExecutionAuditor.requestReceived(request(), "hash-1", " SELECT 1 ");

        ArgumentCaptor<AuditEventWriteRequest> captor = ArgumentCaptor.forClass(AuditEventWriteRequest.class);
        verify(auditEventWriter).requestReceived(captor.capture());
        AuditEventWriteRequest event = captor.getValue();
        assertThat(event.operation()).isEqualTo("UNKNOWN");
        assertThat(event.riskLevel()).isEqualTo("LOW");
        assertThat(event.sqlHash()).isEqualTo("hash-1");
        assertThat(event.sqlText()).isEqualTo("SELECT 1");
        assertThat(event.resultSummary()).isEqualTo("SQL 请求已接收");
        assertThat(event.affectedRows()).isZero();
    }

    /**
     * 验证拒绝审计会被路由到 policyDenied 并保留负载内容。
     */
    @Test
    void shouldRouteDeniedAuditToPolicyDeniedWriter() {
        sqlExecutionAuditor.audit(request(), new SqlExecutionAuditPayload(
                SqlOperation.DELETE,
                SqlRiskLevel.REJECTED,
                "DENIED",
                "hash-2",
                "DELETE FROM orders",
                "策略拒绝",
                0L,
                "DENIED",
                "策略拒绝"
        ));

        ArgumentCaptor<AuditEventWriteRequest> captor = ArgumentCaptor.forClass(AuditEventWriteRequest.class);
        verify(auditEventWriter).policyDenied(captor.capture());
        AuditEventWriteRequest event = captor.getValue();
        assertThat(event.operation()).isEqualTo("DELETE");
        assertThat(event.riskLevel()).isEqualTo("FORBIDDEN");
        assertThat(event.errorCode()).isEqualTo("DENIED");
        assertThat(event.errorMessage()).isEqualTo("策略拒绝");
        assertThat(event.resultSummary()).isEqualTo("策略拒绝");
    }

    /**
     * 创建测试执行请求。
     *
     * @return 执行请求
     */
    private SqlExecutionRequest request() {
        return new SqlExecutionRequest(
                "req-1",
                1L,
                2L,
                "dbf_demo",
                "demo",
                "dev",
                "SELECT 1",
                null,
                false,
                "test",
                SqlExecutionOptions.defaults()
        );
    }
}

