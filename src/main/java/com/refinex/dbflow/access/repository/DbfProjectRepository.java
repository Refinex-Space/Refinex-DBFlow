package com.refinex.dbflow.access.repository;

import com.refinex.dbflow.access.entity.DbfProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * DBFlow 项目 repository。
 *
 * @author refinex
 */
public interface DbfProjectRepository extends JpaRepository<DbfProject, Long> {

    /**
     * 按项目标识查询项目。
     *
     * @param projectKey 唯一项目标识
     * @return 项目元数据
     */
    Optional<DbfProject> findByProjectKey(String projectKey);
}
