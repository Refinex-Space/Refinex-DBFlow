package com.refinex.dbflow.common;

/**
 * DBFlow 通用错误码。
 *
 * @author refinex
 */
public enum ErrorCode {

    /**
     * 请求参数或调用上下文不符合服务端约束。
     */
    INVALID_REQUEST("INVALID_REQUEST", "请求不合法"),

    /**
     * 请求未通过认证。
     */
    UNAUTHENTICATED("UNAUTHENTICATED", "认证失败"),

    /**
     * 服务端内部处理失败。
     */
    INTERNAL_ERROR("INTERNAL_ERROR", "服务端内部错误");

    /**
     * 对外稳定错误码，供接口响应、日志和审计摘要引用。
     */
    private final String code;

    /**
     * 面向调用方的默认中文错误描述。
     */
    private final String message;

    /**
     * 创建错误码枚举值。
     *
     * @param code 对外稳定错误码
     * @param message 默认中文错误描述
     */
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 读取对外稳定错误码。
     *
     * @return 对外稳定错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 读取默认中文错误描述。
     *
     * @return 默认中文错误描述
     */
    public String getMessage() {
        return message;
    }
}
