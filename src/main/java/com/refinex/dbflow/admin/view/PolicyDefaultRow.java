package com.refinex.dbflow.admin.view;

/**
 * 高危 DDL 默认策略行。
 *
 * @param operation   操作类型
 * @param risk        风险级别
 * @param decision    默认决策
 * @param requirement 策略要求
 * @param tone        色调
 * @author refinex
 */
public record PolicyDefaultRow(String operation, String risk, String decision, String requirement, String tone) {
}
