package com.refinex.dbflow.admin.view;

/**
 * 高危 DDL 白名单行。
 *
 * @param operation 操作类型
 * @param risk      风险级别
 * @param project   项目标识
 * @param env       环境标识
 * @param schema    schema 名称
 * @param table     表名
 * @param allowProd 是否允许生产
 * @param prodRule  生产强化说明
 * @param tone      色调
 * @author refinex
 */
public record PolicyWhitelistRow(
        String operation,
        String risk,
        String project,
        String env,
        String schema,
        String table,
        String allowProd,
        String prodRule,
        String tone
) {
}
