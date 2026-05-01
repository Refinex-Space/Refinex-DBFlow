package com.refinex.dbflow.admin.view;

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
 * @author refinex
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
