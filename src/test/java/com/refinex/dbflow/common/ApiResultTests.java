package com.refinex.dbflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 通用 API 结果模型测试。
 *
 * @author refinex
 */
class ApiResultTests {

    /**
     * 验证成功结果携带 OK 状态和响应数据。
     */
    @Test
    void okShouldCarryData() {
        ApiResult<String> result = ApiResult.ok("dbflow");

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("成功");
        assertThat(result.data()).isEqualTo("dbflow");
    }

    /**
     * 验证失败结果携带稳定错误码且不返回数据。
     */
    @Test
    void failedShouldCarryErrorCode() {
        ApiResult<Object> result = ApiResult.failed(ErrorCode.INVALID_REQUEST);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("INVALID_REQUEST");
        assertThat(result.message()).isEqualTo("请求不合法");
        assertThat(result.data()).isNull();
    }
}
