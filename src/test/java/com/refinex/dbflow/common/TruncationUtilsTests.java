package com.refinex.dbflow.common;

import com.refinex.dbflow.common.util.TruncationUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文本截断工具测试。
 *
 * @author refinex
 */
class TruncationUtilsTests {

    /**
     * 验证文本超过上限时按长度截断。
     */
    @Test
    void truncateShouldLimitTextLength() {
        assertThat(TruncationUtils.truncate("dbflow", 2)).isEqualTo("db");
        assertThat(TruncationUtils.truncate("dbflow", 6)).isEqualTo("dbflow");
        assertThat(TruncationUtils.truncate(null, 6)).isNull();
    }

    /**
     * 验证非法长度会被拒绝，避免调用方隐藏边界错误。
     */
    @Test
    void truncateShouldRejectNegativeLength() {
        assertThatThrownBy(() -> TruncationUtils.truncate("dbflow", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxLength 不能小于 0");
    }
}
