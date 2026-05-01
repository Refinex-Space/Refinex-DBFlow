package com.refinex.dbflow.sqlpolicy.service;

import com.refinex.dbflow.access.entity.DbfEnvironment;
import com.refinex.dbflow.access.entity.DbfProject;
import com.refinex.dbflow.access.repository.DbfEnvironmentRepository;
import com.refinex.dbflow.access.repository.DbfProjectRepository;
import com.refinex.dbflow.audit.dto.AuditEventWriteRequest;
import com.refinex.dbflow.audit.entity.DbfConfirmationChallenge;
import com.refinex.dbflow.audit.repository.DbfConfirmationChallengeRepository;
import com.refinex.dbflow.audit.service.AuditEventWriter;
import com.refinex.dbflow.audit.service.ConfirmationService;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import com.refinex.dbflow.sqlpolicy.dto.SqlClassification;
import com.refinex.dbflow.sqlpolicy.dto.TruncateConfirmationConfirmRequest;
import com.refinex.dbflow.sqlpolicy.dto.TruncateConfirmationDecision;
import com.refinex.dbflow.sqlpolicy.dto.TruncateConfirmationRequest;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * TRUNCATE 服务端二次确认挑战服务。
 *
 * @author refinex
 */
@Service
public class TruncateConfirmationService {

    /**
     * TRUNCATE 确认挑战默认有效期。
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    /**
     * SQL 分类服务。
     */
    private final SqlClassifier sqlClassifier;

    /**
     * 项目 repository。
     */
    private final DbfProjectRepository projectRepository;

    /**
     * 环境 repository。
     */
    private final DbfEnvironmentRepository environmentRepository;

    /**
     * 确认挑战基础服务。
     */
    private final ConfirmationService confirmationService;

    /**
     * 确认挑战 repository。
     */
    private final DbfConfirmationChallengeRepository confirmationChallengeRepository;

    /**
     * 审计服务。
     */
    private final AuditEventWriter auditEventWriter;

    /**
     * 创建 TRUNCATE 确认服务。
     *
     * @param sqlClassifier                   SQL 分类服务
     * @param projectRepository               项目 repository
     * @param environmentRepository           环境 repository
     * @param confirmationService             确认挑战基础服务
     * @param confirmationChallengeRepository 确认挑战 repository
     * @param auditEventWriter                统一审计事件写入器
     */
    public TruncateConfirmationService(
            SqlClassifier sqlClassifier,
            DbfProjectRepository projectRepository,
            DbfEnvironmentRepository environmentRepository,
            ConfirmationService confirmationService,
            DbfConfirmationChallengeRepository confirmationChallengeRepository,
            AuditEventWriter auditEventWriter
    ) {
        this.sqlClassifier = sqlClassifier;
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
        this.confirmationService = confirmationService;
        this.confirmationChallengeRepository = confirmationChallengeRepository;
        this.auditEventWriter = auditEventWriter;
    }

    /**
     * 为 TRUNCATE SQL 创建服务端确认挑战，不执行 SQL。
     *
     * @param request 确认挑战创建请求
     * @return 确认挑战决策结果
     */
    @Transactional
    public TruncateConfirmationDecision createChallenge(TruncateConfirmationRequest request) {
        Objects.requireNonNull(request, "request");
        auditEventWriter.requestReceived(auditRequest(request, "TRUNCATE 确认挑战创建请求已接收", null, request.sql(),
                null, null, null));
        SqlClassification classification = classifyTruncate(request.sql());
        DbfEnvironment environment = resolveEnvironment(request.projectKey(), request.environmentKey());
        String sqlText = normalizeSql(request.sql());
        String sqlHash = sqlHash(sqlText);
        String confirmationId = "cnf_" + UUID.randomUUID().toString().replace("-", "");
        Instant requestedAt = defaultInstant(request.requestedAt());
        Instant expiresAt = requestedAt.plus(DEFAULT_TTL);

        DbfConfirmationChallenge challenge = confirmationService.createPending(
                request.userId(),
                request.tokenId(),
                environment.getId(),
                request.projectKey(),
                request.environmentKey(),
                confirmationId,
                sqlHash,
                sqlText,
                classification.riskLevel().name(),
                expiresAt
        );
        audit(request, "REQUIRES_CONFIRMATION", sqlHash, sqlText, challenge.getConfirmationId(), null, null);
        return new TruncateConfirmationDecision(
                true,
                false,
                challenge.getStatus(),
                challenge.getConfirmationId(),
                sqlHash,
                classification.riskLevel(),
                expiresAt
        );
    }

    /**
     * 校验并消费 TRUNCATE 确认挑战。
     *
     * @param request 确认请求
     * @return 确认挑战决策结果
     */
    @Transactional(noRollbackFor = DbflowException.class)
    public TruncateConfirmationDecision confirm(TruncateConfirmationConfirmRequest request) {
        Objects.requireNonNull(request, "request");
        auditEventWriter.requestReceived(auditRequest(request, "TRUNCATE 确认消费请求已接收", null, request.sql(),
                request.confirmationId(), null, null));
        String sqlText = normalizeSql(request.sql());
        String sqlHash = sqlHash(sqlText);
        DbfConfirmationChallenge challenge = confirmationChallengeRepository
                .findByConfirmationId(request.confirmationId())
                .orElseThrow(() -> reject(request, "DENIED", sqlHash, sqlText, request.confirmationId(),
                        "CONFIRMATION_NOT_FOUND", "确认挑战不存在"));

        if (!"PENDING".equals(challenge.getStatus())) {
            throw reject(request, "DENIED", sqlHash, sqlText, challenge.getConfirmationId(),
                    "CONFIRMATION_NOT_PENDING", "确认挑战不是待处理状态");
        }

        Instant confirmedAt = defaultInstant(request.confirmedAt());
        if (!confirmedAt.isBefore(challenge.getExpiresAt())) {
            challenge.expire();
            confirmationChallengeRepository.save(challenge);
            throw reject(request, "CONFIRMATION_EXPIRED", sqlHash, sqlText, challenge.getConfirmationId(),
                    "CONFIRMATION_EXPIRED", "确认挑战已过期");
        }

        if (isMismatch(request, challenge, sqlHash)) {
            throw reject(request, "DENIED", sqlHash, sqlText, challenge.getConfirmationId(),
                    "CONFIRMATION_MISMATCH", "确认挑战不匹配");
        }

        challenge.confirm(confirmedAt);
        DbfConfirmationChallenge saved = confirmationChallengeRepository.save(challenge);
        audit(request, "CONFIRMATION_CONFIRMED", sqlHash, sqlText, saved.getConfirmationId(), null, null);
        return new TruncateConfirmationDecision(
                false,
                true,
                saved.getStatus(),
                saved.getConfirmationId(),
                saved.getSqlHash(),
                SqlRiskLevel.valueOf(saved.getRiskLevel()),
                saved.getExpiresAt()
        );
    }

    /**
     * 判断 SQL 是否为 TRUNCATE。
     *
     * @param sql SQL 原文
     * @return 是否 TRUNCATE
     */
    public boolean isTruncate(String sql) {
        return sqlClassifier.classify(sql).operation() == SqlOperation.TRUNCATE;
    }

    /**
     * 解析并校验 TRUNCATE 分类。
     *
     * @param sql SQL 原文
     * @return SQL 分类结果
     */
    private SqlClassification classifyTruncate(String sql) {
        SqlClassification classification = sqlClassifier.classify(sql);
        if (classification.operation() != SqlOperation.TRUNCATE || classification.rejectedByDefault()) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "仅已成功解析的 TRUNCATE SQL 需要二次确认");
        }
        return classification;
    }

    /**
     * 解析活跃项目环境。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 环境实体
     */
    private DbfEnvironment resolveEnvironment(String projectKey, String environmentKey) {
        DbfProject project = projectRepository.findByProjectKeyAndStatus(projectKey, "ACTIVE")
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "项目不存在或已禁用"));
        return environmentRepository
                .findByProjectIdAndEnvironmentKeyAndStatus(project.getId(), environmentKey, "ACTIVE")
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "环境不存在或已禁用"));
    }

    /**
     * 判断确认请求是否与挑战绑定信息不一致。
     *
     * @param request   确认请求
     * @param challenge 确认挑战
     * @param sqlHash   请求 SQL hash
     * @return 是否不匹配
     */
    private boolean isMismatch(
            TruncateConfirmationConfirmRequest request,
            DbfConfirmationChallenge challenge,
            String sqlHash
    ) {
        return !Objects.equals(request.userId(), challenge.getUserId())
                || !Objects.equals(request.tokenId(), challenge.getTokenId())
                || !Objects.equals(request.projectKey(), challenge.getProjectKey())
                || !Objects.equals(request.environmentKey(), challenge.getEnvironmentKey())
                || !Objects.equals(sqlHash, challenge.getSqlHash());
    }

    /**
     * 记录创建请求审计事件。
     *
     * @param request        创建请求
     * @param status         审计状态
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param confirmationId 确认挑战标识
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     */
    private void audit(
            TruncateConfirmationRequest request,
            String status,
            String sqlHash,
            String sqlText,
            String confirmationId,
            String errorCode,
            String errorMessage
    ) {
        writeConfirmationAudit(auditRequest(request, "TRUNCATE 确认挑战状态变化", sqlHash, sqlText, confirmationId,
                errorCode, errorMessage), status);
    }

    /**
     * 记录确认请求审计事件。
     *
     * @param request        确认请求
     * @param status         审计状态
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param confirmationId 确认挑战标识
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     */
    private void audit(
            TruncateConfirmationConfirmRequest request,
            String status,
            String sqlHash,
            String sqlText,
            String confirmationId,
            String errorCode,
            String errorMessage
    ) {
        writeConfirmationAudit(auditRequest(request, "TRUNCATE 确认挑战状态变化", sqlHash, sqlText, confirmationId,
                errorCode, errorMessage), status);
    }

    /**
     * 按确认状态写入审计事件。
     *
     * @param request 审计写入请求
     * @param status  确认状态
     */
    private void writeConfirmationAudit(AuditEventWriteRequest request, String status) {
        if ("REQUIRES_CONFIRMATION".equals(status)) {
            auditEventWriter.requiresConfirmation(request);
        } else if ("CONFIRMATION_CONFIRMED".equals(status)) {
            auditEventWriter.confirmationConfirmed(request);
        } else if ("CONFIRMATION_EXPIRED".equals(status)) {
            auditEventWriter.confirmationExpired(request);
        } else if ("FAILED".equals(status)) {
            auditEventWriter.failed(request);
        } else {
            auditEventWriter.policyDenied(request);
        }
    }

    /**
     * 创建确认挑战审计写入请求。
     *
     * @param request        确认挑战创建请求
     * @param summary        结果摘要
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param confirmationId 确认挑战标识
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     * @return 审计写入请求
     */
    private AuditEventWriteRequest auditRequest(
            TruncateConfirmationRequest request,
            String summary,
            String sqlHash,
            String sqlText,
            String confirmationId,
            String errorCode,
            String errorMessage
    ) {
        return new AuditEventWriteRequest(
                request.requestId(),
                request.userId(),
                request.tokenId(),
                request.tokenPrefix(),
                request.auditContext(),
                request.projectKey(),
                request.environmentKey(),
                "TRUNCATE",
                "CRITICAL",
                sqlText,
                sqlHash,
                summary,
                0L,
                errorCode,
                errorMessage,
                confirmationId
        );
    }

    /**
     * 创建确认消费审计写入请求。
     *
     * @param request        确认消费请求
     * @param summary        结果摘要
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param confirmationId 确认挑战标识
     * @param errorCode      错误码
     * @param errorMessage   错误摘要
     * @return 审计写入请求
     */
    private AuditEventWriteRequest auditRequest(
            TruncateConfirmationConfirmRequest request,
            String summary,
            String sqlHash,
            String sqlText,
            String confirmationId,
            String errorCode,
            String errorMessage
    ) {
        return new AuditEventWriteRequest(
                request.requestId(),
                request.userId(),
                request.tokenId(),
                request.tokenPrefix(),
                request.auditContext(),
                request.projectKey(),
                request.environmentKey(),
                "TRUNCATE",
                "CRITICAL",
                sqlText,
                sqlHash,
                summary,
                0L,
                errorCode,
                errorMessage,
                confirmationId
        );
    }

    /**
     * 记录拒绝审计并创建异常。
     *
     * @param request        确认请求
     * @param status         审计状态
     * @param sqlHash        SQL hash
     * @param sqlText        SQL 原文
     * @param confirmationId 确认挑战标识
     * @param errorCode      错误码
     * @param message        异常消息
     * @return DBFlow 异常
     */
    private DbflowException reject(
            TruncateConfirmationConfirmRequest request,
            String status,
            String sqlHash,
            String sqlText,
            String confirmationId,
            String errorCode,
            String message
    ) {
        audit(request, status, sqlHash, sqlText, confirmationId, errorCode, message);
        return new DbflowException(ErrorCode.INVALID_REQUEST, message);
    }

    /**
     * 标准化 SQL 文本，避免首尾空白影响 hash。
     *
     * @param sql SQL 原文
     * @return 标准化 SQL
     */
    private String normalizeSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "SQL 不能为空");
        }
        return sql.strip();
    }

    /**
     * 生成 SQL SHA-256 hash。
     *
     * @param sql SQL 原文
     * @return Base64Url 编码 hash
     */
    private String sqlHash(String sql) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(sql.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new DbflowException(ErrorCode.INTERNAL_ERROR, "SQL hash 算法不可用", exception);
        }
    }

    /**
     * 为空时返回当前时间。
     *
     * @param instant 输入时间
     * @return 非空时间
     */
    private Instant defaultInstant(Instant instant) {
        return instant == null ? Instant.now() : instant;
    }
}
