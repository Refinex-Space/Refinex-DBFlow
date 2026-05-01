package com.refinex.dbflow.admin.command;

import java.util.List;

/**
 * 更新用户项目环境授权命令。
 *
 * @param userId          用户主键
 * @param projectKey      项目标识
 * @param environmentKeys 目标环境标识列表
 * @param grantType       授权类型
 * @author refinex
 */
public record UpdateProjectGrantsCommand(Long userId, String projectKey, List<String> environmentKeys,
                                         String grantType) {
}
