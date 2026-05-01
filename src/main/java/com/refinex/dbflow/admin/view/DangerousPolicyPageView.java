package com.refinex.dbflow.admin.view;

import java.util.List;

/**
 * 危险策略页面视图。
 *
 * @param defaults  默认策略行
 * @param whitelist 白名单行
 * @param rules     固定强化规则
 * @param emptyHint 白名单为空提示
 * @author refinex
 */
public record DangerousPolicyPageView(
        List<PolicyDefaultRow> defaults,
        List<PolicyWhitelistRow> whitelist,
        List<PolicyRuleRow> rules,
        String emptyHint
) {
}
