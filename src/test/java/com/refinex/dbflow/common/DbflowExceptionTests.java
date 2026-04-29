package com.refinex.dbflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * DBFlow 通用异常模型测试。
 *
 * @author refinex
 */
class DbflowExceptionTests {

    /**
     * 验证异常对象保留稳定错误码和原始异常原因。
     */
    @Test
    void exceptionShouldCarryErrorCodeAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("bad input");

        DbflowException exception = new DbflowException(ErrorCode.INVALID_REQUEST, "参数错误", cause);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(exception.getMessage()).isEqualTo("参数错误");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
