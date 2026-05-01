package com.refinex.dbflow.admin.view;

import com.refinex.dbflow.audit.dto.AuditEventDetail;

/**
 * 审计详情页面视图。
 *
 * @param detail           审计详情
 * @param createdAt        创建时间展示文本
 * @param userText         用户展示文本
 * @param affectedRowsText 影响行数展示文本
 * @param confirmationText 确认标识展示文本
 * @param failureReason    失败或拒绝原因
 * @author refinex
 */
public record AuditDetailPageView(
        AuditEventDetail detail,
        String createdAt,
        String userText,
        String affectedRowsText,
        String confirmationText,
        String failureReason
) {
}
