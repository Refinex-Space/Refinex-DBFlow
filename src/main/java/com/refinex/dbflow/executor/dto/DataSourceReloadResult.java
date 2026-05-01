package com.refinex.dbflow.executor.dto;

/**
 * 数据源配置重载结果。
 *
 * @param success     是否重载成功
 * @param targetCount 生效目标数量
 * @param message     脱敏结果信息
 * @author refinex
 */
public record DataSourceReloadResult(boolean success, int targetCount, String message) {

    /**
     * 创建成功结果。
     *
     * @param targetCount 生效目标数量
     * @param message     脱敏结果信息
     * @return 成功结果
     */
    public static DataSourceReloadResult success(int targetCount, String message) {
        return new DataSourceReloadResult(true, targetCount, message);
    }

    /**
     * 创建失败结果。
     *
     * @param message 脱敏失败信息
     * @return 失败结果
     */
    public static DataSourceReloadResult failure(String message) {
        return new DataSourceReloadResult(false, 0, message);
    }
}
