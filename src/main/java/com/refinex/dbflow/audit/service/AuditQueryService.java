package com.refinex.dbflow.audit.service;

import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.repository.DbfAuditEventRepository;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

/**
 * 管理端审计查询服务。
 *
 * @author refinex
 */
@Service
public class AuditQueryService {

    /**
     * 默认页码。
     */
    private static final int DEFAULT_PAGE = 0;

    /**
     * 默认每页条数。
     */
    private static final int DEFAULT_SIZE = 20;

    /**
     * 最大每页条数，防止管理端误拉取过大结果集。
     */
    private static final int MAX_SIZE = 100;

    /**
     * API 排序字段到实体字段的白名单映射。
     */
    private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("userId", "userId"),
            Map.entry("project", "projectKey"),
            Map.entry("projectKey", "projectKey"),
            Map.entry("env", "environmentKey"),
            Map.entry("environmentKey", "environmentKey"),
            Map.entry("risk", "riskLevel"),
            Map.entry("riskLevel", "riskLevel"),
            Map.entry("decision", "decision"),
            Map.entry("sqlHash", "sqlHash"),
            Map.entry("tool", "tool")
    );

    /**
     * 审计事件 repository。
     */
    private final DbfAuditEventRepository auditEventRepository;

    /**
     * 创建管理端审计查询服务。
     *
     * @param auditEventRepository 审计事件 repository
     */
    public AuditQueryService(DbfAuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * 按条件分页查询审计事件。
     *
     * @param criteria 查询条件
     * @return 分页审计摘要
     */
    @Transactional(readOnly = true)
    public AuditEventPageResponse<AuditEventSummary> query(AuditQueryCriteria criteria) {
        AuditQueryCriteria safeCriteria = criteria == null
                ? new AuditQueryCriteria(null, null, null, null, null, null, null, null, null, null, null, null, null)
                : criteria;
        Pageable pageable = pageable(safeCriteria);
        Page<AuditEventSummary> page = auditEventRepository.findAll(specification(safeCriteria), pageable)
                .map(this::toSummary);
        return new AuditEventPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                requestedSort(safeCriteria.sort()),
                requestedDirection(safeCriteria.direction()).name().toLowerCase()
        );
    }

    /**
     * 查询单条审计详情。
     *
     * @param id 审计事件主键
     * @return 脱敏审计详情
     * @throws DbflowException 审计事件不存在时抛出
     */
    @Transactional(readOnly = true)
    public AuditEventDetail getDetail(Long id) {
        if (id == null) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "审计事件 id 不能为空");
        }
        DbfAuditEvent event = auditEventRepository.findById(id)
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "审计事件不存在"));
        return toDetail(event);
    }

    /**
     * 构建审计查询 Specification。
     *
     * @param criteria 查询条件
     * @return 查询 Specification
     */
    private Specification<DbfAuditEvent> specification(AuditQueryCriteria criteria) {
        return Specification.allOf(
                greaterThanOrEqual("createdAt", criteria.from()),
                lessThanOrEqual("createdAt", criteria.to()),
                equal("userId", criteria.userId()),
                equalText("projectKey", criteria.projectKey()),
                equalText("environmentKey", criteria.environmentKey()),
                equalText("riskLevel", criteria.riskLevel()),
                equalText("decision", criteria.decision()),
                equalText("sqlHash", criteria.sqlHash()),
                equalText("tool", criteria.tool())
        );
    }

    /**
     * 构建分页排序对象。
     *
     * @param criteria 查询条件
     * @return 分页排序对象
     */
    private Pageable pageable(AuditQueryCriteria criteria) {
        int page = criteria.page() == null || criteria.page() < 0 ? DEFAULT_PAGE : criteria.page();
        int size = criteria.size() == null || criteria.size() <= 0 ? DEFAULT_SIZE : Math.min(criteria.size(), MAX_SIZE);
        Sort sort = Sort.by(requestedDirection(criteria.direction()), requestedSort(criteria.sort()));
        return PageRequest.of(page, size, sort);
    }

    /**
     * 解析排序字段。
     *
     * @param sort 排序字段请求值
     * @return 实体排序字段
     */
    private String requestedSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "createdAt";
        }
        return SORT_FIELDS.getOrDefault(sort, "createdAt");
    }

    /**
     * 解析排序方向。
     *
     * @param direction 排序方向请求值
     * @return Spring Data 排序方向
     */
    private Sort.Direction requestedDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /**
     * 构建大于等于时间谓词。
     *
     * @param field 字段名
     * @param value 时间值
     * @return 查询谓词
     */
    private Specification<DbfAuditEvent> greaterThanOrEqual(String field, Instant value) {
        return value == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get(field), value);
    }

    /**
     * 构建小于等于时间谓词。
     *
     * @param field 字段名
     * @param value 时间值
     * @return 查询谓词
     */
    private Specification<DbfAuditEvent> lessThanOrEqual(String field, Instant value) {
        return value == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get(field), value);
    }

    /**
     * 构建数值等值谓词。
     *
     * @param field 字段名
     * @param value 数值
     * @return 查询谓词
     */
    private Specification<DbfAuditEvent> equal(String field, Long value) {
        return value == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(field), value);
    }

    /**
     * 构建文本等值谓词。
     *
     * @param field 字段名
     * @param value 文本值
     * @return 查询谓词
     */
    private Specification<DbfAuditEvent> equalText(String field, String value) {
        return !StringUtils.hasText(value) ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(field), value.trim());
    }

    /**
     * 映射审计列表摘要。
     *
     * @param event 审计事件
     * @return 审计摘要
     */
    private AuditEventSummary toSummary(DbfAuditEvent event) {
        return new AuditEventSummary(
                event.getId(),
                event.getRequestId(),
                event.getUserId(),
                event.getProjectKey(),
                event.getEnvironmentKey(),
                event.getClientName(),
                event.getClientVersion(),
                event.getTool(),
                event.getOperationType(),
                event.getRiskLevel(),
                event.getStatus(),
                event.getDecision(),
                event.getSqlHash(),
                AuditTextSanitizer.sanitize(event.getResultSummary()),
                event.getAffectedRows(),
                event.getCreatedAt()
        );
    }

    /**
     * 映射审计详情。
     *
     * @param event 审计事件
     * @return 审计详情
     */
    private AuditEventDetail toDetail(DbfAuditEvent event) {
        return new AuditEventDetail(
                event.getId(),
                event.getRequestId(),
                event.getUserId(),
                event.getProjectKey(),
                event.getEnvironmentKey(),
                event.getClientName(),
                event.getClientVersion(),
                event.getUserAgent(),
                event.getSourceIp(),
                event.getTool(),
                event.getOperationType(),
                event.getRiskLevel(),
                event.getStatus(),
                event.getDecision(),
                event.getSqlHash(),
                AuditTextSanitizer.sanitize(event.getSqlText()),
                AuditTextSanitizer.sanitize(event.getResultSummary()),
                event.getAffectedRows(),
                event.getErrorCode(),
                AuditTextSanitizer.sanitize(event.getErrorMessage()),
                event.getConfirmationId(),
                event.getCreatedAt()
        );
    }
}
