package com.refinex.dbflow.admin.view;

import java.util.List;

/**
 * 配置查看页视图。
 *
 * @param sourceLabel 配置来源
 * @param rows        配置行
 * @param emptyHint   空状态提示
 * @author refinex
 */
public record ConfigPageView(String sourceLabel, List<ConfigRow> rows, String emptyHint) {
}
