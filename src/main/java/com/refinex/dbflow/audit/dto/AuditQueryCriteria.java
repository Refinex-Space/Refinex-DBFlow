package com.refinex.dbflow.audit.dto;

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

    /**
     * 创建空查询条件。
     *
     * @return 空查询条件
     */
    public static AuditQueryCriteria empty() {
        return builder().build();
    }

    /**
     * 创建审计查询条件 Builder。
     *
     * @return 审计查询条件 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于已有查询条件创建 Builder。
     *
     * @param criteria 已有查询条件
     * @return 审计查询条件 Builder
     */
    public static Builder from(AuditQueryCriteria criteria) {
        Builder builder = builder();
        if (criteria == null) {
            return builder;
        }
        return builder.from(criteria.from())
                .to(criteria.to())
                .userId(criteria.userId())
                .projectKey(criteria.projectKey())
                .environmentKey(criteria.environmentKey())
                .riskLevel(criteria.riskLevel())
                .decision(criteria.decision())
                .sqlHash(criteria.sqlHash())
                .tool(criteria.tool())
                .page(criteria.page())
                .size(criteria.size())
                .sort(criteria.sort())
                .direction(criteria.direction());
    }

    /**
     * 审计查询条件 Builder，避免调用端堆叠长参数构造器。
     */
    public static final class Builder {

        /**
         * 创建时间起点，包含边界。
         */
        private Instant from;

        /**
         * 创建时间终点，包含边界。
         */
        private Instant to;

        /**
         * 用户主键。
         */
        private Long userId;

        /**
         * 项目标识。
         */
        private String projectKey;

        /**
         * 环境标识。
         */
        private String environmentKey;

        /**
         * 风险级别。
         */
        private String riskLevel;

        /**
         * 审计决策。
         */
        private String decision;

        /**
         * SQL hash。
         */
        private String sqlHash;

        /**
         * 工具名称。
         */
        private String tool;

        /**
         * 页码，从 0 开始。
         */
        private Integer page;

        /**
         * 每页条数。
         */
        private Integer size;

        /**
         * 排序字段。
         */
        private String sort;

        /**
         * 排序方向。
         */
        private String direction;

        /**
         * 设置创建时间起点。
         *
         * @param from 创建时间起点
         * @return 当前 Builder
         */
        public Builder from(Instant from) {
            this.from = from;
            return this;
        }

        /**
         * 设置创建时间终点。
         *
         * @param to 创建时间终点
         * @return 当前 Builder
         */
        public Builder to(Instant to) {
            this.to = to;
            return this;
        }

        /**
         * 设置用户主键。
         *
         * @param userId 用户主键
         * @return 当前 Builder
         */
        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置项目标识。
         *
         * @param projectKey 项目标识
         * @return 当前 Builder
         */
        public Builder projectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        /**
         * 设置环境标识。
         *
         * @param environmentKey 环境标识
         * @return 当前 Builder
         */
        public Builder environmentKey(String environmentKey) {
            this.environmentKey = environmentKey;
            return this;
        }

        /**
         * 设置风险级别。
         *
         * @param riskLevel 风险级别
         * @return 当前 Builder
         */
        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        /**
         * 设置审计决策。
         *
         * @param decision 审计决策
         * @return 当前 Builder
         */
        public Builder decision(String decision) {
            this.decision = decision;
            return this;
        }

        /**
         * 设置 SQL hash。
         *
         * @param sqlHash SQL hash
         * @return 当前 Builder
         */
        public Builder sqlHash(String sqlHash) {
            this.sqlHash = sqlHash;
            return this;
        }

        /**
         * 设置工具名称。
         *
         * @param tool 工具名称
         * @return 当前 Builder
         */
        public Builder tool(String tool) {
            this.tool = tool;
            return this;
        }

        /**
         * 设置页码。
         *
         * @param page 页码
         * @return 当前 Builder
         */
        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        /**
         * 设置每页条数。
         *
         * @param size 每页条数
         * @return 当前 Builder
         */
        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        /**
         * 设置排序字段。
         *
         * @param sort 排序字段
         * @return 当前 Builder
         */
        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        /**
         * 设置排序方向。
         *
         * @param direction 排序方向
         * @return 当前 Builder
         */
        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }

        /**
         * 构建审计查询条件。
         *
         * @return 审计查询条件
         */
        public AuditQueryCriteria build() {
            return new AuditQueryCriteria(from, to, userId, projectKey, environmentKey, riskLevel, decision, sqlHash,
                    tool, page, size, sort, direction);
        }
    }
}
