package com.refinex.dbflow.sqlpolicy;

/**
 * 可审计的 SQL 分类结果。
 *
 * @param statementType     语句类型
 * @param operation         操作类型
 * @param riskLevel         风险等级
 * @param targetSchema      目标 schema
 * @param targetTable       目标表
 * @param isDdl             是否 DDL
 * @param isDml             是否 DML
 * @param parseStatus       解析状态
 * @param rejectedByDefault 是否默认拒绝
 * @param auditReason       审计原因
 * @author refinex
 */
public record SqlClassification(
        SqlStatementType statementType,
        SqlOperation operation,
        SqlRiskLevel riskLevel,
        String targetSchema,
        String targetTable,
        boolean isDdl,
        boolean isDml,
        SqlParseStatus parseStatus,
        boolean rejectedByDefault,
        String auditReason) {
}
