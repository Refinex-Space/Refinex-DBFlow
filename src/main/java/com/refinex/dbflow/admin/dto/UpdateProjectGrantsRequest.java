package com.refinex.dbflow.admin.dto;

import java.util.List;

/**
 * React 管理端更新用户项目环境授权请求。
 *
 * @param userId          用户主键
 * @param projectKey      项目标识
 * @param environmentKeys 目标环境标识列表，空列表表示撤销该项目下全部授权
 * @param grantType       授权类型
 * @author refinex
 */
public record UpdateProjectGrantsRequest(Long userId, String projectKey, List<String> environmentKeys,
                                         String grantType) {
}
