package com.refinex.dbflow.access.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * DBFlow API Token 元数据实体。
 *
 * @author refinex
 */
@Entity
@Table(name = "dbf_api_tokens")
public class DbfApiToken {

    /**
     * Token 主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 归属用户主键。
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Token hash，禁止保存 Token 明文。
     */
    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    /**
     * Token 前缀，用于后台展示和审计关联。
     */
    @Column(name = "token_prefix", nullable = false, length = 32)
    private String tokenPrefix;

    /**
     * Token 状态。
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /**
     * active 唯一约束标记；仅 ACTIVE 为 1，非 ACTIVE 必须为 null。
     */
    @Column(name = "active_flag")
    private Byte activeFlag;

    /**
     * 过期时间。
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * 最近使用时间。
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * 吊销时间。
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

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
     * 创建 active Token 元数据。
     *
     * @param userId      归属用户主键
     * @param tokenHash   Token hash
     * @param tokenPrefix Token 前缀
     * @param expiresAt   过期时间
     * @return API Token 元数据实体
     */
    public static DbfApiToken active(Long userId, String tokenHash, String tokenPrefix, Instant expiresAt) {
        DbfApiToken token = new DbfApiToken();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.tokenPrefix = tokenPrefix;
        token.status = "ACTIVE";
        token.activeFlag = (byte) 1;
        token.expiresAt = expiresAt;
        return token;
    }

    /**
     * 标记 Token 已吊销。
     *
     * @param revokedAt 吊销时间
     */
    public void revoke(Instant revokedAt) {
        this.status = "REVOKED";
        this.activeFlag = null;
        this.revokedAt = revokedAt;
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
     * 读取 Token 主键。
     *
     * @return Token 主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 读取归属用户主键。
     *
     * @return 归属用户主键
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 读取 Token 状态。
     *
     * @return Token 状态
     */
    public String getStatus() {
        return status;
    }
}
