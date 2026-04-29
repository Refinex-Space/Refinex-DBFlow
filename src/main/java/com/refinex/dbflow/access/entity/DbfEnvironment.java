package com.refinex.dbflow.access.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * DBFlow 项目环境元数据实体。
 *
 * @author refinex
 */
@Entity
@Table(name = "dbf_environments")
public class DbfEnvironment {

    /**
     * 环境主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 所属项目主键。
     */
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 环境标识。
     */
    @Column(name = "environment_key", nullable = false, length = 128)
    private String environmentKey;

    /**
     * 环境名称。
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * 环境状态。
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
     * 创建项目环境实体。
     *
     * @param projectId      所属项目主键
     * @param environmentKey 环境标识
     * @param name           环境名称
     * @return 项目环境实体
     */
    public static DbfEnvironment create(Long projectId, String environmentKey, String name) {
        DbfEnvironment environment = new DbfEnvironment();
        environment.projectId = projectId;
        environment.environmentKey = environmentKey;
        environment.name = name;
        environment.status = "ACTIVE";
        return environment;
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
     * 读取环境主键。
     *
     * @return 环境主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 读取所属项目主键。
     *
     * @return 所属项目主键
     */
    public Long getProjectId() {
        return projectId;
    }
}
