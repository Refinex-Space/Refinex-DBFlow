package com.refinex.dbflow.common;

import com.refinex.dbflow.common.util.TextUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文本基础工具测试。
 *
 * @author refinex
 */
class TextUtilsTests {

    /**
     * 验证空白判断兼容 null、空字符串和有效文本。
     */
    @Test
    void hasTextShouldDetectNonBlankText() {
        assertThat(TextUtils.hasText(null)).isFalse();
        assertThat(TextUtils.hasText("  ")).isFalse();
        assertThat(TextUtils.hasText(" dbflow ")).isTrue();
    }

    /**
     * 验证裁剪工具返回稳定空值语义。
     */
    @Test
    void trimHelpersShouldReturnStableEmptyValues() {
        assertThat(TextUtils.trimToNull("  ")).isNull();
        assertThat(TextUtils.trimToNull(" dbflow ")).isEqualTo("dbflow");
        assertThat(TextUtils.trimToEmpty(null)).isEmpty();
        assertThat(TextUtils.trimToEmpty(" dbflow ")).isEqualTo("dbflow");
    }

    /**
     * 验证展示文本为空时使用统一短横线。
     */
    @Test
    void displayTextShouldFallbackForBlankValues() {
        assertThat(TextUtils.displayText(null)).isEqualTo("-");
        assertThat(TextUtils.displayText("")).isEqualTo("-");
        assertThat(TextUtils.displayText("value")).isEqualTo("value");
    }
}
