package com.refinex.dbflow.admin.view;

/**
 * 授权分组行内单个环境条目。
 *
 * @param grantId        授权主键
 * @param environmentKey 环境标识
 * @param grantType      授权类型
 * @param status         授权状态
 * @author refinex
 */
public record GrantEnvEntry(Long grantId, String environmentKey, String grantType, String status) {
}
