package com.refinex.dbflow.audit.repository;

import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * DBFlow 审计事件 repository。
 *
 * @author refinex
 */
public interface DbfAuditEventRepository extends JpaRepository<DbfAuditEvent, Long>, JpaSpecificationExecutor<DbfAuditEvent> {

    /**
     * 查询指定用户最近的审计事件。
     *
     * @param userId 用户主键
     * @return 最近审计事件列表
     */
    List<DbfAuditEvent> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 查询最近 5 条审计事件，用于管理端总览页。
     *
     * @return 最近审计事件列表
     */
    List<DbfAuditEvent> findTop5ByOrderByCreatedAtDesc();
}
