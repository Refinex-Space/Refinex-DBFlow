package com.refinex.dbflow.admin.view;

import java.util.List;

/**
 * 审计列表页面视图。
 *
 * @param rows          当前页审计摘要行
 * @param filter        筛选值
 * @param page          当前页码
 * @param size          每页条数
 * @param totalElements 总记录数
 * @param totalPages    总页数
 * @param sort          排序字段
 * @param direction     排序方向
 * @param hasPrevious   是否存在上一页
 * @param hasNext       是否存在下一页
 * @param previousPage  上一页页码
 * @param nextPage      下一页页码
 * @param previousUrl   上一页链接
 * @param nextUrl       下一页链接
 * @param firstItem     当前页第一条序号
 * @param lastItem      当前页最后一条序号
 * @author refinex
 */
public record AuditPageView(
        List<AuditSummaryRow> rows,
        AuditFilterView filter,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort,
        String direction,
        boolean hasPrevious,
        boolean hasNext,
        int previousPage,
        int nextPage,
        String previousUrl,
        String nextUrl,
        long firstItem,
        long lastItem
) {
}
