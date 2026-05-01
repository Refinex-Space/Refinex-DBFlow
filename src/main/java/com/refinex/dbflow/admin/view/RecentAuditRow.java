package com.refinex.dbflow.admin.view;

/**
 * 最近审计行。
 *
 * @param id           审计事件主键
 * @param time         时间
 * @param user         用户
 * @param project      项目
 * @param env          环境
 * @param operation    操作类型
 * @param risk         风险级别
 * @param riskTone     风险色调
 * @param decision     决策
 * @param decisionTone 决策色调
 * @param sqlHash      SQL hash
 * @author refinex
 */
public record RecentAuditRow(
        Long id,
        String time,
        String user,
        String project,
        String env,
        String operation,
        String risk,
        String riskTone,
        String decision,
        String decisionTone,
        String sqlHash
) {
}
