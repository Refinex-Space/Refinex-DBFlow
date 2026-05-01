package com.refinex.dbflow.admin.view;

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
public record HealthItem(String name, String component, String status, String description, String detail,
                         String tone) {
}
