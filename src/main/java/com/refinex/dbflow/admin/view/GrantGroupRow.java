package com.refinex.dbflow.admin.view;

import java.util.List;

/**
 * 授权分组行（同一用户×项目合并展示）。
 *
 * @param userId       用户主键
 * @param username     用户名
 * @param projectKey   项目标识
 * @param environments 分组下环境授权条目
 * @author refinex
 */
public record GrantGroupRow(Long userId, String username, String projectKey, List<GrantEnvEntry> environments) {
}
