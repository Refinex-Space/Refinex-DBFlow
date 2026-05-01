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
     * 创建审计事件构建器。
     *
     * @return 审计事件构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 审计事件构建器。
     */
    public static final class Builder {

        /**
         * 请求标识。
         */
        private String requestId;
        /**
         * 用户主键。
         */
        private Long userId;
        /**
         * Token 元数据主键。
         */
        private Long tokenId;
        /**
         * Token 前缀。
         */
        private String tokenPrefix;
        /**
         * 项目标识。
         */
        private String projectKey;
        /**
         * 环境标识。
         */
        private String environmentKey;
        /**
         * 客户端名称。
         */
        private String clientName;
        /**
         * 客户端版本。
         */
        private String clientVersion;
        /**
         * HTTP User-Agent。
         */
        private String userAgent;
        /**
         * 来源 IP。
         */
        private String sourceIp;
        /**
         * MCP 工具名称。
         */
        private String tool;
        /**
         * 操作类型。
         */
        private String operationType;
        /**
         * 风险级别。
         */
        private String riskLevel;
        /**
         * 执行状态。
         */
        private String status;
        /**
         * 审计决策。
         */
        private String decision;
        /**
         * SQL hash。
         */
        private String sqlHash;
        /**
         * SQL 原文。
         */
        private String sqlText;
        /**
         * 结果摘要。
         */
        private String resultSummary;
        /**
         * 影响行数。
         */
        private Long affectedRows;
        /**
         * 错误码。
         */
        private String errorCode;
        /**
         * 错误信息摘要。
         */
        private String errorMessage;
        /**
         * 确认挑战标识。
         */
        private String confirmationId;

        /**
         * 禁止外部直接实例化。
         */
        private Builder() {
        }

        /**
         * @param requestId 请求标识 @return this
         */
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * @param userId 用户主键 @return this
         */
        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @param tokenId Token 元数据主键 @return this
         */
        public Builder tokenId(Long tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        /**
         * @param tokenPrefix Token 前缀 @return this
         */
        public Builder tokenPrefix(String tokenPrefix) {
            this.tokenPrefix = tokenPrefix;
            return this;
        }

        /**
         * @param projectKey 项目标识 @return this
         */
        public Builder projectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        /**
         * @param environmentKey 环境标识 @return this
         */
        public Builder environmentKey(String environmentKey) {
            this.environmentKey = environmentKey;
            return this;
        }

        /**
         * @param clientName 客户端名称 @return this
         */
        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        /**
         * @param clientVersion 客户端版本 @return this
         */
        public Builder clientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        /**
         * @param userAgent HTTP User-Agent @return this
         */
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * @param sourceIp 来源 IP @return this
         */
        public Builder sourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
            return this;
        }

        /**
         * @param tool MCP 工具名称 @return this
         */
        public Builder tool(String tool) {
            this.tool = tool;
            return this;
        }

        /**
         * @param operationType 操作类型 @return this
         */
        public Builder operationType(String operationType) {
            this.operationType = operationType;
            return this;
        }

        /**
         * @param riskLevel 风险级别 @return this
         */
        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        /**
         * @param status 执行状态 @return this
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * @param decision 审计决策 @return this
         */
        public Builder decision(String decision) {
            this.decision = decision;
            return this;
        }

        /**
         * @param sqlHash SQL hash @return this
         */
        public Builder sqlHash(String sqlHash) {
            this.sqlHash = sqlHash;
            return this;
        }

        /**
         * @param sqlText SQL 原文 @return this
         */
        public Builder sqlText(String sqlText) {
            this.sqlText = sqlText;
            return this;
        }

        /**
         * @param resultSummary 结果摘要 @return this
         */
        public Builder resultSummary(String resultSummary) {
            this.resultSummary = resultSummary;
            return this;
        }

        /**
         * @param affectedRows 影响行数 @return this
         */
        public Builder affectedRows(Long affectedRows) {
            this.affectedRows = affectedRows;
            return this;
        }

        /**
         * @param errorCode 错误码 @return this
         */
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * @param errorMessage 错误信息摘要 @return this
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @param confirmationId 确认挑战标识 @return this
         */
        public Builder confirmationId(String confirmationId) {
            this.confirmationId = confirmationId;
            return this;
        }

        /**
         * 构造审计事件实体。
         *
         * @return 审计事件实体
         */
        public DbfAuditEvent build() {
            DbfAuditEvent event = new DbfAuditEvent();
            event.requestId = this.requestId;
            event.userId = this.userId;
            event.tokenId = this.tokenId;
            event.tokenPrefix = this.tokenPrefix;
            event.projectKey = this.projectKey;
            event.environmentKey = this.environmentKey;
            event.clientName = this.clientName;
            event.clientVersion = this.clientVersion;
            event.userAgent = this.userAgent;
            event.sourceIp = this.sourceIp;
            event.tool = this.tool;
            event.operationType = this.operationType;
            event.riskLevel = this.riskLevel;
            event.decision = this.decision;
            event.status = statusOrDefault();
            event.sqlHash = this.sqlHash;
            event.sqlText = this.sqlText;
            event.resultSummary = this.resultSummary;
            event.affectedRows = this.affectedRows;
            event.errorCode = this.errorCode;
            event.errorMessage = this.errorMessage;
            event.confirmationId = this.confirmationId;
            return event;
        }

        /**
         * 解析非空审计状态，兼容只设置 decision 的旧调用路径。
         *
         * @return 审计状态
         */
        private String statusOrDefault() {
            if (status != null && !status.isBlank()) {
                return status;
            }
            if (decision != null && !decision.isBlank()) {
                return decision;
            }
            return "UNKNOWN";
        }
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
     * 读取影响行数。
     *
     * @return 影响行数
     */
    public Long getAffectedRows() {
        return affectedRows;
    }

    /**
     * 读取错误码。
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 读取错误信息摘要。
     *
     * @return 错误信息摘要
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 读取确认挑战标识。
     *
     * @return 确认挑战标识
     */
    public String getConfirmationId() {
        return confirmationId;
    }

    /**
     * 读取创建时间。
     *
     * @return 创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
