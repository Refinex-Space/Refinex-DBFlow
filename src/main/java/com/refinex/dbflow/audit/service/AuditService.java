package com.refinex.dbflow.audit.service;

import com.refinex.dbflow.audit.entity.DbfAuditEvent;
import com.refinex.dbflow.audit.repository.DbfAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 操作审计基础服务。
 *
 * @author refinex
 */
@Service
public class AuditService {

    /**
     * 审计事件 repository。
     */
    private final DbfAuditEventRepository auditEventRepository;

    /**
     * 创建审计服务。
     *
     * @param auditEventRepository 审计事件 repository
     */
    public AuditService(DbfAuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * 保存审计事件。
     *
     * @param event 审计事件实体
     * @return 已保存审计事件
     */
    @Transactional
    public DbfAuditEvent saveAuditEvent(DbfAuditEvent event) {
        return auditEventRepository.save(event);
    }

    /**
     * 查询指定用户最近审计事件。
     *
     * @param userId 用户主键
     * @return 最近审计事件列表
     */
    @Transactional(readOnly = true)
    public List<DbfAuditEvent> findRecentByUser(Long userId) {
        return auditEventRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }
}
