package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.command.AuditQueryFilter;
import com.refinex.dbflow.audit.dto.AuditEventDetail;
import com.refinex.dbflow.audit.dto.AuditEventPageResponse;
import com.refinex.dbflow.audit.dto.AuditEventSummary;
import com.refinex.dbflow.audit.service.AuditQueryService;
import com.refinex.dbflow.common.ApiResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端审计查询控制器。
 *
 * @author refinex
 */
@RestController
@RequestMapping(value = "/admin/api/audit-events", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminAuditEventController {

    /**
     * 审计查询服务。
     */
    private final AuditQueryService auditQueryService;

    /**
     * 创建管理端审计查询控制器。
     *
     * @param auditQueryService 审计查询服务
     */
    public AdminAuditEventController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    /**
     * 分页查询审计事件。
     *
     * @param filter 查询筛选条件
     * @return 审计事件分页结果
     */
    @GetMapping
    public ApiResult<AuditEventPageResponse<AuditEventSummary>> list(@ModelAttribute AuditQueryFilter filter) {
        return ApiResult.ok(auditQueryService.query(filter.toCriteria()));
    }

    /**
     * 查询单条审计详情。
     *
     * @param id 审计事件主键
     * @return 审计事件详情
     */
    @GetMapping("/{id}")
    public ApiResult<AuditEventDetail> detail(@PathVariable Long id) {
        return ApiResult.ok(auditQueryService.getDetail(id));
    }
}
