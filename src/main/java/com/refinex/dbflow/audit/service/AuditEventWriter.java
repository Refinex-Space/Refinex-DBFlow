package com.refinex.dbflow.audit.service;

import com.refinex.dbflow.audit.dto.AuditEventWriteRequest;
import com.refinex.dbflow.audit.dto.AuditRequestContext;
import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.observability.service.DbflowMetricsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 统一审计事件写入器。
 *
 * @author refinex
 */
@Service
public class AuditEventWriter {

    /**
     * 请求已接收状态。
     */
    private static final String STATUS_REQUEST_RECEIVED = "REQUEST_RECEIVED";

    /**
     * 拒绝状态。
     */
    private static final String STATUS_DENIED = "DENIED";

    /**
     * 需要确认状态。
     */
    private static final String STATUS_REQUIRES_CONFIRMATION = "REQUIRES_CONFIRMATION";

    /**
     * 执行成功状态。
     */
    private static final String STATUS_EXECUTED = "ALLOWED_EXECUTED";

    /**
     * 失败状态。
     */
    private static final String STATUS_FAILED = "FAILED";

    /**
     * 确认成功状态。
     */
    private static final String STATUS_CONFIRMATION_CONFIRMED = "CONFIRMATION_CONFIRMED";

    /**
     * 确认过期状态。
     */
    private static final String STATUS_CONFIRMATION_EXPIRED = "CONFIRMATION_EXPIRED";

    /**
     * 结果摘要最大长度。
     */
    private static final int MAX_RESULT_SUMMARY_LENGTH = 512;

    /**
     * 审计服务。
     */
    private final AuditService auditService;

    /**
     * DBFlow 指标服务，JPA slice 测试中允许不存在。
     */
    private final DbflowMetricsService metricsService;

    /**
     * 创建统一审计事件写入器。
     *
     * @param auditService            审计服务
     * @param metricsServiceProvider  DBFlow 指标服务 provider
     */
    public AuditEventWriter(
            AuditService auditService,
            ObjectProvider<DbflowMetricsService> metricsServiceProvider
    ) {
        this.auditService = auditService;
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    /**
     * 写入请求已接收事件。
     *
     * @param request 审计写入请求
     * @return 已保存审计事件
     */
    public DbfAuditEvent requestReceived(AuditEventWriteRequest request) {
        return write(request, STATUS_REQUEST_RECEIVED, "REQUEST_RECEIVED");
    }

    /**
     * 写入策略拒绝事件。
     *
     * @param request 审计写入请求
     * @return 已保存审计事件
     */
    public DbfAuditEvent policyDenied(AuditEventWriteRequest request) {
        return write(request, STATUS_DENIED, "POLICY_DENIED");
    }

    /**
     * 写入需要确认事件。
     *
     * @param request 审计写入请求
     * @return 已保存审计事件
     */
    public DbfAuditEvent requiresConfirmation(AuditEventWriteRequest request) {
        return write(request, STATUS_REQUIRES_CONFIRMATION, "REQUIRES_CONFIRMATION");
    }

    /**
     * 写入执行成功事件。
     *
     * @param request 审计写入请求
     * @return 已保存审计事件
     */
    public DbfAuditEvent executed(AuditEventWriteRequest request) {
        return write(request, STATUS_EXECUTED, "EXECUTED");
    }

    /**
     * 写入失败事件。
     *
     * @param request 审计写入请求
     * @return 已保存审计事件
     */
    public DbfAuditEvent failed(AuditEventWriteRequest request) {
        return write(request, STATUS_FAILED, "FAILED");
    }

    /**
     * 写入确认成功事件。
     *
     * @param request 审计写入请求
     * @return 已保存审计事件
     */
    public DbfAuditEvent confirmationConfirmed(AuditEventWriteRequest request) {
        return write(request, STATUS_CONFIRMATION_CONFIRMED, "CONFIRMATION_CONFIRMED");
    }

    /**
     * 写入确认过期事件。
     *
     * @param request 审计写入请求
     * @return 已保存审计事件
     */
    public DbfAuditEvent confirmationExpired(AuditEventWriteRequest request) {
        return write(request, STATUS_CONFIRMATION_EXPIRED, "CONFIRMATION_EXPIRED");
    }

    /**
     * 写入审计事件。
     *
     * @param request  审计写入请求
     * @param status   审计状态
     * @param decision 审计决策
     * @return 已保存审计事件
     */
    private DbfAuditEvent write(AuditEventWriteRequest request, String status, String decision) {
        AuditRequestContext context = request.effectiveContext();
        DbfAuditEvent saved = auditService.record(DbfAuditEvent.auditEvent(
                valueOrUnknown(request.requestId()),
                request.userId(),
                request.tokenId(),
                truncate(request.tokenPrefix(), 32),
                valueOrUnknown(request.projectKey()),
                valueOrUnknown(request.environmentKey()),
                truncate(valueOrUnknown(context.clientName()), 128),
                truncate(valueOrUnknown(context.clientVersion()), 64),
                truncate(valueOrUnknown(context.userAgent()), 255),
                truncate(valueOrUnknown(context.sourceIp()), 64),
                truncate(valueOrUnknown(context.tool()), 128),
                truncate(valueOrUnknown(request.operation()), 64),
                truncate(valueOrDefault(request.riskLevel(), "LOW"), 32),
                status,
                decision,
                truncate(request.sqlHash(), 128),
                safeSqlText(request.sqlText()),
                boundedSummary(request.resultSummary()),
                request.affectedRows(),
                truncate(request.errorCode(), 128),
                boundedSummary(request.errorMessage()),
                truncate(request.confirmationId(), 64)
        ));
        recordMetrics(request, context, decision);
        return saved;
    }

    /**
     * 写入审计派生指标。
     *
     * @param request  审计写入请求
     * @param context  审计来源上下文
     * @param decision 审计决策
     */
    private void recordMetrics(AuditEventWriteRequest request, AuditRequestContext context, String decision) {
        if (metricsService == null || STATUS_REQUEST_RECEIVED.equals(decision)) {
            return;
        }
        metricsService.recordSqlRisk(context.tool(), request.operation(), request.riskLevel(), decision);
        if (isRejectedDecision(decision)) {
            metricsService.recordRejection(context.tool(), request.operation(), request.riskLevel(), decision);
        }
    }

    /**
     * 判断审计决策是否属于拒绝。
     *
     * @param decision 审计决策
     * @return 拒绝时返回 true
     */
    private boolean isRejectedDecision(String decision) {
        return "POLICY_DENIED".equalsIgnoreCase(decision) || "DENIED".equalsIgnoreCase(decision);
    }

    /**
     * 生成有界结果摘要。
     *
     * @param value 原始摘要
     * @return 有界摘要
     */
    private String boundedSummary(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() <= MAX_RESULT_SUMMARY_LENGTH) {
            return normalized;
        }
        String suffix = "...[truncated]";
        return normalized.substring(0, MAX_RESULT_SUMMARY_LENGTH - suffix.length()) + suffix;
    }

    /**
     * 清理 SQL 文本。
     *
     * @param value SQL 原文
     * @return 清理后的 SQL
     */
    private String safeSqlText(String value) {
        return StringUtils.hasText(value) ? value.strip() : null;
    }

    /**
     * 返回非空值或 unknown。
     *
     * @param value 原始值
     * @return 非空值或 unknown
     */
    private String valueOrUnknown(String value) {
        return valueOrDefault(value, "unknown");
    }

    /**
     * 返回非空值或默认值。
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @return 非空值或默认值
     */
    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * 裁剪字段长度。
     *
     * @param value  原始值
     * @param length 最大长度
     * @return 裁剪后的值
     */
    private String truncate(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }
        return value.substring(0, length);
    }
}
