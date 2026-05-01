package com.refinex.dbflow.sqlpolicy.dto;

import com.refinex.dbflow.sqlpolicy.model.DangerousDdlPolicyReasonCode;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;

/**
 * 高危 DDL 策略判定结果，供后续审计事件完整落库。
 *
 * @param projectKey       项目标识
 * @param environmentKey   环境标识
 * @param operation        SQL 操作
 * @param targetSchema     目标 schema
 * @param targetTable      目标表
 * @param allowed          是否允许继续
 * @param reasonCode       机器可读原因码
 * @param reason           人类可读原因
 * @param matchedWhitelist 是否命中白名单
 * @param auditRequired    是否必须写审计
 * @author refinex
 */
public record DangerousDdlPolicyDecision(
        String projectKey,
        String environmentKey,
        SqlOperation operation,
        String targetSchema,
        String targetTable,
        boolean allowed,
        DangerousDdlPolicyReasonCode reasonCode,
        String reason,
        boolean matchedWhitelist,
        boolean auditRequired) {
}
