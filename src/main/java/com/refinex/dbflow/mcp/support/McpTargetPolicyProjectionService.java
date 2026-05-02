package com.refinex.dbflow.mcp.support;

import com.refinex.dbflow.access.dto.ConfiguredEnvironmentView;
import com.refinex.dbflow.access.service.ProjectEnvironmentCatalogService;
import com.refinex.dbflow.config.model.DangerousDdlDecision;
import com.refinex.dbflow.config.model.DangerousDdlOperation;
import com.refinex.dbflow.config.properties.DbflowProperties;
import com.refinex.dbflow.mcp.auth.McpAccessBoundaryService;
import com.refinex.dbflow.mcp.auth.McpAuthenticationContext;
import com.refinex.dbflow.mcp.auth.McpAuthorizationBoundary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.refinex.dbflow.mcp.support.McpResponseBuilder.data;

/**
 * MCP 目标与策略投影服务，统一为 tool/resource 输出已授权视图。
 *
 * @author refinex
 */
@Service
public class McpTargetPolicyProjectionService {

    /**
     * 通配符配置值。
     */
    private static final String WILDCARD = "*";

    /**
     * 项目环境配置目录服务。
     */
    private final ProjectEnvironmentCatalogService catalogService;

    /**
     * DBFlow 配置属性。
     */
    private final DbflowProperties dbflowProperties;

    /**
     * 创建 MCP 目标与策略投影服务。
     *
     * @param catalogService   项目环境配置目录服务
     * @param dbflowProperties DBFlow 配置属性
     */
    public McpTargetPolicyProjectionService(
            ProjectEnvironmentCatalogService catalogService,
            DbflowProperties dbflowProperties
    ) {
        this.catalogService = catalogService;
        this.dbflowProperties = dbflowProperties;
    }

    /**
     * 查询当前 MCP 主体可见的项目环境目标。
     *
     * @param context               MCP 认证上下文
     * @param accessBoundaryService MCP 访问授权边界服务
     * @param operation             MCP 操作名称
     * @return 可见目标列表
     */
    public List<Map<String, Object>> visibleTargets(
            McpAuthenticationContext context,
            McpAccessBoundaryService accessBoundaryService,
            String operation
    ) {
        if (!context.authenticated()) {
            return List.of();
        }
        return catalogService.listConfiguredEnvironments()
                .stream()
                .filter(view -> allowed(context, accessBoundaryService, operation, view))
                .sorted(Comparator
                        .comparing(ConfiguredEnvironmentView::projectKey)
                        .thenComparing(ConfiguredEnvironmentView::environmentKey))
                .map(this::targetData)
                .toList();
    }

    /**
     * 创建目标项目环境的有效危险 DDL 策略视图。
     *
     * @param project   项目标识
     * @param env       环境标识
     * @param schema    schema 名称
     * @param table     表名
     * @param operation SQL 操作类型
     * @param boundary  MCP 授权边界
     * @return 策略视图
     */
    public Map<String, Object> effectivePolicy(
            String project,
            String env,
            String schema,
            String table,
            String operation,
            McpAuthorizationBoundary boundary
    ) {
        DangerousDdlOperation dangerousOperation = parseDangerousOperation(operation);
        if (!boundary.allowed()) {
            return data(
                    "project", project,
                    "env", env,
                    "schema", schema,
                    "table", table,
                    "operation", operation,
                    "status", "DENIED",
                    "dangerousDdlOperation", dangerousOperation == null ? null : dangerousOperation.name(),
                    "effectiveDefaultDecision", null,
                    "defaults", Map.of(),
                    "whitelist", List.of()
            );
        }
        DbflowProperties.DangerousDdl dangerousDdl = dbflowProperties.getPolicies().getDangerousDdl();
        return data(
                "project", project,
                "env", env,
                "schema", schema,
                "table", table,
                "operation", operation,
                "status", "AUTHORIZED",
                "dangerousDdlOperation", dangerousOperation == null ? null : dangerousOperation.name(),
                "effectiveDefaultDecision", defaultDecision(dangerousDdl, dangerousOperation),
                "defaults", defaultsData(dangerousDdl),
                "whitelist", whitelistData(dangerousDdl, project, env, schema, table, operation, dangerousOperation)
        );
    }

    /**
     * 判断配置目标是否允许当前主体访问。
     *
     * @param context               MCP 认证上下文
     * @param accessBoundaryService MCP 访问授权边界服务
     * @param operation             MCP 操作名称
     * @param view                  配置环境视图
     * @return 允许访问时返回 true
     */
    private boolean allowed(
            McpAuthenticationContext context,
            McpAccessBoundaryService accessBoundaryService,
            String operation,
            ConfiguredEnvironmentView view
    ) {
        McpAuthorizationBoundary boundary = accessBoundaryService.targetBoundary(
                context,
                view.projectKey(),
                view.environmentKey(),
                operation
        );
        return boundary.allowed();
    }

    /**
     * 创建脱敏目标视图。
     *
     * @param view 配置环境视图
     * @return 脱敏目标数据
     */
    private Map<String, Object> targetData(ConfiguredEnvironmentView view) {
        return data(
                "project", view.projectKey(),
                "projectName", view.projectName(),
                "env", view.environmentKey(),
                "environmentName", view.environmentName(),
                "databaseName", databaseName(view.jdbcUrl()),
                "driverClassName", view.driverClassName(),
                "username", view.username(),
                "metadataPresent", view.metadataPresent()
        );
    }

    /**
     * 提取 JDBC URL 中的数据库名，不返回完整连接串。
     *
     * @param jdbcUrl JDBC URL
     * @return 数据库名
     */
    private String databaseName(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl)) {
            return null;
        }
        int authorityStart = jdbcUrl.indexOf("://");
        int searchStart = authorityStart < 0 ? 0 : authorityStart + 3;
        int slash = jdbcUrl.indexOf('/', searchStart);
        if (slash < 0 || slash + 1 >= jdbcUrl.length()) {
            return null;
        }
        int end = jdbcUrl.length();
        int query = jdbcUrl.indexOf('?', slash + 1);
        if (query >= 0) {
            end = Math.min(end, query);
        }
        int semicolon = jdbcUrl.indexOf(';', slash + 1);
        if (semicolon >= 0) {
            end = Math.min(end, semicolon);
        }
        String name = jdbcUrl.substring(slash + 1, end);
        return StringUtils.hasText(name) ? name : null;
    }

    /**
     * 创建默认危险 DDL 策略视图。
     *
     * @param dangerousDdl 高危 DDL 配置
     * @return 默认策略视图
     */
    private Map<String, Object> defaultsData(DbflowProperties.DangerousDdl dangerousDdl) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        dangerousDdl.getDefaults()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> defaults.put(entry.getKey().name(), entry.getValue().name()));
        return defaults;
    }

    /**
     * 创建匹配当前范围的白名单视图。
     *
     * @param dangerousDdl       高危 DDL 配置
     * @param project            项目标识
     * @param env                环境标识
     * @param schema             schema 名称
     * @param table              表名
     * @param rawOperation       原始 SQL 操作类型
     * @param dangerousOperation 高危 DDL 操作类型
     * @return 白名单视图
     */
    private List<Map<String, Object>> whitelistData(
            DbflowProperties.DangerousDdl dangerousDdl,
            String project,
            String env,
            String schema,
            String table,
            String rawOperation,
            DangerousDdlOperation dangerousOperation
    ) {
        return dangerousDdl.getWhitelist()
                .stream()
                .filter(entry -> matchesWhitelist(entry, project, env, schema, table, rawOperation, dangerousOperation))
                .map(this::whitelistEntryData)
                .toList();
    }

    /**
     * 判断白名单条目是否匹配当前查询范围。
     *
     * @param entry              白名单条目
     * @param project            项目标识
     * @param env                环境标识
     * @param schema             schema 名称
     * @param table              表名
     * @param rawOperation       原始 SQL 操作类型
     * @param dangerousOperation 高危 DDL 操作类型
     * @return 匹配时返回 true
     */
    private boolean matchesWhitelist(
            DbflowProperties.WhitelistEntry entry,
            String project,
            String env,
            String schema,
            String table,
            String rawOperation,
            DangerousDdlOperation dangerousOperation
    ) {
        if (!matchesField(entry.getProjectKey(), project) || !matchesField(entry.getEnvironmentKey(), env)) {
            return false;
        }
        if (StringUtils.hasText(rawOperation) && dangerousOperation == null) {
            return false;
        }
        if (dangerousOperation != null && entry.getOperation() != dangerousOperation) {
            return false;
        }
        return matchesOptionalField(entry.getSchemaName(), schema)
                && matchesOptionalField(entry.getTableName(), table);
    }

    /**
     * 创建白名单条目视图。
     *
     * @param entry 白名单条目
     * @return 白名单条目视图
     */
    private Map<String, Object> whitelistEntryData(DbflowProperties.WhitelistEntry entry) {
        return data(
                "operation", entry.getOperation().name(),
                "project", entry.getProjectKey(),
                "env", entry.getEnvironmentKey(),
                "schema", entry.getSchemaName(),
                "table", entry.getTableName(),
                "allowProdDangerousDdl", entry.isAllowProdDangerousDdl()
        );
    }

    /**
     * 解析危险 DDL 操作类型。
     *
     * @param operation SQL 操作类型
     * @return 危险 DDL 操作类型
     */
    private DangerousDdlOperation parseDangerousOperation(String operation) {
        if (!StringUtils.hasText(operation)) {
            return null;
        }
        String normalized = operation.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        try {
            return DangerousDdlOperation.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * 获取高危操作默认处置策略名称。
     *
     * @param dangerousDdl 高危 DDL 配置
     * @param operation    高危 DDL 操作类型
     * @return 默认处置策略名称
     */
    private String defaultDecision(DbflowProperties.DangerousDdl dangerousDdl, DangerousDdlOperation operation) {
        if (operation == null) {
            return null;
        }
        DangerousDdlDecision decision = dangerousDdl.defaultDecision(operation);
        return decision == null ? null : decision.name();
    }

    /**
     * 判断必填字段是否匹配配置值。
     *
     * @param pattern 配置值
     * @param value   请求值
     * @return 匹配时返回 true
     */
    private boolean matchesField(String pattern, String value) {
        return WILDCARD.equals(pattern) || Objects.equals(pattern, value);
    }

    /**
     * 判断可选字段是否匹配配置值。
     *
     * @param pattern 配置值
     * @param value   请求值
     * @return 匹配时返回 true
     */
    private boolean matchesOptionalField(String pattern, String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        return matchesField(pattern, value);
    }
}
