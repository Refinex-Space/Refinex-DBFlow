package com.refinex.dbflow.common;

/**
 * DBFlow 通用 API 结果模型。
 *
 * @param success 请求是否成功
 * @param code 结果码，成功时为 OK，失败时为稳定错误码
 * @param message 结果描述
 * @param data 响应数据
 * @param <T> 响应数据类型
 * @author refinex
 */
public record ApiResult<T>(boolean success, String code, String message, T data) {

    /**
     * 创建成功结果。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功 API 结果
     */
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, "OK", "成功", data);
    }

    /**
     * 根据错误码创建失败结果。
     *
     * @param errorCode 业务错误码
     * @param <T> 响应数据类型
     * @return 失败 API 结果
     */
    public static <T> ApiResult<T> failed(ErrorCode errorCode) {
        return new ApiResult<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }
}
