package com.refinex.dbflow.admin.service;

import com.refinex.dbflow.access.dto.ConfiguredEnvironmentView;
import com.refinex.dbflow.access.entity.*;
import com.refinex.dbflow.access.repository.*;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.admin.command.CreateUserCommand;
import com.refinex.dbflow.admin.command.GrantEnvironmentCommand;
import com.refinex.dbflow.admin.command.IssueTokenCommand;
import com.refinex.dbflow.admin.command.UpdateProjectGrantsCommand;
import com.refinex.dbflow.admin.view.*;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import com.refinex.dbflow.security.token.McpTokenIssueResult;
import com.refinex.dbflow.security.token.McpTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端用户、Token 和项目环境授权服务。
 *
 * @author refinex
 */
@Service
public class AdminAccessManagementService {

    /**
     * 默认 Token 有效天数。
     */
    private static final int DEFAULT_EXPIRES_IN_DAYS = 30;

    /**
     * 用户 repository。
     */
    private final DbfUserRepository userRepository;

    /**
     * Token repository。
     */
    private final DbfApiTokenRepository tokenRepository;

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
     * 项目环境配置目录服务。
     */
    private final ProjectEnvironmentCatalogService catalogService;

    /**
     * MCP Token 生命周期服务。
     */
    private final McpTokenService tokenService;

    /**
     * 管理端密码编码器。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建管理端访问管理服务。
     *
     * @param userRepository        用户 repository
     * @param tokenRepository       Token repository
     * @param projectRepository     项目 repository
     * @param environmentRepository 环境 repository
     * @param grantRepository       授权 repository
     * @param catalogService        项目环境配置目录服务
     * @param tokenService          MCP Token 生命周期服务
     * @param passwordEncoder       管理端密码编码器
     */
    public AdminAccessManagementService(
            DbfUserRepository userRepository,
            DbfApiTokenRepository tokenRepository,
            DbfProjectRepository projectRepository,
            DbfEnvironmentRepository environmentRepository,
            DbfUserEnvGrantRepository grantRepository,
            ProjectEnvironmentCatalogService catalogService,
            McpTokenService tokenService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
        this.grantRepository = grantRepository;
        this.catalogService = catalogService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询用户列表。
     *
     * @param filter 用户筛选条件
     * @return 用户表格行
     */
    @Transactional(readOnly = true)
    public List<UserRow> listUsers(UserFilter filter) {
        List<DbfApiToken> tokens = tokenRepository.findAll();
        List<DbfUserEnvGrant> grants = grantRepository.findAll();
        return userRepository.findAll().stream()
                .filter(user -> matches(filter.username(), user.getUsername()))
                .filter(user -> matchesStatus(filter.status(), user.getStatus()))
                .sorted(Comparator.comparing(DbfUser::getId))
                .map(user -> toUserRow(user, grants, tokens))
                .toList();
    }

    /**
     * 查询可用于表单选择的 active 用户。
     *
     * @return active 用户选项
     */
    @Transactional(readOnly = true)
    public List<UserOption> listActiveUserOptions() {
        return userRepository.findAll().stream()
                .filter(user -> "ACTIVE".equals(user.getStatus()))
                .sorted(Comparator.comparing(DbfUser::getUsername))
                .map(user -> new UserOption(user.getId(), user.getUsername(), user.getDisplayName()))
                .toList();
    }

    /**
     * 创建用户。
     *
     * @param command 创建用户命令
     * @return 创建后的用户表格行
     */
    @Transactional
    public UserRow createUser(CreateUserCommand command) {
        String username = required(command.username(), "用户名不能为空");
        String displayName = required(command.displayName(), "显示名不能为空");
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "用户名已存在");
        }
        String passwordHash = null;
        if (!isBlank(command.password())) {
            passwordHash = passwordEncoder.encode(command.password());
        }
        DbfUser savedUser = userRepository.save(DbfUser.create(username, displayName, passwordHash));
        return toUserRow(savedUser, List.of(), List.of());
    }

    /**
     * 禁用用户。
     *
     * @param userId 用户主键
     */
    @Transactional
    public void disableUser(Long userId) {
        DbfUser user = requireUser(userId);
        user.disable();
        userRepository.save(user);
    }

    /**
     * 启用用户。
     *
     * @param userId 用户主键
     */
    @Transactional
    public void enableUser(Long userId) {
        DbfUser user = requireUser(userId);
        user.enable();
        userRepository.save(user);
    }

    /**
     * 重置用户管理端密码。
     *
     * @param userId      用户主键
     * @param newPassword 新明文密码
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "新密码不能为空");
        }
        DbfUser user = requireUser(userId);
        user.resetPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 查询 Token 列表。
     *
     * @param filter Token 筛选条件
     * @return Token 表格行
     */
    @Transactional(readOnly = true)
    public List<TokenRow> listTokens(TokenFilter filter) {
        Map<Long, DbfUser> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(DbfUser::getId, Function.identity()));
        return tokenRepository.findAll().stream()
                .filter(token -> {
                    // 默认视图不展示已吊销 Token；显式筛选 REVOKED 或 "全部" 时才显示
                    if (isBlank(filter.status())) {
                        return !"REVOKED".equals(token.getStatus());
                    }
                    return matchesStatus(filter.status(), token.getStatus());
                })
                .filter(token -> matchesUser(filter.username(), userMap.get(token.getUserId())))
                .sorted(Comparator.comparing(DbfApiToken::getId).reversed())
                .map(token -> toTokenRow(token, userMap.get(token.getUserId())))
                .toList();
    }

    /**
     * 颁发 Token。
     *
     * @param command 颁发命令
     * @return 一次性 Token 展示视图
     */
    @Transactional
    public IssuedTokenView issueToken(IssueTokenCommand command) {
        DbfUser user = requireActiveUser(command.userId());
        McpTokenIssueResult result = tokenService.issueToken(user.getId(), expiresAt(command.expiresInDays()));
        return new IssuedTokenView(
                result.tokenId(),
                user.getId(),
                user.getUsername(),
                result.plaintextToken(),
                result.tokenPrefix(),
                result.expiresAt());
    }

    /**
     * 吊销 Token。
     *
     * @param tokenId Token 主键
     */
    @Transactional
    public void revokeToken(Long tokenId) {
        DbfApiToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "Token 不存在"));
        if (!"ACTIVE".equals(token.getStatus())) {
            return;
        }
        tokenService.revokeActiveToken(token.getUserId(), Instant.now());
    }

    /**
     * 重新颁发 Token，存在 active Token 时先吊销旧 Token。
     *
     * @param command 颁发命令
     * @return 一次性 Token 展示视图
     */
    @Transactional
    public IssuedTokenView reissueToken(IssueTokenCommand command) {
        DbfUser user = requireActiveUser(command.userId());
        tokenRepository.findByUserIdAndStatus(user.getId(), "ACTIVE")
                .ifPresent(token -> tokenService.revokeActiveToken(user.getId(), Instant.now()));
        return issueToken(command);
    }

    /**
     * 查询可授权环境选项。
     *
     * @return 环境选项
     */
    @Transactional
    public List<GrantEnvironmentOption> listEnvironmentOptions() {
        catalogService.syncConfiguredProjectEnvironments();
        return catalogService.listConfiguredEnvironments().stream()
                .filter(ConfiguredEnvironmentView::metadataPresent)
                .map(view -> new GrantEnvironmentOption(
                        view.projectKey(),
                        view.projectName(),
                        view.environmentKey(),
                        view.environmentName()))
                .toList();
    }

    /**
     * 以「用户 × 项目」为维度查询授权分组列表（同一用户同一项目的多个环境合并为一行）。
     *
     * @param filter 授权筛选条件
     * @return 授权分组行列表
     */
    @Transactional
    public List<GrantGroupRow> listGrantGroups(GrantFilter filter) {
        catalogService.syncConfiguredProjectEnvironments();
        Map<Long, DbfUser> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(DbfUser::getId, Function.identity()));
        Map<Long, DbfEnvironment> environmentMap = environmentRepository.findAll().stream()
                .collect(Collectors.toMap(DbfEnvironment::getId, Function.identity()));
        Map<Long, DbfProject> projectMap = projectRepository.findAll().stream()
                .collect(Collectors.toMap(DbfProject::getId, Function.identity()));

        // 先按基础筛选条件过滤单条授权，再组装分组视图
        return grantRepository.findAll().stream()
                .map(grant -> toGrantRow(grant, userMap, environmentMap, projectMap))
                .filter(row -> matches(filter.username(), row.username()))
                .filter(row -> matches(filter.projectKey(), row.projectKey()))
                .filter(row -> matches(filter.environmentKey(), row.environmentKey()))
                .filter(row -> matchesStatus(filter.status(), row.status()))
                .collect(Collectors.groupingBy(
                        row -> row.userId() + ":" + row.projectKey(),
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values().stream()
                .map(rows -> {
                    GrantRow first = rows.get(0);
                    List<GrantEnvEntry> envEntries = rows.stream()
                            .sorted(Comparator.comparing(GrantRow::id))
                            .map(r -> new GrantEnvEntry(r.id(), r.environmentKey(), r.grantType(), r.status()))
                            .toList();
                    return new GrantGroupRow(first.userId(), first.username(), first.projectKey(), envEntries);
                })
                .sorted(Comparator.comparing(GrantGroupRow::username).thenComparing(GrantGroupRow::projectKey))
                .toList();
    }

    /**
     * 查询授权列表。
     *
     * @param filter 授权筛选条件
     * @return 授权表格行
     */
    @Transactional
    public List<GrantRow> listGrants(GrantFilter filter) {
        catalogService.syncConfiguredProjectEnvironments();
        Map<Long, DbfUser> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(DbfUser::getId, Function.identity()));
        Map<Long, DbfEnvironment> environmentMap = environmentRepository.findAll().stream()
                .collect(Collectors.toMap(DbfEnvironment::getId, Function.identity()));
        Map<Long, DbfProject> projectMap = projectRepository.findAll().stream()
                .collect(Collectors.toMap(DbfProject::getId, Function.identity()));
        return grantRepository.findAll().stream()
                .filter(grant -> matchesStatus(filter.status(), grant.getStatus()))
                .map(grant -> toGrantRow(grant, userMap, environmentMap, projectMap))
                .filter(row -> matches(filter.username(), row.username()))
                .filter(row -> matches(filter.projectKey(), row.projectKey()))
                .filter(row -> matches(filter.environmentKey(), row.environmentKey()))
                .sorted(Comparator.comparing(GrantRow::id))
                .toList();
    }

    /**
     * 授权用户访问项目环境。
     *
     * @param command 授权命令
     * @return 授权表格行
     */
    @Transactional
    public GrantRow grantEnvironment(GrantEnvironmentCommand command) {
        DbfUser user = requireActiveUser(command.userId());
        DbfEnvironment environment = resolveEnvironment(command.projectKey(), command.environmentKey());
        grantRepository.findByUserIdAndEnvironmentId(user.getId(), environment.getId()).ifPresent(grant -> {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "用户环境授权已存在");
        });
        DbfUserEnvGrant savedGrant = grantRepository.save(DbfUserEnvGrant.active(
                user.getId(),
                environment.getId(),
                required(command.grantType(), "授权类型不能为空")));
        return toGrantRow(savedGrant, userMap(user), environmentMap(environment), projectMap(environment));
    }

    /**
     * 撤销授权。
     *
     * @param grantId 授权主键
     */
    @Transactional
    public void revokeGrant(Long grantId) {
        if (grantRepository.existsById(grantId)) {
            grantRepository.deleteById(grantId);
        }
    }

    /**
     * 更新某用户在某项目下已授权的环境列表。
     * <p>
     * 与目标列表中不再包含的环境对应授权将被撤销；新加入的环境将新建 ACTIVE 授权。
     *
     * @param command 更新命令
     */
    @Transactional
    public void updateUserProjectGrants(UpdateProjectGrantsCommand command) {
        DbfUser user = requireActiveUser(command.userId());
        catalogService.syncConfiguredProjectEnvironments();
        DbfProject project = projectRepository.findByProjectKeyAndStatus(
                        required(command.projectKey(), "项目不能为空"), "ACTIVE")
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "项目不存在或不可用"));

        // 该项目下所有可用环境
        List<DbfEnvironment> projectEnvironments = environmentRepository
                .findAll().stream()
                .filter(env -> Objects.equals(env.getProjectId(), project.getId()))
                .filter(env -> "ACTIVE".equals(env.getStatus()))
                .toList();

        // 当前用户在该项目下已有的授权（key：environmentId → grant）
        Map<Long, DbfUserEnvGrant> existingByEnvId = grantRepository.findAll().stream()
                .filter(g -> Objects.equals(g.getUserId(), user.getId()))
                .filter(g -> projectEnvironments.stream()
                        .anyMatch(e -> Objects.equals(e.getId(), g.getEnvironmentId())))
                .collect(Collectors.toMap(DbfUserEnvGrant::getEnvironmentId, Function.identity()));

        String grantType = required(command.grantType(), "授权类型不能为空");
        Set<String> targetEnvKeys = command.environmentKeys() == null
                ? Set.of()
                : new HashSet<>(command.environmentKeys());

        for (DbfEnvironment env : projectEnvironments) {
            boolean wantGrant = targetEnvKeys.contains(env.getEnvironmentKey());
            DbfUserEnvGrant existing = existingByEnvId.get(env.getId());
            if (wantGrant && existing == null) {
                // 新增
                grantRepository.save(DbfUserEnvGrant.active(user.getId(), env.getId(), grantType));
            } else if (!wantGrant && existing != null) {
                // 撤销
                grantRepository.deleteById(existing.getId());
            }
        }
    }

    /**
     * 构造用户行。
     *
     * @param user   用户实体
     * @param grants 授权实体列表
     * @param tokens Token 实体列表
     * @return 用户行
     */
    private UserRow toUserRow(DbfUser user, List<DbfUserEnvGrant> grants, List<DbfApiToken> tokens) {
        long grantCount = grants.stream()
                .filter(grant -> Objects.equals(grant.getUserId(), user.getId()))
                .filter(grant -> "ACTIVE".equals(grant.getStatus()))
                .count();
        long tokenCount = tokens.stream()
                .filter(token -> Objects.equals(token.getUserId(), user.getId()))
                .filter(token -> "ACTIVE".equals(token.getStatus()))
                .count();
        return new UserRow(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                "ADMIN_SESSION",
                user.getStatus(),
                grantCount,
                tokenCount);
    }

    /**
     * 构造 Token 行。
     *
     * @param token Token 实体
     * @param user  用户实体
     * @return Token 行
     */
    private TokenRow toTokenRow(DbfApiToken token, DbfUser user) {
        return new TokenRow(
                token.getId(),
                token.getUserId(),
                user == null ? "unknown" : user.getUsername(),
                token.getTokenPrefix(),
                token.getStatus(),
                token.getExpiresAt(),
                token.getLastUsedAt(),
                null);
    }

    /**
     * 构造授权行。
     *
     * @param grant          授权实体
     * @param userMap        用户映射
     * @param environmentMap 环境映射
     * @param projectMap     项目映射
     * @return 授权行
     */
    private GrantRow toGrantRow(
            DbfUserEnvGrant grant,
            Map<Long, DbfUser> userMap,
            Map<Long, DbfEnvironment> environmentMap,
            Map<Long, DbfProject> projectMap) {
        DbfUser user = userMap.get(grant.getUserId());
        DbfEnvironment environment = environmentMap.get(grant.getEnvironmentId());
        DbfProject project = environment == null ? null : projectMap.get(environment.getProjectId());
        return new GrantRow(
                grant.getId(),
                grant.getUserId(),
                user == null ? "unknown" : user.getUsername(),
                project == null ? "unknown" : project.getProjectKey(),
                environment == null ? "unknown" : environment.getEnvironmentKey(),
                grant.getGrantType(),
                grant.getStatus());
    }

    /**
     * 根据项目和环境标识解析 active 环境。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 环境实体
     */
    private DbfEnvironment resolveEnvironment(String projectKey, String environmentKey) {
        catalogService.syncConfiguredProjectEnvironments();
        DbfProject project = projectRepository.findByProjectKeyAndStatus(required(projectKey, "项目不能为空"), "ACTIVE")
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "项目不存在或不可用"));
        return environmentRepository.findByProjectIdAndEnvironmentKeyAndStatus(
                        project.getId(),
                        required(environmentKey, "环境不能为空"),
                        "ACTIVE")
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "环境不存在或不可用"));
    }

    /**
     * 查询用户并要求 active。
     *
     * @param userId 用户主键
     * @return active 用户
     */
    private DbfUser requireActiveUser(Long userId) {
        DbfUser user = requireUser(userId);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "用户已禁用");
        }
        return user;
    }

    /**
     * 查询用户。
     *
     * @param userId 用户主键
     * @return 用户实体
     */
    private DbfUser requireUser(Long userId) {
        if (userId == null) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "用户不能为空");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "用户不存在"));
    }

    /**
     * 计算 Token 过期时间。
     *
     * @param expiresInDays 有效天数
     * @return 过期时间
     */
    private Instant expiresAt(Integer expiresInDays) {
        int days = expiresInDays == null || expiresInDays <= 0 ? DEFAULT_EXPIRES_IN_DAYS : expiresInDays;
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    /**
     * 构造单元素用户映射。
     *
     * @param user 用户实体
     * @return 用户映射
     */
    private Map<Long, DbfUser> userMap(DbfUser user) {
        return Map.of(user.getId(), user);
    }

    /**
     * 构造单元素环境映射。
     *
     * @param environment 环境实体
     * @return 环境映射
     */
    private Map<Long, DbfEnvironment> environmentMap(DbfEnvironment environment) {
        return Map.of(environment.getId(), environment);
    }

    /**
     * 构造单元素项目映射。
     *
     * @param environment 环境实体
     * @return 项目映射
     */
    private Map<Long, DbfProject> projectMap(DbfEnvironment environment) {
        return projectRepository.findById(environment.getProjectId())
                .map(project -> Map.of(project.getId(), project))
                .orElseGet(Map::of);
    }

    /**
     * 判断字符串过滤是否匹配。
     *
     * @param filter 过滤值
     * @param value  待匹配值
     * @return 匹配时返回 true
     */
    private boolean matches(String filter, String value) {
        return isBlank(filter) || "全部".equals(filter)
                || (value != null && value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)));
    }

    /**
     * 判断状态过滤是否匹配。
     *
     * @param filter 状态过滤
     * @param status 当前状态
     * @return 匹配时返回 true
     */
    private boolean matchesStatus(String filter, String status) {
        return isBlank(filter) || "全部".equals(filter) || Objects.equals(filter, status);
    }

    /**
     * 判断用户过滤是否匹配。
     *
     * @param filter 用户名过滤
     * @param user   用户实体
     * @return 匹配时返回 true
     */
    private boolean matchesUser(String filter, DbfUser user) {
        return isBlank(filter) || "全部".equals(filter)
                || (user != null && matches(filter, user.getUsername()));
    }

    /**
     * 校验必填字符串。
     *
     * @param value   待校验值
     * @param message 错误消息
     * @return 去空白后的值
     */
    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, message);
        }
        return value.trim();
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 待判断值
     * @return 空白时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
