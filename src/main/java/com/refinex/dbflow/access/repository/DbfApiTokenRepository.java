package com.refinex.dbflow.access.repository;

import com.refinex.dbflow.access.entity.DbfApiToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * DBFlow API Token repository。
 *
 * @author refinex
 */
public interface DbfApiTokenRepository extends JpaRepository<DbfApiToken, Long> {

    /**
     * 查询用户指定状态的 Token。
     *
     * @param userId 用户主键
     * @param status Token 状态
     * @return API Token 元数据
     */
    Optional<DbfApiToken> findByUserIdAndStatus(Long userId, String status);

    /**
     * 按 Token hash 查询 Token 元数据。
     *
     * @param tokenHash Token hash
     * @return API Token 元数据
     */
    Optional<DbfApiToken> findByTokenHash(String tokenHash);
}
