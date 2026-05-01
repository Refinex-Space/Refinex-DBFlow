package com.refinex.dbflow.sqlpolicy.model;

/**
 * 高危 DDL 策略判定原因码。
 *
 * @author refinex
 */
public enum DangerousDdlPolicyReasonCode {

    /**
     * 命中 YAML 白名单。
     */
    WHITELIST_MATCH,

    /**
     * 未命中白名单，按默认拒绝处理。
     */
    DEFAULT_DENY,

    /**
     * 生产环境缺少显式允许开关。
     */
    PROD_REQUIRES_EXPLICIT_ALLOW,

    /**
     * 缺少策略判定所需目标对象。
     */
    MISSING_TARGET,

    /**
     * SQL 分类阶段已经默认拒绝。
     */
    CLASSIFICATION_REJECTED,

    /**
     * 非当前策略引擎处理的操作。
     */
    NOT_APPLICABLE
}
