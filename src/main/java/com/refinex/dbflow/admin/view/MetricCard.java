package com.refinex.dbflow.admin.view;

/**
 * 总览指标卡。
 *
 * @param label 标签
 * @param value 指标值
 * @param hint  提示
 * @param tone  色调
 * @author refinex
 */
public record MetricCard(String label, String value, String hint, String tone) {
}
