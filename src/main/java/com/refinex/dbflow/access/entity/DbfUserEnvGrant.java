package com.refinex.dbflow.access.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * DBFlow 用户环境授权实体。
 *
 * @author refinex
 */
@Entity
@Table(name = "dbf_user_env_grants")
public class DbfUserEnvGrant {

    /**
     * 授权主键。
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
     * 环境主键。
     */
    @Column(name = "environment_id", nullable = false)
    private Long environmentId;

    /**
     * 授权类型。
     */
    @Column(name = "grant_type", nullable = false, length = 32)
    private String grantType;

    /**
     * 授权状态。
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

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
     * 创建授权实体。
     *
     * @param userId        用户主键
     * @param environmentId 环境主键
     * @param grantType     授权类型
     * @return 用户环境授权实体
     */
    public static DbfUserEnvGrant active(Long userId, Long environmentId, String grantType) {
        DbfUserEnvGrant grant = new DbfUserEnvGrant();
        grant.userId = userId;
        grant.environmentId = environmentId;
        grant.grantType = grantType;
        grant.status = "ACTIVE";
        return grant;
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
     * 读取授权主键。
     *
     * @return 授权主键
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
     * 读取环境主键。
     *
     * @return 环境主键
     */
    public Long getEnvironmentId() {
        return environmentId;
    }

    /**
     * 读取授权类型。
     *
     * @return 授权类型
     */
    public String getGrantType() {
        return grantType;
    }

    /**
     * 读取授权状态。
     *
     * @return 授权状态
     */
    public String getStatus() {
        return status;
    }
}
