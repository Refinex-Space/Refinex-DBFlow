package com.refinex.dbflow.audit.service;

import java.time.Instant;

/**
 * 管理端审计查询条件。
 *
 * @param from           创建时间起点，包含边界
 * @param to             创建时间终点，包含边界
 * @param userId         用户主键
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param riskLevel      风险级别
 * @param decision       审计决策
 * @param sqlHash        SQL hash
 * @param tool           工具名称
 * @param page           页码，从 0 开始
 * @param size           每页条数
 * @param sort           排序字段
 * @param direction      排序方向
 * @author refinex
 */
public record AuditQueryCriteria(
        Instant from,
        Instant to,
        Long userId,
        String projectKey,
        String environmentKey,
        String riskLevel,
        String decision,
        String sqlHash,
        String tool,
        Integer page,
        Integer size,
        String sort,
        String direction
) {
}
