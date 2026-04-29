package com.refinex.dbflow.access.repository;

import com.refinex.dbflow.access.entity.DbfEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * DBFlow 项目环境 repository。
 *
 * @author refinex
 */
public interface DbfEnvironmentRepository extends JpaRepository<DbfEnvironment, Long> {

    /**
     * 按项目和环境标识查询环境。
     *
     * @param projectId      项目主键
     * @param environmentKey 环境标识
     * @return 项目环境元数据
     */
    Optional<DbfEnvironment> findByProjectIdAndEnvironmentKey(Long projectId, String environmentKey);
}
