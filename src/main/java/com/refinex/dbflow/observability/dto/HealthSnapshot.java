package com.refinex.dbflow.observability.dto;

import java.util.List;

/**
 * 系统健康快照。
 *
 * @param overall        总体状态
 * @param tone           总体色调
 * @param totalCount     健康项总数
 * @param unhealthyCount 非健康项数量
 * @param components     健康项
 * @author refinex
 */
public record HealthSnapshot(
        String overall,
        String tone,
        int totalCount,
        long unhealthyCount,
        List<HealthComponent> components
) {
}
