package com.refinex.dbflow.audit.service;

import java.util.List;

/**
 * 管理端分页响应模型。
 *
 * @param content       当前页内容
 * @param page          当前页码
 * @param size          每页条数
 * @param totalElements 总记录数
 * @param totalPages    总页数
 * @param sort          排序字段
 * @param direction     排序方向
 * @param <T>           分页内容类型
 * @author refinex
 */
public record AuditEventPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort,
        String direction
) {
}
