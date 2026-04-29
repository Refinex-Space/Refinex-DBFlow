package com.refinex.dbflow.access.service;

import com.refinex.dbflow.access.entity.*;
import com.refinex.dbflow.access.repository.*;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 元数据访问控制基础服务。
 *
 * @author refinex
 */
@Service
public class AccessService {

    /**
     * 用户 repository。
     */
    private final DbfUserRepository userRepository;

    /**
     * API Token repository。
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
     * 创建访问控制服务。
     *
     * @param userRepository        用户 repository
     * @param apiTokenRepository    API Token repository
     * @param projectRepository     项目 repository
     * @param environmentRepository 环境 repository
     * @param grantRepository       授权 repository
     */
    public AccessService(
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
     * 创建用户。
     *
     * @param username     用户名
     * @param displayName  展示名称
     * @param passwordHash 密码 hash
     * @return 用户实体
     */
    @Transactional
    public DbfUser createUser(String username, String displayName, String passwordHash) {
        return userRepository.save(DbfUser.create(username, displayName, passwordHash));
    }

    /**
     * 禁用用户。
     *
     * @param userId 用户主键
     * @return 禁用后的用户实体
     */
    @Transactional
    public DbfUser disableUser(Long userId) {
        DbfUser user = userRepository.findById(userId)
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "用户不存在"));
        user.disable();
        return userRepository.save(user);
    }

    /**
     * 创建项目。
     *
     * @param projectKey  项目标识
     * @param name        项目名称
     * @param description 项目描述
     * @return 项目实体
     */
    @Transactional
    public DbfProject createProject(String projectKey, String name, String description) {
        return projectRepository.save(DbfProject.create(projectKey, name, description));
    }

    /**
     * 创建项目环境。
     *
     * @param projectId      项目主键
     * @param environmentKey 环境标识
     * @param name           环境名称
     * @return 环境实体
     */
    @Transactional
    public DbfEnvironment createEnvironment(Long projectId, String environmentKey, String name) {
        return environmentRepository.save(DbfEnvironment.create(projectId, environmentKey, name));
    }

    /**
     * 颁发 active Token 元数据。
     *
     * @param userId      用户主键
     * @param tokenHash   Token hash
     * @param tokenPrefix Token 前缀
     * @param expiresAt   过期时间
     * @return API Token 元数据
     */
    @Transactional
    public DbfApiToken issueActiveToken(Long userId, String tokenHash, String tokenPrefix, Instant expiresAt) {
        apiTokenRepository.findByUserIdAndStatus(userId, "ACTIVE").ifPresent(token -> {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "用户已有 active token");
        });
        return apiTokenRepository.save(DbfApiToken.active(userId, tokenHash, tokenPrefix, expiresAt));
    }

    /**
     * 吊销 Token。
     *
     * @param tokenId   Token 主键
     * @param revokedAt 吊销时间
     * @return 吊销后的 Token 元数据
     */
    @Transactional
    public DbfApiToken revokeToken(Long tokenId, Instant revokedAt) {
        DbfApiToken token = apiTokenRepository.findById(tokenId)
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "token 不存在"));
        token.revoke(revokedAt);
        return apiTokenRepository.save(token);
    }

    /**
     * 查询用户 active Token。
     *
     * @param userId 用户主键
     * @return active Token 元数据
     */
    @Transactional(readOnly = true)
    public Optional<DbfApiToken> findActiveToken(Long userId) {
        return apiTokenRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }

    /**
     * 创建用户环境授权。
     *
     * @param userId        用户主键
     * @param environmentId 环境主键
     * @param grantType     授权类型
     * @return 授权实体
     */
    @Transactional
    public DbfUserEnvGrant grantEnvironment(Long userId, Long environmentId, String grantType) {
        grantRepository.findByUserIdAndEnvironmentId(userId, environmentId).ifPresent(grant -> {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "用户环境授权已存在");
        });
        return grantRepository.save(DbfUserEnvGrant.active(userId, environmentId, grantType));
    }

    /**
     * 按项目和环境标识创建用户环境授权。
     *
     * @param userId         用户主键
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param grantType      授权类型
     * @return 授权实体
     */
    @Transactional
    public DbfUserEnvGrant grantEnvironment(
            Long userId,
            String projectKey,
            String environmentKey,
            String grantType
    ) {
        DbfEnvironment environment = resolveActiveEnvironment(projectKey, environmentKey);
        return grantEnvironment(userId, environment.getId(), grantType);
    }

    /**
     * 按项目和环境标识删除用户环境授权。
     *
     * @param userId         用户主键
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 删除到授权时返回 true
     */
    @Transactional
    public boolean deleteGrant(Long userId, String projectKey, String environmentKey) {
        DbfEnvironment environment = resolveActiveEnvironment(projectKey, environmentKey);
        Optional<DbfUserEnvGrant> grant = grantRepository.findByUserIdAndEnvironmentId(userId, environment.getId());
        grant.ifPresent(grantRepository::delete);
        return grant.isPresent();
    }

    /**
     * 按项目和环境标识查询用户环境授权。
     *
     * @param userId         用户主键
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 授权元数据
     */
    @Transactional(readOnly = true)
    public Optional<DbfUserEnvGrant> findGrant(Long userId, String projectKey, String environmentKey) {
        DbfEnvironment environment = resolveActiveEnvironment(projectKey, environmentKey);
        return grantRepository.findByUserIdAndEnvironmentId(userId, environment.getId());
    }

    /**
     * 查询用户 active 授权列表。
     *
     * @param userId 用户主键
     * @return active 授权列表
     */
    @Transactional(readOnly = true)
    public List<DbfUserEnvGrant> findActiveGrants(Long userId) {
        return grantRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }

    /**
     * 判断用户是否具备目标环境 active 授权。
     *
     * @param userId        用户主键
     * @param environmentId 环境主键
     * @return 具备 active 授权时返回 true
     */
    @Transactional(readOnly = true)
    public boolean hasActiveGrant(Long userId, Long environmentId) {
        return grantRepository.existsByUserIdAndEnvironmentIdAndStatus(userId, environmentId, "ACTIVE");
    }

    /**
     * 判断用户是否具备指定项目环境 active 授权。
     *
     * @param userId         用户主键
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 具备 active 授权时返回 true
     */
    @Transactional(readOnly = true)
    public boolean hasActiveGrant(Long userId, String projectKey, String environmentKey) {
        DbfEnvironment environment = resolveActiveEnvironment(projectKey, environmentKey);
        return hasActiveGrant(userId, environment.getId());
    }

    /**
     * 解析 active 项目环境。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return active 项目环境
     */
    private DbfEnvironment resolveActiveEnvironment(String projectKey, String environmentKey) {
        DbfProject project = projectRepository.findByProjectKeyAndStatus(projectKey, "ACTIVE")
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "项目不存在或不可用"));
        return environmentRepository.findByProjectIdAndEnvironmentKeyAndStatus(project.getId(), environmentKey, "ACTIVE")
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "环境不存在或不可用"));
    }
}
