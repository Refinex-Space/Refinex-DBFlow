package com.refinex.dbflow.admin.view;

import java.util.List;

/**
 * 系统健康页视图。
 *
 * @param overall        总体状态
 * @param tone           总体色调
 * @param totalCount     健康项总数
 * @param unhealthyCount 非健康项数量
 * @param items          健康项
 * @author refinex
 */
public record HealthPageView(
        String overall,
        String tone,
        int totalCount,
        long unhealthyCount,
        List<HealthItem> items
) {
}
