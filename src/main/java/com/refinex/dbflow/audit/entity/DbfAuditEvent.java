package com.refinex.dbflow.audit.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * DBFlow 操作审计事件实体。
 *
 * @author refinex
 */
@Entity
@Table(name = "dbf_audit_events")
public class DbfAuditEvent {

    /**
     * 审计事件主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 请求标识。
     */
    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    /**
     * 用户主键。
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Token 前缀。
     */
    @Column(name = "token_prefix", length = 32)
    private String tokenPrefix;

    /**
     * 项目标识。
     */
    @Column(name = "project_key", nullable = false, length = 128)
    private String projectKey;

    /**
     * 环境标识。
     */
    @Column(name = "environment_key", nullable = false, length = 128)
    private String environmentKey;

    /**
     * MCP 客户端名称。
     */
    @Column(name = "client_name", length = 128)
    private String clientName;

    /**
     * MCP 客户端版本。
     */
    @Column(name = "client_version", length = 64)
    private String clientVersion;

    /**
     * 来源 IP。
     */
    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    /**
     * 操作类型。
     */
    @Column(name = "operation_type", nullable = false, length = 64)
    private String operationType;

    /**
     * 风险级别。
     */
    @Column(name = "risk_level", nullable = false, length = 32)
    private String riskLevel;

    /**
     * 执行状态。
     */
    @Column(name = "status", nullable = false, length = 64)
    private String status;

    /**
     * SQL hash。
     */
    @Column(name = "sql_hash", length = 128)
    private String sqlHash;

    /**
     * SQL 原文；不包含完整结果集。
     */
    @Column(name = "sql_text", columnDefinition = "TEXT")
    private String sqlText;

    /**
     * 执行结果摘要；禁止保存完整结果集。
     */
    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    /**
     * 影响行数。
     */
    @Column(name = "affected_rows")
    private Long affectedRows;

    /**
     * 错误码。
     */
    @Column(name = "error_code", length = 128)
    private String errorCode;

    /**
     * 错误信息摘要。
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 关联确认标识。
     */
    @Column(name = "confirmation_id", length = 64)
    private String confirmationId;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 创建审计事件。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param operationType  操作类型
     * @param riskLevel      风险级别
     * @param status         执行状态
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param resultSummary  结果摘要
     * @param affectedRows   影响行数
     * @return 审计事件实体
     */
    public static DbfAuditEvent executed(
            String requestId,
            Long userId,
            String projectKey,
            String environmentKey,
            String operationType,
            String riskLevel,
            String status,
            String sqlHash,
            String sqlText,
            String resultSummary,
            Long affectedRows
    ) {
        DbfAuditEvent event = new DbfAuditEvent();
        event.requestId = requestId;
        event.userId = userId;
        event.projectKey = projectKey;
        event.environmentKey = environmentKey;
        event.operationType = operationType;
        event.riskLevel = riskLevel;
        event.status = status;
        event.sqlHash = sqlHash;
        event.sqlText = sqlText;
        event.resultSummary = resultSummary;
        event.affectedRows = affectedRows;
        return event;
    }

    /**
     * 创建确认挑战审计事件。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param tokenPrefix    Token 前缀
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param status         确认状态
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param confirmationId 确认挑战标识
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     * @return 审计事件实体
     */
    public static DbfAuditEvent confirmation(
            String requestId,
            Long userId,
            String tokenPrefix,
            String projectKey,
            String environmentKey,
            String status,
            String sqlHash,
            String sqlText,
            String confirmationId,
            String errorCode,
            String errorMessage
    ) {
        DbfAuditEvent event = new DbfAuditEvent();
        event.requestId = requestId;
        event.userId = userId;
        event.tokenPrefix = tokenPrefix;
        event.projectKey = projectKey;
        event.environmentKey = environmentKey;
        event.operationType = "TRUNCATE";
        event.riskLevel = "CRITICAL";
        event.status = status;
        event.sqlHash = sqlHash;
        event.sqlText = sqlText;
        event.resultSummary = "TRUNCATE 确认挑战状态变化";
        event.affectedRows = 0L;
        event.confirmationId = confirmationId;
        event.errorCode = errorCode;
        event.errorMessage = errorMessage;
        return event;
    }

    /**
     * 持久化前补齐创建时间。
     */
    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    /**
     * 读取审计事件主键。
     *
     * @return 审计事件主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 读取请求标识。
     *
     * @return 请求标识
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 读取执行状态。
     *
     * @return 执行状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 读取确认挑战标识。
     *
     * @return 确认挑战标识
     */
    public String getConfirmationId() {
        return confirmationId;
    }
}
