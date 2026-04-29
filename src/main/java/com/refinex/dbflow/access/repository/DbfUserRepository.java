package com.refinex.dbflow.access.repository;

import com.refinex.dbflow.access.entity.DbfUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * DBFlow 用户元数据 repository。
 *
 * @author refinex
 */
public interface DbfUserRepository extends JpaRepository<DbfUser, Long> {

    /**
     * 按用户名查询用户。
     *
     * @param username 唯一用户名
     * @return 用户元数据
     */
    Optional<DbfUser> findByUsername(String username);
}
