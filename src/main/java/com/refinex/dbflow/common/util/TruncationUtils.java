package com.refinex.dbflow.common.util;

/**
 * 文本截断工具，统一处理有界摘要输出。
 *
 * @author refinex
 */
public final class TruncationUtils {

    /**
     * 工具类不允许实例化。
     */
    private TruncationUtils() {
    }

    /**
     * 按最大长度截断文本。
     *
     * @param value     原始文本
     * @param maxLength 最大长度，必须大于等于 0
     * @return 截断后的文本；原始值为 null 时返回 null
     */
    public static String truncate(String value, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength 不能小于 0");
        }
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
