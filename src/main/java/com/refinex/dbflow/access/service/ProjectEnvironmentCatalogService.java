package com.refinex.dbflow.access.service;

import com.refinex.dbflow.access.dto.ConfiguredEnvironmentView;
import com.refinex.dbflow.access.entity.DbfEnvironment;
import com.refinex.dbflow.access.entity.DbfProject;
import com.refinex.dbflow.access.repository.DbfEnvironmentRepository;
import com.refinex.dbflow.access.repository.DbfProjectRepository;
import com.refinex.dbflow.config.properties.DbflowProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 项目环境配置目录服务，负责打通 YAML 配置与元数据库展示模型。
 *
 * @author refinex
 */
@Service
public class ProjectEnvironmentCatalogService {

    /**
     * DBFlow 配置属性。
     */
    private final DbflowProperties dbflowProperties;

    /**
     * 项目 repository。
     */
    private final DbfProjectRepository projectRepository;

    /**
     * 环境 repository。
     */
    private final DbfEnvironmentRepository environmentRepository;

    /**
     * 创建项目环境配置目录服务。
     *
     * @param dbflowProperties      DBFlow 配置属性
     * @param projectRepository     项目 repository
     * @param environmentRepository 环境 repository
     */
    public ProjectEnvironmentCatalogService(
            DbflowProperties dbflowProperties,
            DbfProjectRepository projectRepository,
            DbfEnvironmentRepository environmentRepository
    ) {
        this.dbflowProperties = dbflowProperties;
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
    }

    /**
     * 将 YAML 配置中的项目环境同步为元数据库展示模型。
     *
     * @return 同步后的脱敏项目环境视图列表
     */
    @Transactional
    public List<ConfiguredEnvironmentView> syncConfiguredProjectEnvironments() {
        for (DbflowProperties.Project configuredProject : dbflowProperties.getProjects()) {
            DbfProject project = projectRepository.findByProjectKey(configuredProject.getKey())
                    .orElseGet(() -> projectRepository.save(DbfProject.create(
                            configuredProject.getKey(),
                            displayName(configuredProject.getName(), configuredProject.getKey()),
                            null
                    )));
            for (DbflowProperties.Environment configuredEnvironment : configuredProject.getEnvironments()) {
                environmentRepository.findByProjectIdAndEnvironmentKey(
                        project.getId(),
                        configuredEnvironment.getKey()
                ).orElseGet(() -> environmentRepository.save(DbfEnvironment.create(
                        project.getId(),
                        configuredEnvironment.getKey(),
                        displayName(configuredEnvironment.getName(), configuredEnvironment.getKey())
                )));
            }
        }
        return listConfiguredEnvironments();
    }

    /**
     * 查询脱敏后的配置项目环境视图。
     *
     * @return 脱敏项目环境视图列表
     */
    @Transactional(readOnly = true)
    public List<ConfiguredEnvironmentView> listConfiguredEnvironments() {
        List<ConfiguredEnvironmentView> views = new ArrayList<>();
        for (DbflowProperties.Project configuredProject : dbflowProperties.getProjects()) {
            Optional<DbfProject> project = projectRepository.findByProjectKey(configuredProject.getKey());
            for (DbflowProperties.Environment configuredEnvironment : configuredProject.getEnvironments()) {
                Optional<DbfEnvironment> environment = project.flatMap(existingProject ->
                        environmentRepository.findByProjectIdAndEnvironmentKey(
                                existingProject.getId(),
                                configuredEnvironment.getKey()
                        ));
                views.add(toView(configuredProject, configuredEnvironment, project, environment));
            }
        }
        return views;
    }

    /**
     * 创建项目环境展示视图。
     *
     * @param configuredProject     配置项目
     * @param configuredEnvironment 配置环境
     * @param project               元数据库项目
     * @param environment           元数据库环境
     * @return 项目环境展示视图
     */
    private ConfiguredEnvironmentView toView(
            DbflowProperties.Project configuredProject,
            DbflowProperties.Environment configuredEnvironment,
            Optional<DbfProject> project,
            Optional<DbfEnvironment> environment
    ) {
        return new ConfiguredEnvironmentView(
                project.map(DbfProject::getId).orElse(null),
                environment.map(DbfEnvironment::getId).orElse(null),
                configuredProject.getKey(),
                displayName(configuredProject.getName(), configuredProject.getKey()),
                configuredEnvironment.getKey(),
                displayName(configuredEnvironment.getName(), configuredEnvironment.getKey()),
                configuredEnvironment.getJdbcUrl(),
                effectiveDriverClassName(configuredEnvironment),
                effectiveUsername(configuredEnvironment),
                project.isPresent() && environment.isPresent()
        );
    }

    /**
     * 解析有效驱动类名。
     *
     * @param environment 配置环境
     * @return 有效驱动类名
     */
    private String effectiveDriverClassName(DbflowProperties.Environment environment) {
        if (!isBlank(environment.getDriverClassName())) {
            return environment.getDriverClassName();
        }
        return dbflowProperties.getDatasourceDefaults().getDriverClassName();
    }

    /**
     * 解析有效数据库用户名。
     *
     * @param environment 配置环境
     * @return 有效数据库用户名
     */
    private String effectiveUsername(DbflowProperties.Environment environment) {
        if (!isBlank(environment.getUsername())) {
            return environment.getUsername();
        }
        return dbflowProperties.getDatasourceDefaults().getUsername();
    }

    /**
     * 解析展示名称。
     *
     * @param value    配置展示名称
     * @param fallback 兜底名称
     * @return 展示名称
     */
    private String displayName(String value, String fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        return value;
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 字符串
     * @return 为空白时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
