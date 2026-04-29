package com.refinex.dbflow.audit.repository;

import com.refinex.dbflow.audit.entity.DbfConfirmationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * DBFlow 确认挑战 repository。
 *
 * @author refinex
 */
public interface DbfConfirmationChallengeRepository extends JpaRepository<DbfConfirmationChallenge, Long> {

    /**
     * 按确认标识查询挑战。
     *
     * @param confirmationId 对外确认标识
     * @return 确认挑战实体
     */
    Optional<DbfConfirmationChallenge> findByConfirmationId(String confirmationId);

    /**
     * 查询用户指定状态的确认挑战。
     *
     * @param userId 用户主键
     * @param status 挑战状态
     * @return 确认挑战列表
     */
    List<DbfConfirmationChallenge> findByUserIdAndStatus(Long userId, String status);
}
