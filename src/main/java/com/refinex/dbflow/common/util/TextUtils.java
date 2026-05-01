package com.refinex.dbflow.common.util;

/**
 * 文本基础工具，统一处理空白、裁剪和展示兜底。
 *
 * @author refinex
 */
public final class TextUtils {

    /**
     * 默认空值展示文本。
     */
    private static final String DEFAULT_EMPTY_DISPLAY = "-";

    /**
     * 工具类不允许实例化。
     */
    private TextUtils() {
    }

    /**
     * 判断文本是否包含非空白字符。
     *
     * @param value 待判断文本
     * @return 包含非空白字符时返回 true
     */
    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 将文本裁剪为空值或去空白文本。
     *
     * @param value 原始文本
     * @return 空白时返回 null，否则返回去除首尾空白后的文本
     */
    public static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 将文本裁剪为空字符串或去空白文本。
     *
     * @param value 原始文本
     * @return 空白时返回空字符串，否则返回去除首尾空白后的文本
     */
    public static String trimToEmpty(String value) {
        return hasText(value) ? value.trim() : "";
    }

    /**
     * 转换面向管理端或诊断输出的展示文本。
     *
     * @param value 原始对象
     * @return 空白时返回短横线，否则返回对象文本
     */
    public static String displayText(Object value) {
        if (value == null) {
            return DEFAULT_EMPTY_DISPLAY;
        }
        String text = value.toString();
        return hasText(text) ? text : DEFAULT_EMPTY_DISPLAY;
    }
}
