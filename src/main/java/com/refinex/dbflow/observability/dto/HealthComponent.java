package com.refinex.dbflow.observability.dto;

/**
 * 系统健康项。
 *
 * @param name        名称
 * @param component   组件类型
 * @param status      状态
 * @param description 描述
 * @param detail      详情
 * @param tone        色调
 * @author refinex
 */
public record HealthComponent(
        String name,
        String component,
        String status,
        String description,
        String detail,
        String tone
) {

    /**
     * 判断当前组件是否不健康。
     *
     * @return 不健康时返回 true
     */
    public boolean unhealthy() {
        return "DEGRADED".equalsIgnoreCase(status) || "DOWN".equalsIgnoreCase(status);
    }
}
