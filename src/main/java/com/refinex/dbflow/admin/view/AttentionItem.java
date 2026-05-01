package com.refinex.dbflow.admin.view;

/**
 * 关注事项。
 *
 * @param label  标签
 * @param status 状态
 * @param tone   色调
 * @param href   链接
 * @author refinex
 */
public record AttentionItem(String label, String status, String tone, String href) {
}
