package com.refinex.dbflow.access.model;

/**
 * 访问判断原因。
 *
 * @author refinex
 */
public enum AccessDecisionReason {

    /**
     * 允许访问。
     */
    ALLOWED,

    /**
     * 用户不存在。
     */
    USER_NOT_FOUND,

    /**
     * 用户已禁用。
     */
    USER_DISABLED,

    /**
     * Token 不存在。
     */
    TOKEN_NOT_FOUND,

    /**
     * Token 与用户不匹配。
     */
    TOKEN_USER_MISMATCH,

    /**
     * Token 已禁用。
     */
    TOKEN_DISABLED,

    /**
     * Token 已过期。
     */
    TOKEN_EXPIRED,

    /**
     * 项目不存在。
     */
    PROJECT_NOT_FOUND,

    /**
     * 环境不存在。
     */
    ENVIRONMENT_NOT_FOUND,

    /**
     * 授权不存在。
     */
    GRANT_NOT_FOUND
}
