package com.refinex.dbflow.config.properties;

import com.refinex.dbflow.config.model.DangerousDdlDecision;
import com.refinex.dbflow.config.model.DangerousDdlOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.*;

/**
 * DBFlow YAML 配置绑定模型，覆盖数据源默认值、项目环境和高危 DDL 策略。
 *
 * @author refinex
 */
@Validated
@ConfigurationProperties(prefix = "dbflow")
public class DbflowProperties implements InitializingBean {

    /**
     * 目标数据源默认配置，不存放真实密码。
     */
    @Valid
    private DatasourceDefaults datasourceDefaults = new DatasourceDefaults();

    /**
     * 项目配置列表；允许为空，便于骨架阶段本地启动。
     */
    @Valid
    private List<Project> projects = new ArrayList<>();

    /**
     * DBFlow 策略配置集合。
     */
    @Valid
    private Policies policies = new Policies();

    /**
     * 判断字符串是否为空白。
     *
     * @param value 待判断字符串
     * @return 空白时返回 true
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 返回目标数据源默认配置。
     *
     * @return 目标数据源默认配置
     */
    public DatasourceDefaults getDatasourceDefaults() {
        return datasourceDefaults;
    }

    /**
     * 设置目标数据源默认配置。
     *
     * @param datasourceDefaults 目标数据源默认配置
     */
    public void setDatasourceDefaults(DatasourceDefaults datasourceDefaults) {
        this.datasourceDefaults = Objects.requireNonNullElseGet(datasourceDefaults, DatasourceDefaults::new);
    }

    /**
     * 返回项目配置列表。
     *
     * @return 项目配置列表
     */
    public List<Project> getProjects() {
        return projects;
    }

    /**
     * 设置项目配置列表。
     *
     * @param projects 项目配置列表
     */
    public void setProjects(List<Project> projects) {
        this.projects = Objects.requireNonNullElseGet(projects, ArrayList::new);
    }

    /**
     * 返回策略配置集合。
     *
     * @return 策略配置集合
     */
    public Policies getPolicies() {
        return policies;
    }

    /**
     * 设置策略配置集合。
     *
     * @param policies 策略配置集合
     */
    public void setPolicies(Policies policies) {
        this.policies = Objects.requireNonNullElseGet(policies, Policies::new);
    }

    /**
     * 在配置绑定完成后执行跨字段启动期校验。
     */
    @Override
    public void afterPropertiesSet() {
        datasourceDefaults.validate();
        validateProjectKeys();
        validateProjects();
        policies.getDangerousDdl().validate();
    }

    /**
     * 校验项目 key 在全局范围内唯一。
     */
    private void validateProjectKeys() {
        Set<String> projectKeys = new HashSet<>();
        for (Project project : projects) {
            if (!projectKeys.add(project.getKey())) {
                throw new IllegalStateException("dbflow.projects 存在重复 project key: " + project.getKey());
            }
        }
    }

    /**
     * 校验所有项目下的环境配置。
     */
    private void validateProjects() {
        for (Project project : projects) {
            validateProjectEnvironments(project);
        }
    }

    /**
     * 校验单个项目下的环境 key 和数据源必填项。
     *
     * @param project 项目配置
     */
    private void validateProjectEnvironments(Project project) {
        Set<String> environmentKeys = new HashSet<>();
        for (Environment environment : project.getEnvironments()) {
            if (!environmentKeys.add(environment.getKey())) {
                throw new IllegalStateException("dbflow.projects[].environments 存在重复 environment key: "
                        + project.getKey() + "/" + environment.getKey());
            }
            validateEnvironmentDatasource(project, environment);
        }
    }

    /**
     * 校验环境数据源连接串和驱动配置。
     *
     * @param project     项目配置
     * @param environment 环境配置
     */
    private void validateEnvironmentDatasource(Project project, Environment environment) {
        if (isBlank(environment.getJdbcUrl())) {
            throw new IllegalStateException("dbflow.projects[].environments[].jdbc-url 不能为空: "
                    + project.getKey() + "/" + environment.getKey());
        }
        if (containsPasswordParameter(environment.getJdbcUrl())) {
            throw new IllegalStateException("dbflow.projects[].environments[].jdbc-url 不能包含密码参数: "
                    + project.getKey() + "/" + environment.getKey());
        }
        if (isBlank(environment.getDriverClassName()) && isBlank(datasourceDefaults.getDriverClassName())) {
            throw new IllegalStateException("dbflow datasource driver 不能为空: "
                    + project.getKey() + "/" + environment.getKey());
        }
    }

    /**
     * 判断 JDBC URL 是否包含密码参数，避免连接池或驱动日志泄露密码。
     *
     * @param jdbcUrl JDBC URL
     * @return 包含密码参数时返回 true
     */
    private boolean containsPasswordParameter(String jdbcUrl) {
        String normalized = jdbcUrl.toLowerCase(Locale.ROOT);
        return normalized.contains("password=") || normalized.contains("password:");
    }

    /**
     * 数据源默认配置，供项目环境继承非敏感连接参数。
     *
     * @author refinex
     */
    public static class DatasourceDefaults {

        /**
         * 默认 JDBC 驱动类名。
         */
        private String driverClassName;

        /**
         * 默认数据库用户名，不应写入生产敏感账号。
         */
        private String username;

        /**
         * 默认数据库密码，仅允许环境变量占位或空值。
         */
        private String password;

        /**
         * 是否在启动阶段主动获取连接校验目标库可用性；默认关闭，避免本地无目标库时阻断启动。
         */
        private boolean validateOnStartup;

        /**
         * 共享 Hikari 连接池配置，应用到每个项目环境目标数据源。
         */
        @Valid
        private Hikari hikari = new Hikari();

        /**
         * 返回默认 JDBC 驱动类名。
         *
         * @return 默认 JDBC 驱动类名
         */
        public String getDriverClassName() {
            return driverClassName;
        }

        /**
         * 设置默认 JDBC 驱动类名。
         *
         * @param driverClassName 默认 JDBC 驱动类名
         */
        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        /**
         * 返回默认数据库用户名。
         *
         * @return 默认数据库用户名
         */
        public String getUsername() {
            return username;
        }

        /**
         * 设置默认数据库用户名。
         *
         * @param username 默认数据库用户名
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * 返回默认数据库密码占位。
         *
         * @return 默认数据库密码占位
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置默认数据库密码占位。
         *
         * @param password 默认数据库密码占位
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * 返回是否启用启动期连接校验。
         *
         * @return 启用时返回 true
         */
        public boolean isValidateOnStartup() {
            return validateOnStartup;
        }

        /**
         * 设置是否启用启动期连接校验。
         *
         * @param validateOnStartup 是否启用启动期连接校验
         */
        public void setValidateOnStartup(boolean validateOnStartup) {
            this.validateOnStartup = validateOnStartup;
        }

        /**
         * 返回共享 Hikari 连接池配置。
         *
         * @return 共享 Hikari 连接池配置
         */
        public Hikari getHikari() {
            return hikari;
        }

        /**
         * 设置共享 Hikari 连接池配置。
         *
         * @param hikari 共享 Hikari 连接池配置
         */
        public void setHikari(Hikari hikari) {
            this.hikari = Objects.requireNonNullElseGet(hikari, Hikari::new);
        }

        /**
         * 校验共享 Hikari 连接池配置。
         */
        private void validate() {
            hikari.validate();
        }
    }

    /**
     * Hikari 连接池共享配置。
     *
     * @author refinex
     */
    public static class Hikari {

        /**
         * 连接池名称前缀，最终池名会追加 projectKey 和 environmentKey。
         */
        private String poolNamePrefix = "dbflow-target";

        /**
         * 最大连接数；为空时使用 Hikari 默认值。
         */
        @Min(value = 1, message = "maximum-pool-size 必须大于等于 1")
        private Integer maximumPoolSize;

        /**
         * 最小空闲连接数；为空时使用 Hikari 默认值。
         */
        @Min(value = 0, message = "minimum-idle 必须大于等于 0")
        private Integer minimumIdle;

        /**
         * 获取连接最大等待时间；为空时使用 Hikari 默认值。
         */
        private Duration connectionTimeout;

        /**
         * 空闲连接保留时间；为空时使用 Hikari 默认值。
         */
        private Duration idleTimeout;

        /**
         * 连接最大生命周期；为空时使用 Hikari 默认值。
         */
        private Duration maxLifetime;

        /**
         * 返回连接池名称前缀。
         *
         * @return 连接池名称前缀
         */
        public String getPoolNamePrefix() {
            return poolNamePrefix;
        }

        /**
         * 设置连接池名称前缀。
         *
         * @param poolNamePrefix 连接池名称前缀
         */
        public void setPoolNamePrefix(String poolNamePrefix) {
            this.poolNamePrefix = poolNamePrefix;
        }

        /**
         * 返回最大连接数。
         *
         * @return 最大连接数
         */
        public Integer getMaximumPoolSize() {
            return maximumPoolSize;
        }

        /**
         * 设置最大连接数。
         *
         * @param maximumPoolSize 最大连接数
         */
        public void setMaximumPoolSize(Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        /**
         * 返回最小空闲连接数。
         *
         * @return 最小空闲连接数
         */
        public Integer getMinimumIdle() {
            return minimumIdle;
        }

        /**
         * 设置最小空闲连接数。
         *
         * @param minimumIdle 最小空闲连接数
         */
        public void setMinimumIdle(Integer minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        /**
         * 返回获取连接最大等待时间。
         *
         * @return 获取连接最大等待时间
         */
        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        /**
         * 设置获取连接最大等待时间。
         *
         * @param connectionTimeout 获取连接最大等待时间
         */
        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        /**
         * 返回空闲连接保留时间。
         *
         * @return 空闲连接保留时间
         */
        public Duration getIdleTimeout() {
            return idleTimeout;
        }

        /**
         * 设置空闲连接保留时间。
         *
         * @param idleTimeout 空闲连接保留时间
         */
        public void setIdleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        /**
         * 返回连接最大生命周期。
         *
         * @return 连接最大生命周期
         */
        public Duration getMaxLifetime() {
            return maxLifetime;
        }

        /**
         * 设置连接最大生命周期。
         *
         * @param maxLifetime 连接最大生命周期
         */
        public void setMaxLifetime(Duration maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        /**
         * 校验 Hikari 连接池配置。
         */
        private void validate() {
            if (maximumPoolSize != null && minimumIdle != null && minimumIdle > maximumPoolSize) {
                throw new IllegalStateException("dbflow.datasource-defaults.hikari minimum-idle 不能大于 maximum-pool-size");
            }
        }
    }

    /**
     * DBFlow 项目配置。
     *
     * @author refinex
     */
    public static class Project {

        /**
         * 项目标识，必须全局唯一。
         */
        @NotBlank(message = "project key 不能为空")
        private String key;

        /**
         * 项目展示名称。
         */
        private String name;

        /**
         * 项目下的环境配置列表。
         */
        @Valid
        private List<Environment> environments = new ArrayList<>();

        /**
         * 返回项目标识。
         *
         * @return 项目标识
         */
        public String getKey() {
            return key;
        }

        /**
         * 设置项目标识。
         *
         * @param key 项目标识
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * 返回项目展示名称。
         *
         * @return 项目展示名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置项目展示名称。
         *
         * @param name 项目展示名称
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 返回项目环境配置列表。
         *
         * @return 项目环境配置列表
         */
        public List<Environment> getEnvironments() {
            return environments;
        }

        /**
         * 设置项目环境配置列表。
         *
         * @param environments 项目环境配置列表
         */
        public void setEnvironments(List<Environment> environments) {
            this.environments = Objects.requireNonNullElseGet(environments, ArrayList::new);
        }
    }

    /**
     * DBFlow 项目环境配置。
     *
     * @author refinex
     */
    public static class Environment {

        /**
         * 环境标识，必须在所属项目内唯一。
         */
        @NotBlank(message = "environment key 不能为空")
        private String key;

        /**
         * 环境展示名称。
         */
        private String name;

        /**
         * 目标库 JDBC URL，必须由配置显式提供。
         */
        private String jdbcUrl;

        /**
         * JDBC 驱动类名；为空时继承 datasource-defaults.driver-class-name。
         */
        private String driverClassName;

        /**
         * 数据库用户名；为空时可由后续执行层继承默认值。
         */
        private String username;

        /**
         * 数据库密码，仅允许环境变量占位或空值。
         */
        private String password;

        /**
         * 返回环境标识。
         *
         * @return 环境标识
         */
        public String getKey() {
            return key;
        }

        /**
         * 设置环境标识。
         *
         * @param key 环境标识
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * 返回环境展示名称。
         *
         * @return 环境展示名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置环境展示名称。
         *
         * @param name 环境展示名称
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 返回目标库 JDBC URL。
         *
         * @return 目标库 JDBC URL
         */
        public String getJdbcUrl() {
            return jdbcUrl;
        }

        /**
         * 设置目标库 JDBC URL。
         *
         * @param jdbcUrl 目标库 JDBC URL
         */
        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        /**
         * 返回 JDBC 驱动类名。
         *
         * @return JDBC 驱动类名
         */
        public String getDriverClassName() {
            return driverClassName;
        }

        /**
         * 设置 JDBC 驱动类名。
         *
         * @param driverClassName JDBC 驱动类名
         */
        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        /**
         * 返回数据库用户名。
         *
         * @return 数据库用户名
         */
        public String getUsername() {
            return username;
        }

        /**
         * 设置数据库用户名。
         *
         * @param username 数据库用户名
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * 返回数据库密码占位。
         *
         * @return 数据库密码占位
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置数据库密码占位。
         *
         * @param password 数据库密码占位
         */
        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * DBFlow 策略配置集合。
     *
     * @author refinex
     */
    public static class Policies {

        /**
         * 高危 DDL 策略配置。
         */
        @Valid
        private DangerousDdl dangerousDdl = new DangerousDdl();

        /**
         * 返回高危 DDL 策略配置。
         *
         * @return 高危 DDL 策略配置
         */
        public DangerousDdl getDangerousDdl() {
            return dangerousDdl;
        }

        /**
         * 设置高危 DDL 策略配置。
         *
         * @param dangerousDdl 高危 DDL 策略配置
         */
        public void setDangerousDdl(DangerousDdl dangerousDdl) {
            this.dangerousDdl = Objects.requireNonNullElseGet(dangerousDdl, DangerousDdl::new);
        }
    }

    /**
     * 高危 DDL 策略配置。
     *
     * @author refinex
     */
    public static class DangerousDdl {

        /**
         * 高危 DDL 默认处置策略。
         */
        private Map<DangerousDdlOperation, DangerousDdlDecision> defaults = defaultDecisions();

        /**
         * 高危 DDL 白名单条目列表。
         */
        @Valid
        private List<WhitelistEntry> whitelist = new ArrayList<>();

        /**
         * 创建高危 DDL 默认处置策略。
         *
         * @return 高危 DDL 默认处置策略
         */
        private static Map<DangerousDdlOperation, DangerousDdlDecision> defaultDecisions() {
            Map<DangerousDdlOperation, DangerousDdlDecision> decisions = new EnumMap<>(DangerousDdlOperation.class);
            decisions.put(DangerousDdlOperation.DROP_TABLE, DangerousDdlDecision.DENY);
            decisions.put(DangerousDdlOperation.DROP_DATABASE, DangerousDdlDecision.DENY);
            decisions.put(DangerousDdlOperation.TRUNCATE, DangerousDdlDecision.REQUIRE_CONFIRMATION);
            return decisions;
        }

        /**
         * 返回高危 DDL 默认处置策略。
         *
         * @return 高危 DDL 默认处置策略
         */
        public Map<DangerousDdlOperation, DangerousDdlDecision> getDefaults() {
            return defaults;
        }

        /**
         * 设置高危 DDL 默认处置策略。
         *
         * @param defaults 高危 DDL 默认处置策略
         */
        public void setDefaults(Map<DangerousDdlOperation, DangerousDdlDecision> defaults) {
            this.defaults = defaultDecisions();
            if (defaults != null) {
                this.defaults.putAll(defaults);
            }
        }

        /**
         * 返回高危 DDL 白名单条目列表。
         *
         * @return 高危 DDL 白名单条目列表
         */
        public List<WhitelistEntry> getWhitelist() {
            return whitelist;
        }

        /**
         * 设置高危 DDL 白名单条目列表。
         *
         * @param whitelist 高危 DDL 白名单条目列表
         */
        public void setWhitelist(List<WhitelistEntry> whitelist) {
            this.whitelist = Objects.requireNonNullElseGet(whitelist, ArrayList::new);
        }

        /**
         * 返回指定操作的有效默认策略。
         *
         * @param operation 高危 DDL 操作类型
         * @return 有效默认策略
         */
        public DangerousDdlDecision defaultDecision(DangerousDdlOperation operation) {
            return defaults.get(operation);
        }

        /**
         * 校验高危 DDL 策略配置。
         */
        private void validate() {
            for (DangerousDdlOperation operation : DangerousDdlOperation.values()) {
                if (defaults.get(operation) == null) {
                    throw new IllegalStateException("dbflow.policies.dangerous-ddl.defaults 缺少操作策略: " + operation);
                }
            }
            for (WhitelistEntry entry : whitelist) {
                entry.validate();
            }
        }
    }

    /**
     * 高危 DDL 白名单条目。
     *
     * @author refinex
     */
    public static class WhitelistEntry {

        /**
         * 白名单所属项目标识。
         */
        @NotBlank(message = "whitelist project-key 不能为空")
        private String projectKey;

        /**
         * 白名单所属环境标识。
         */
        @NotBlank(message = "whitelist environment-key 不能为空")
        private String environmentKey;

        /**
         * 白名单所属 schema 名称。
         */
        @NotBlank(message = "whitelist schema-name 不能为空")
        private String schemaName;

        /**
         * 白名单所属表名；数据库级操作必须为空，表级操作必须非空。
         */
        private String tableName;

        /**
         * 白名单适用的高危 DDL 操作类型。
         */
        private DangerousDdlOperation operation;

        /**
         * 是否显式允许生产环境执行该白名单命中的高危 DDL。
         */
        private boolean allowProdDangerousDdl;

        /**
         * 返回白名单所属项目标识。
         *
         * @return 白名单所属项目标识
         */
        public String getProjectKey() {
            return projectKey;
        }

        /**
         * 设置白名单所属项目标识。
         *
         * @param projectKey 白名单所属项目标识
         */
        public void setProjectKey(String projectKey) {
            this.projectKey = projectKey;
        }

        /**
         * 返回白名单所属环境标识。
         *
         * @return 白名单所属环境标识
         */
        public String getEnvironmentKey() {
            return environmentKey;
        }

        /**
         * 设置白名单所属环境标识。
         *
         * @param environmentKey 白名单所属环境标识
         */
        public void setEnvironmentKey(String environmentKey) {
            this.environmentKey = environmentKey;
        }

        /**
         * 返回白名单所属 schema 名称。
         *
         * @return 白名单所属 schema 名称
         */
        public String getSchemaName() {
            return schemaName;
        }

        /**
         * 设置白名单所属 schema 名称。
         *
         * @param schemaName 白名单所属 schema 名称
         */
        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        /**
         * 返回白名单所属表名。
         *
         * @return 白名单所属表名
         */
        public String getTableName() {
            return tableName;
        }

        /**
         * 设置白名单所属表名。
         *
         * @param tableName 白名单所属表名
         */
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        /**
         * 返回白名单适用操作类型。
         *
         * @return 白名单适用操作类型
         */
        public DangerousDdlOperation getOperation() {
            return operation;
        }

        /**
         * 设置白名单适用操作类型。
         *
         * @param operation 白名单适用操作类型
         */
        public void setOperation(DangerousDdlOperation operation) {
            this.operation = operation;
        }

        /**
         * 返回是否显式允许生产环境高危 DDL。
         *
         * @return 允许时返回 true
         */
        public boolean isAllowProdDangerousDdl() {
            return allowProdDangerousDdl;
        }

        /**
         * 设置是否显式允许生产环境高危 DDL。
         *
         * @param allowProdDangerousDdl 是否允许生产环境高危 DDL
         */
        public void setAllowProdDangerousDdl(boolean allowProdDangerousDdl) {
            this.allowProdDangerousDdl = allowProdDangerousDdl;
        }

        /**
         * 校验白名单条目的操作粒度。
         */
        private void validate() {
            if (operation == null) {
                throw new IllegalStateException("dbflow.policies.dangerous-ddl.whitelist.operation 不能为空");
            }
            if (operation == DangerousDdlOperation.DROP_DATABASE && !isBlank(tableName)) {
                throw new IllegalStateException("DROP_DATABASE 白名单不能配置 table-name");
            }
            if (operation != DangerousDdlOperation.DROP_DATABASE && isBlank(tableName)) {
                throw new IllegalStateException(operation + " 白名单必须配置 table-name");
            }
        }
    }
}
