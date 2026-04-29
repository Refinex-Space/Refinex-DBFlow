package com.refinex.dbflow.access.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * DBFlow 项目元数据实体。
 *
 * @author refinex
 */
@Entity
@Table(name = "dbf_projects")
public class DbfProject {

    /**
     * 项目主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 唯一项目标识。
     */
    @Column(name = "project_key", nullable = false, length = 128)
    private String projectKey;

    /**
     * 项目名称。
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * 项目描述。
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 项目状态。
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
     * 创建项目实体。
     *
     * @param projectKey  唯一项目标识
     * @param name        项目名称
     * @param description 项目描述
     * @return 项目实体
     */
    public static DbfProject create(String projectKey, String name, String description) {
        DbfProject project = new DbfProject();
        project.projectKey = projectKey;
        project.name = name;
        project.description = description;
        project.status = "ACTIVE";
        return project;
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
     * 读取项目主键。
     *
     * @return 项目主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 读取项目标识。
     *
     * @return 唯一项目标识
     */
    public String getProjectKey() {
        return projectKey;
    }
}
