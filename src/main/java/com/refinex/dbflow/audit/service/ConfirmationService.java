package com.refinex.dbflow.audit.service;

import com.refinex.dbflow.audit.entity.DbfConfirmationChallenge;
import com.refinex.dbflow.audit.repository.DbfConfirmationChallengeRepository;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * SQL 二次确认基础服务。
 *
 * @author refinex
 */
@Service
public class ConfirmationService {

    /**
     * 确认挑战 repository。
     */
    private final DbfConfirmationChallengeRepository confirmationChallengeRepository;

    /**
     * 创建确认服务。
     *
     * @param confirmationChallengeRepository 确认挑战 repository
     */
    public ConfirmationService(DbfConfirmationChallengeRepository confirmationChallengeRepository) {
        this.confirmationChallengeRepository = confirmationChallengeRepository;
    }

    /**
     * 创建待确认挑战。
     *
     * @param userId         用户主键
     * @param tokenId        MCP Token 主键
     * @param environmentId  环境主键
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param confirmationId 对外确认标识
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param riskLevel      风险级别
     * @param expiresAt      过期时间
     * @return 确认挑战实体
     */
    @Transactional
    public DbfConfirmationChallenge createPending(
            Long userId,
            Long tokenId,
            Long environmentId,
            String projectKey,
            String environmentKey,
            String confirmationId,
            String sqlHash,
            String sqlText,
            String riskLevel,
            Instant expiresAt
    ) {
        return confirmationChallengeRepository.save(DbfConfirmationChallenge.pending(
                userId,
                tokenId,
                environmentId,
                projectKey,
                environmentKey,
                confirmationId,
                sqlHash,
                sqlText,
                riskLevel,
                expiresAt
        ));
    }

    /**
     * 确认待处理挑战。
     *
     * @param confirmationId 对外确认标识
     * @param confirmedAt    确认时间
     * @return 已确认挑战实体
     */
    @Transactional
    public DbfConfirmationChallenge confirm(String confirmationId, Instant confirmedAt) {
        DbfConfirmationChallenge challenge = confirmationChallengeRepository.findByConfirmationId(confirmationId)
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "确认挑战不存在"));
        if (!"PENDING".equals(challenge.getStatus())) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "确认挑战不是待处理状态");
        }
        challenge.confirm(confirmedAt);
        return confirmationChallengeRepository.save(challenge);
    }

    /**
     * 查询用户待确认挑战。
     *
     * @param userId 用户主键
     * @return 待确认挑战列表
     */
    @Transactional(readOnly = true)
    public List<DbfConfirmationChallenge> findPendingByUser(Long userId) {
        return confirmationChallengeRepository.findByUserIdAndStatus(userId, "PENDING");
    }
}
