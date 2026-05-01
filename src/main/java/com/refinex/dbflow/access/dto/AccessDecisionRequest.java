package com.refinex.dbflow.access.dto;

/**
 * 项目环境访问判断请求。
 *
 * @param userId         用户主键
 * @param tokenId        Token 主键
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @author refinex
 */
public record AccessDecisionRequest(
        Long userId,
        Long tokenId,
        String projectKey,
        String environmentKey
) {
}
