package com.refinex.dbflow.admin.view;

import java.util.List;

/**
 * 总览页视图。
 *
 * @param metrics            指标卡
 * @param recentAuditRows    最近审计行
 * @param attentionItems     关注事项
 * @param environmentOptions 环境选项
 * @param windowLabel        统计窗口说明
 * @author refinex
 */
public record OverviewPageView(
        List<MetricCard> metrics,
        List<RecentAuditRow> recentAuditRows,
        List<AttentionItem> attentionItems,
        List<OverviewEnvironmentOption> environmentOptions,
        String windowLabel
) {
}
