package com.refinex.dbflow.admin.support;

import com.refinex.dbflow.common.util.TextUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 管理端展示文本格式化工具。
 *
 * @author refinex
 */
public final class AdminDisplayFormatter {

    /**
     * 管理端时间格式。
     */
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * 工具类不允许实例化。
     */
    private AdminDisplayFormatter() {
    }

    /**
     * 转换展示文本。
     *
     * @param value 原始值
     * @return 展示文本
     */
    public static String displayText(Object value) {
        return TextUtils.displayText(value);
    }

    /**
     * 格式化管理端时间。
     *
     * @param instant 时间
     * @return 展示文本
     */
    public static String displayTime(Instant instant) {
        return instant == null ? "-" : DISPLAY_TIME_FORMATTER.format(instant);
    }
}
