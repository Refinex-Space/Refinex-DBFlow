package com.refinex.dbflow.admin.view;

/**
 * 审计筛选页面值。
 *
 * @param from             起始时间
 * @param to               结束时间
 * @param userId           用户主键
 * @param project          项目标识
 * @param env              环境标识
 * @param risk             风险级别
 * @param decision         决策
 * @param sqlHash          SQL hash
 * @param tool             工具名称
 * @param size             每页条数
 * @param sort             排序字段
 * @param direction        排序方向
 * @param hasActiveFilters 是否存在筛选条件
 * @author refinex
 */
public record AuditFilterView(
        String from,
        String to,
        String userId,
        String project,
        String env,
        String risk,
        String decision,
        String sqlHash,
        String tool,
        String size,
        String sort,
        String direction,
        boolean hasActiveFilters
) {
}
