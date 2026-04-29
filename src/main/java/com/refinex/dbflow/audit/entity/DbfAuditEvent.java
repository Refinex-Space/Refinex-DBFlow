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
     * Token 元数据主键。
     */
    @Column(name = "token_id")
    private Long tokenId;

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
     * HTTP User-Agent。
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * 来源 IP。
     */
    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    /**
     * MCP 工具名称。
     */
    @Column(name = "tool", length = 128)
    private String tool;

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
     * 审计决策。
     */
    @Column(name = "decision", length = 64)
    private String decision;

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
     * 创建 SQL 执行审计事件。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param tokenPrefix    Token 前缀
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param operationType  操作类型
     * @param riskLevel      风险级别
     * @param status         执行状态
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param resultSummary  结果摘要
     * @param affectedRows   影响行数
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     * @return 审计事件实体
     */
    public static DbfAuditEvent sqlExecution(
            String requestId,
            Long userId,
            String tokenPrefix,
            String projectKey,
            String environmentKey,
            String operationType,
            String riskLevel,
            String status,
            String sqlHash,
            String sqlText,
            String resultSummary,
            Long affectedRows,
            String errorCode,
            String errorMessage
    ) {
        DbfAuditEvent event = executed(
                requestId,
                userId,
                projectKey,
                environmentKey,
                operationType,
                riskLevel,
                status,
                sqlHash,
                sqlText,
                resultSummary,
                affectedRows
        );
        event.tokenPrefix = tokenPrefix;
        event.errorCode = errorCode;
        event.errorMessage = errorMessage;
        return event;
    }

    /**
     * 创建统一审计事件。
     *
     * @param requestId      请求标识
     * @param userId         用户主键
     * @param tokenId        Token 元数据主键
     * @param tokenPrefix    Token 展示前缀
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param clientName     客户端名称
     * @param clientVersion  客户端版本
     * @param userAgent      HTTP User-Agent
     * @param sourceIp       来源 IP
     * @param tool           MCP 工具名称
     * @param operationType  操作类型
     * @param riskLevel      风险级别
     * @param status         审计状态
     * @param decision       审计决策
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param resultSummary  结果摘要
     * @param affectedRows   影响行数
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     * @param confirmationId 确认挑战标识
     * @return 审计事件实体
     */
    public static DbfAuditEvent auditEvent(
            String requestId,
            Long userId,
            Long tokenId,
            String tokenPrefix,
            String projectKey,
            String environmentKey,
            String clientName,
            String clientVersion,
            String userAgent,
            String sourceIp,
            String tool,
            String operationType,
            String riskLevel,
            String status,
            String decision,
            String sqlHash,
            String sqlText,
            String resultSummary,
            Long affectedRows,
            String errorCode,
            String errorMessage,
            String confirmationId
    ) {
        DbfAuditEvent event = new DbfAuditEvent();
        event.requestId = requestId;
        event.userId = userId;
        event.tokenId = tokenId;
        event.tokenPrefix = tokenPrefix;
        event.projectKey = projectKey;
        event.environmentKey = environmentKey;
        event.clientName = clientName;
        event.clientVersion = clientVersion;
        event.userAgent = userAgent;
        event.sourceIp = sourceIp;
        event.tool = tool;
        event.operationType = operationType;
        event.riskLevel = riskLevel;
        event.status = status;
        event.decision = decision;
        event.sqlHash = sqlHash;
        event.sqlText = sqlText;
        event.resultSummary = resultSummary;
        event.affectedRows = affectedRows;
        event.errorCode = errorCode;
        event.errorMessage = errorMessage;
        event.confirmationId = confirmationId;
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
     * 读取用户主键。
     *
     * @return 用户主键
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 读取 Token 元数据主键。
     *
     * @return Token 元数据主键
     */
    public Long getTokenId() {
        return tokenId;
    }

    /**
     * 读取 Token 前缀。
     *
     * @return Token 前缀
     */
    public String getTokenPrefix() {
        return tokenPrefix;
    }

    /**
     * 读取项目标识。
     *
     * @return 项目标识
     */
    public String getProjectKey() {
        return projectKey;
    }

    /**
     * 读取环境标识。
     *
     * @return 环境标识
     */
    public String getEnvironmentKey() {
        return environmentKey;
    }

    /**
     * 读取客户端名称。
     *
     * @return 客户端名称
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * 读取客户端版本。
     *
     * @return 客户端版本
     */
    public String getClientVersion() {
        return clientVersion;
    }

    /**
     * 读取 HTTP User-Agent。
     *
     * @return HTTP User-Agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * 读取来源 IP。
     *
     * @return 来源 IP
     */
    public String getSourceIp() {
        return sourceIp;
    }

    /**
     * 读取 MCP 工具名称。
     *
     * @return MCP 工具名称
     */
    public String getTool() {
        return tool;
    }

    /**
     * 读取操作类型。
     *
     * @return 操作类型
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * 读取风险级别。
     *
     * @return 风险级别
     */
    public String getRiskLevel() {
        return riskLevel;
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
     * 读取审计决策。
     *
     * @return 审计决策
     */
    public String getDecision() {
        return decision;
    }

    /**
     * 读取 SQL hash。
     *
     * @return SQL hash
     */
    public String getSqlHash() {
        return sqlHash;
    }

    /**
     * 读取 SQL 原文。
     *
     * @return SQL 原文
     */
    public String getSqlText() {
        return sqlText;
    }

    /**
     * 读取结果摘要。
     *
     * @return 结果摘要
     */
    public String getResultSummary() {
        return resultSummary;
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
