package com.refinex.dbflow.access.service;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.entity.DbfEnvironment;
import com.refinex.dbflow.access.entity.DbfProject;
import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 项目环境访问判断服务，供未来 MCP SQL 执行前强制调用。
 *
 * @author refinex
 */
@Service
public class AccessDecisionService {

    /**
     * 用户 repository。
     */
    private final DbfUserRepository userRepository;

    /**
     * Token repository。
     */
    private final DbfApiTokenRepository apiTokenRepository;

    /**
     * 项目 repository。
     */
    private final DbfProjectRepository projectRepository;

    /**
     * 环境 repository。
     */
    private final DbfEnvironmentRepository environmentRepository;

    /**
     * 授权 repository。
     */
    private final DbfUserEnvGrantRepository grantRepository;

    /**
     * 创建项目环境访问判断服务。
     *
     * @param userRepository        用户 repository
     * @param apiTokenRepository    Token repository
     * @param projectRepository     项目 repository
     * @param environmentRepository 环境 repository
     * @param grantRepository       授权 repository
     */
    public AccessDecisionService(
            DbfUserRepository userRepository,
            DbfApiTokenRepository apiTokenRepository,
            DbfProjectRepository projectRepository,
            DbfEnvironmentRepository environmentRepository,
            DbfUserEnvGrantRepository grantRepository
    ) {
        this.userRepository = userRepository;
        this.apiTokenRepository = apiTokenRepository;
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
        this.grantRepository = grantRepository;
    }

    /**
     * 判断用户 Token 是否可以访问指定项目环境。
     *
     * @param request 访问判断请求
     * @return 访问判断结果
     */
    @Transactional(readOnly = true)
    public AccessDecision decide(AccessDecisionRequest request) {
        return decide(request, Instant.now());
    }

    /**
     * 判断用户 Token 是否可以访问指定项目环境。
     *
     * @param request 访问判断请求
     * @param now     当前时间
     * @return 访问判断结果
     */
    @Transactional(readOnly = true)
    public AccessDecision decide(AccessDecisionRequest request, Instant now) {
        DbfUser user = userRepository.findById(request.userId()).orElse(null);
        if (user == null) {
            return AccessDecision.deny(AccessDecisionReason.USER_NOT_FOUND, "用户不存在");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            return AccessDecision.deny(AccessDecisionReason.USER_DISABLED, "用户已禁用");
        }

        DbfApiToken token = apiTokenRepository.findById(request.tokenId()).orElse(null);
        if (token == null) {
            return AccessDecision.deny(AccessDecisionReason.TOKEN_NOT_FOUND, "MCP Token 不存在");
        }
        if (!user.getId().equals(token.getUserId())) {
            return AccessDecision.deny(AccessDecisionReason.TOKEN_USER_MISMATCH, "MCP Token 不属于当前用户");
        }
        if (!"ACTIVE".equals(token.getStatus()) || token.getRevokedAt() != null) {
            return AccessDecision.deny(AccessDecisionReason.TOKEN_DISABLED, "MCP Token 已禁用");
        }
        if (token.getExpiresAt() != null && !token.getExpiresAt().isAfter(now)) {
            return AccessDecision.deny(AccessDecisionReason.TOKEN_EXPIRED, "MCP Token 已过期");
        }

        DbfProject project = projectRepository.findByProjectKeyAndStatus(request.projectKey(), "ACTIVE").orElse(null);
        if (project == null) {
            return AccessDecision.deny(AccessDecisionReason.PROJECT_NOT_FOUND, "项目不存在或不可用");
        }
        DbfEnvironment environment = environmentRepository.findByProjectIdAndEnvironmentKeyAndStatus(
                project.getId(),
                request.environmentKey(),
                "ACTIVE"
        ).orElse(null);
        if (environment == null) {
            return AccessDecision.deny(AccessDecisionReason.ENVIRONMENT_NOT_FOUND, "环境不存在或不可用");
        }
        if (!grantRepository.existsByUserIdAndEnvironmentIdAndStatus(user.getId(), environment.getId(), "ACTIVE")) {
            return AccessDecision.deny(AccessDecisionReason.GRANT_NOT_FOUND, "用户未获得项目环境授权");
        }
        return AccessDecision.allow();
    }
}
