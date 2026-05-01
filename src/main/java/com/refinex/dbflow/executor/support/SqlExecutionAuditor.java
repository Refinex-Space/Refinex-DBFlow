package com.refinex.dbflow.executor.support;

import com.refinex.dbflow.audit.dto.AuditEventWriteRequest;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.executor.dto.SqlExecutionRequest;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SQL 执行审计编排器，集中创建审计写入请求并路由审计状态。
 *
 * @author refinex
 */
@Component
public class SqlExecutionAuditor {

    /**
     * 审计状态：已拒绝。
     */
    private static final String STATUS_DENIED = "DENIED";

    /**
     * 审计状态：执行失败。
     */
    private static final String STATUS_FAILED = "FAILED";

    /**
     * 审计事件写入器。
     */
    private final AuditEventWriter auditEventWriter;

    /**
     * 创建 SQL 执行审计编排器。
     *
     * @param auditEventWriter 审计事件写入器
     */
    public SqlExecutionAuditor(AuditEventWriter auditEventWriter) {
        this.auditEventWriter = auditEventWriter;
    }

    /**
     * 记录 SQL 请求已接收。
     *
     * @param request 执行请求
     * @param sqlHash SQL hash
     * @param sqlText SQL 原文
     */
    public void requestReceived(SqlExecutionRequest request, String sqlHash, String sqlText) {
        auditEventWriter.requestReceived(auditRequest(request, SqlOperation.UNKNOWN, SqlRiskLevel.LOW, sqlHash,
                sqlText, "SQL 请求已接收", 0L, null, null, null));
    }

    /**
     * 记录 SQL 执行审计。
     *
     * @param request      执行请求
     * @param operation    SQL 操作
     * @param riskLevel    风险等级
     * @param status       审计状态
     * @param sqlHash      SQL hash
     * @param sqlText      SQL 原文
     * @param summary      结果摘要
     * @param affectedRows 影响行数
     * @param errorCode    错误码
     * @param errorMessage 错误摘要
     */
    public void audit(
            SqlExecutionRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String status,
            String sqlHash,
            String sqlText,
            String summary,
            long affectedRows,
            String errorCode,
            String errorMessage
    ) {
        AuditEventWriteRequest eventRequest = auditRequest(request, operation, riskLevel, sqlHash, sqlText, summary,
                affectedRows, errorCode, errorMessage, null);
        if (STATUS_DENIED.equals(status)) {
            auditEventWriter.policyDenied(eventRequest);
        } else if (STATUS_FAILED.equals(status)) {
            auditEventWriter.failed(eventRequest);
        } else {
            auditEventWriter.executed(eventRequest);
        }
    }

    /**
     * 创建审计写入请求。
     *
     * @param request        执行请求
     * @param operation      SQL 操作
     * @param riskLevel      风险等级
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param summary        结果摘要
     * @param affectedRows   影响行数
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     * @param confirmationId 确认挑战标识
     * @return 审计写入请求
     */
    private AuditEventWriteRequest auditRequest(
            SqlExecutionRequest request,
            SqlOperation operation,
            SqlRiskLevel riskLevel,
            String sqlHash,
            String sqlText,
            String summary,
            Long affectedRows,
            String errorCode,
            String errorMessage,
            String confirmationId
    ) {
        return new AuditEventWriteRequest(
                request.requestId(),
                request.userId(),
                request.tokenId(),
                request.tokenPrefix(),
                request.auditContext(),
                request.projectKey(),
                request.environmentKey(),
                operation.name(),
                auditRiskLevel(riskLevel),
                safeSqlText(sqlText),
                sqlHash,
                summary,
                affectedRows,
                errorCode,
                errorMessage,
                confirmationId
        );
    }

    /**
     * 映射审计表允许的风险等级。
     *
     * @param riskLevel SQL 分类风险等级
     * @return 审计风险等级
     */
    private String auditRiskLevel(SqlRiskLevel riskLevel) {
        if (riskLevel == SqlRiskLevel.REJECTED) {
            return "FORBIDDEN";
        }
        return riskLevel.name();
    }

    /**
     * 安全标准化 SQL 文本。
     *
     * @param sql SQL 原文
     * @return 标准化 SQL 或空字符串
     */
    private String safeSqlText(String sql) {
        return StringUtils.hasText(sql) ? sql.strip() : "";
    }
}
