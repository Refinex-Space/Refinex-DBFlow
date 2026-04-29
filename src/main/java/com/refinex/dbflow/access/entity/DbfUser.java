package com.refinex.dbflow.access.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * DBFlow 用户元数据实体。
 *
 * @author refinex
 */
@Entity
@Table(name = "dbf_users")
public class DbfUser {

    /**
     * 用户主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 唯一用户名。
     */
    @Column(name = "username", nullable = false, length = 128)
    private String username;

    /**
     * 用户展示名称。
     */
    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    /**
     * 管理端密码 hash；MCP Token 不使用该字段。
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * 用户状态。
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
     * 创建用户实体。
     *
     * @param username     唯一用户名
     * @param displayName  用户展示名称
     * @param passwordHash 管理端密码 hash
     * @return 用户实体
     */
    public static DbfUser create(String username, String displayName, String passwordHash) {
        DbfUser user = new DbfUser();
        user.username = username;
        user.displayName = displayName;
        user.passwordHash = passwordHash;
        user.status = "ACTIVE";
        return user;
    }

    /**
     * 禁用用户。
     */
    public void disable() {
        this.status = "DISABLED";
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
     * 读取用户主键。
     *
     * @return 用户主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 读取用户名。
     *
     * @return 唯一用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 读取用户展示名称。
     *
     * @return 用户展示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 读取管理端密码 hash。
     *
     * @return 管理端密码 hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 读取用户状态。
     *
     * @return 用户状态
     */
    public String getStatus() {
        return status;
    }
}
