package com.refinex.dbflow.observability.dto;

import java.util.List;

/**
 * 目标数据源注册表健康分组。
 *
 * @param configuredTargets 配置的目标环境数量
 * @param healthyTargets    健康目标数量
 * @param unhealthyTargets  不健康目标数量
 * @param components        目标健康项
 * @author refinex
 */
public record TargetDatasourceHealth(
        int configuredTargets,
        long healthyTargets,
        long unhealthyTargets,
        List<HealthComponent> components
) {
}
