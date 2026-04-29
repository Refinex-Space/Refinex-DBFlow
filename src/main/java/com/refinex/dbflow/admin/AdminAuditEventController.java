package com.refinex.dbflow.admin;

import com.refinex.dbflow.audit.service.*;
import com.refinex.dbflow.common.ApiResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

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
     * @param from      创建时间起点，ISO-8601 格式
     * @param to        创建时间终点，ISO-8601 格式
     * @param userId    用户主键
     * @param project   项目标识
     * @param env       环境标识
     * @param risk      风险级别
     * @param decision  审计决策
     * @param sqlHash   SQL hash
     * @param tool      工具名称
     * @param page      页码
     * @param size      每页条数
     * @param sort      排序字段
     * @param direction 排序方向
     * @return 审计事件分页结果
     */
    @GetMapping
    public ApiResult<AuditEventPageResponse<AuditEventSummary>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String env,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) String sqlHash,
            @RequestParam(required = false) String tool,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction
    ) {
        AuditQueryCriteria criteria = new AuditQueryCriteria(
                from,
                to,
                userId,
                project,
                env,
                risk,
                decision,
                sqlHash,
                tool,
                page,
                size,
                sort,
                direction
        );
        return ApiResult.ok(auditQueryService.query(criteria));
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
