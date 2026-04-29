package com.refinex.dbflow.access.repository;

import com.refinex.dbflow.access.entity.DbfUserEnvGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * DBFlow 用户环境授权 repository。
 *
 * @author refinex
 */
public interface DbfUserEnvGrantRepository extends JpaRepository<DbfUserEnvGrant, Long> {

    /**
     * 查询指定用户的指定状态授权。
     *
     * @param userId 用户主键
     * @param status 授权状态
     * @return 授权列表
     */
    List<DbfUserEnvGrant> findByUserIdAndStatus(Long userId, String status);

    /**
     * 查询指定用户与环境的授权。
     *
     * @param userId        用户主键
     * @param environmentId 环境主键
     * @return 授权元数据
     */
    Optional<DbfUserEnvGrant> findByUserIdAndEnvironmentId(Long userId, Long environmentId);

    /**
     * 查询指定用户、环境和状态的授权。
     *
     * @param userId        用户主键
     * @param environmentId 环境主键
     * @param status        授权状态
     * @return 授权元数据
     */
    Optional<DbfUserEnvGrant> findByUserIdAndEnvironmentIdAndStatus(
            Long userId,
            Long environmentId,
            String status
    );

    /**
     * 判断指定用户与环境是否存在指定状态授权。
     *
     * @param userId        用户主键
     * @param environmentId 环境主键
     * @param status        授权状态
     * @return 存在时返回 true
     */
    boolean existsByUserIdAndEnvironmentIdAndStatus(Long userId, Long environmentId, String status);
}
