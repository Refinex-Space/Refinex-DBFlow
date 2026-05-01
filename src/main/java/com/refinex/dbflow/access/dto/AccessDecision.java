package com.refinex.dbflow.access.dto;

import com.refinex.dbflow.access.model.AccessDecisionReason;

/**
 * 项目环境访问判断结果。
 *
 * @param allowed 是否允许访问
 * @param reason  判断原因
 * @param message 判断说明
 * @author refinex
 */
public record AccessDecision(
        boolean allowed,
        AccessDecisionReason reason,
        String message
) {

    /**
     * 创建允许访问结果。
     *
     * @return 允许访问结果
     */
    public static AccessDecision allow() {
        return new AccessDecision(true, AccessDecisionReason.ALLOWED, "允许访问项目环境");
    }

    /**
     * 创建拒绝访问结果。
     *
     * @param reason  拒绝原因
     * @param message 拒绝说明
     * @return 拒绝访问结果
     */
    public static AccessDecision deny(AccessDecisionReason reason, String message) {
        return new AccessDecision(false, reason, message);
    }
}
