package com.refinex.dbflow.admin.view;

/**
 * 固定策略规则行。
 *
 * @param name        规则名称
 * @param status      状态
 * @param description 描述
 * @param detail      详情
 * @param tone        色调
 * @author refinex
 */
public record PolicyRuleRow(String name, String status, String description, String detail, String tone) {
}
