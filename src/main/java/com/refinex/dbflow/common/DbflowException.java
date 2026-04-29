package com.refinex.dbflow.common;

/**
 * DBFlow 通用运行时异常。
 *
 * @author refinex
 */
public class DbflowException extends RuntimeException {

    /**
     * 业务错误码，用于向外层响应、日志和审计传递稳定失败类型。
     */
    private final ErrorCode errorCode;

    /**
     * 创建带错误码和异常消息的 DBFlow 异常。
     *
     * @param errorCode 业务错误码
     * @param message 异常消息
     */
    public DbflowException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建带错误码、异常消息和原始原因的 DBFlow 异常。
     *
     * @param errorCode 业务错误码
     * @param message 异常消息
     * @param cause 原始异常原因
     */
    public DbflowException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 读取业务错误码。
     *
     * @return 业务错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
