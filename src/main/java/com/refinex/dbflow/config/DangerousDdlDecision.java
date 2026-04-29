package com.refinex.dbflow.config;

/**
 * 高危 DDL 操作的默认处置策略。
 *
 * @author refinex
 */
public enum DangerousDdlDecision {

    /**
     * 拒绝执行，除非后续策略层明确发现白名单放行。
     */
    DENY,

    /**
     * 需要服务端确认挑战，确认完成后才允许继续执行。
     */
    REQUIRE_CONFIRMATION,

    /**
     * 允许执行；当前仅作为显式配置值保留，不作为高危操作默认值。
     */
    ALLOW
}
