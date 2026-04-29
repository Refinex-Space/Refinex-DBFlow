package com.refinex.dbflow.audit.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * DBFlow SQL 二次确认挑战实体。
 *
 * @author refinex
 */
@Entity
@Table(name = "dbf_confirmation_challenges")
public class DbfConfirmationChallenge {

    /**
     * 确认挑战主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 用户主键。
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * MCP Token 主键。
     */
    @Column(name = "token_id", nullable = false)
    private Long tokenId;

    /**
     * 环境主键。
     */
    @Column(name = "environment_id", nullable = false)
    private Long environmentId;

    /**
     * 项目标识快照。
     */
    @Column(name = "project_key", nullable = false, length = 128)
    private String projectKey;

    /**
     * 环境标识快照。
     */
    @Column(name = "environment_key", nullable = false, length = 128)
    private String environmentKey;

    /**
     * 对外确认标识。
     */
    @Column(name = "confirmation_id", nullable = false, length = 64)
    private String confirmationId;

    /**
     * SQL hash。
     */
    @Column(name = "sql_hash", nullable = false, length = 128)
    private String sqlHash;

    /**
     * 待确认 SQL 原文。
     */
    @Column(name = "sql_text", nullable = false, columnDefinition = "TEXT")
    private String sqlText;

    /**
     * 风险级别。
     */
    @Column(name = "risk_level", nullable = false, length = 32)
    private String riskLevel;

    /**
     * 确认挑战状态。
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /**
     * 过期时间。
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 确认时间。
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 更新时间。
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 创建待确认挑战。
     *
     * @param userId         用户主键
     * @param tokenId        MCP Token 主键
     * @param environmentId  环境主键
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param confirmationId 对外确认标识
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param riskLevel      风险级别
     * @param expiresAt      过期时间
     * @return 确认挑战实体
     */
    public static DbfConfirmationChallenge pending(
            Long userId,
            Long tokenId,
            Long environmentId,
            String projectKey,
            String environmentKey,
            String confirmationId,
            String sqlHash,
            String sqlText,
            String riskLevel,
            Instant expiresAt
    ) {
        DbfConfirmationChallenge challenge = new DbfConfirmationChallenge();
        challenge.userId = userId;
        challenge.tokenId = tokenId;
        challenge.environmentId = environmentId;
        challenge.projectKey = projectKey;
        challenge.environmentKey = environmentKey;
        challenge.confirmationId = confirmationId;
        challenge.sqlHash = sqlHash;
        challenge.sqlText = sqlText;
        challenge.riskLevel = riskLevel;
        challenge.status = "PENDING";
        challenge.expiresAt = expiresAt;
        return challenge;
    }

    /**
     * 标记挑战已确认。
     *
     * @param confirmedAt 确认时间
     */
    public void confirm(Instant confirmedAt) {
        this.status = "CONFIRMED";
        this.confirmedAt = confirmedAt;
    }

    /**
     * 标记挑战已过期。
     */
    public void expire() {
        this.status = "EXPIRED";
    }

    /**
     * 持久化前补齐时间字段。
     */
    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 更新前刷新更新时间。
     */
    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * 读取确认挑战主键。
     *
     * @return 确认挑战主键
     */
    public Long getId() {
        return id;
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
     * 读取 MCP Token 主键。
     *
     * @return MCP Token 主键
     */
    public Long getTokenId() {
        return tokenId;
    }

    /**
     * 读取环境主键。
     *
     * @return 环境主键
     */
    public Long getEnvironmentId() {
        return environmentId;
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
     * 读取确认标识。
     *
     * @return 对外确认标识
     */
    public String getConfirmationId() {
        return confirmationId;
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
     * 读取风险级别。
     *
     * @return 风险级别
     */
    public String getRiskLevel() {
        return riskLevel;
    }

    /**
     * 读取确认状态。
     *
     * @return 确认状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 读取过期时间。
     *
     * @return 过期时间
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * 读取确认时间。
     *
     * @return 确认时间
     */
    public Instant getConfirmedAt() {
        return confirmedAt;
    }
}
