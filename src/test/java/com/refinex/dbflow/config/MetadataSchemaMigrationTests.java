package com.refinex.dbflow.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 元数据库 Flyway schema 迁移测试。
 *
 * @author refinex
 */
class MetadataSchemaMigrationTests {

    /**
     * 核心元数据库表名称。
     */
    private static final Set<String> CORE_TABLES = Set.of(
            "dbf_users",
            "dbf_api_tokens",
            "dbf_projects",
            "dbf_environments",
            "dbf_user_env_grants",
            "dbf_confirmation_challenges",
            "dbf_audit_events"
    );

    /**
     * 验证 V1 migration 可以干净创建全部核心表。
     *
     * @throws SQLException 数据库元数据读取异常
     */
    @Test
    void migrationShouldCreateCoreTables() throws SQLException {
        try (Connection connection = migratedConnection()) {
            Set<String> tableNames = readTableNames(connection);

            assertThat(tableNames).containsAll(CORE_TABLES);
        }
    }

    /**
     * 验证 token 表不包含明文字段，并强制一个用户最多只有一个 active token。
     *
     * @throws SQLException 数据库操作异常
     */
    @Test
    void tokenSchemaShouldHidePlaintextAndEnforceSingleActiveToken() throws SQLException {
        try (Connection connection = migratedConnection()) {
            Set<String> tokenColumns = readColumnNames(connection, "dbf_api_tokens");
            long userId = insertUser(connection, "alice");

            assertThat(tokenColumns)
                    .contains("token_hash", "token_prefix", "status", "last_used_at")
                    .doesNotContain("token", "token_plaintext", "plain_token");

            insertToken(connection, userId, "hash-1", "dbf_1", "ACTIVE", 1);

            assertThatThrownBy(() -> insertToken(connection, userId, "hash-2", "dbf_2", "ACTIVE", 1))
                    .isInstanceOf(SQLException.class);

            insertToken(connection, userId, "hash-3", "dbf_3", "REVOKED", null);
            insertToken(connection, userId, "hash-4", "dbf_4", "EXPIRED", null);
        }
    }

    /**
     * 验证用户到项目环境授权的唯一约束。
     *
     * @throws SQLException 数据库操作异常
     */
    @Test
    void grantSchemaShouldEnforceUniqueUserEnvironmentGrant() throws SQLException {
        try (Connection connection = migratedConnection()) {
            long userId = insertUser(connection, "bob");
            long environmentId = insertEnvironment(connection, "project-a", "dev");

            insertGrant(connection, userId, environmentId);

            assertThatThrownBy(() -> insertGrant(connection, userId, environmentId))
                    .isInstanceOf(SQLException.class);
        }
    }

    /**
     * 验证审计表只保存摘要字段，并具备常用查询索引。
     *
     * @throws SQLException 数据库元数据读取异常
     */
    @Test
    void auditSchemaShouldExposeSummaryColumnsAndQueryIndexes() throws SQLException {
        try (Connection connection = migratedConnection()) {
            Set<String> auditColumns = readColumnNames(connection, "dbf_audit_events");
            Set<String> auditIndexes = readIndexNames(connection, "dbf_audit_events");

            assertThat(auditColumns)
                    .contains(
                            "request_id",
                            "user_id",
                            "project_key",
                            "environment_key",
                            "client_name",
                            "operation_type",
                            "risk_level",
                            "status",
                            "sql_hash",
                            "sql_text",
                            "result_summary",
                            "affected_rows",
                            "error_code",
                            "error_message",
                            "created_at"
                    )
                    .doesNotContain("result_set", "full_result", "rows_json");

            assertThat(auditIndexes).contains(
                    "idx_dbf_audit_user_time",
                    "idx_dbf_audit_target_time",
                    "idx_dbf_audit_status_time",
                    "idx_dbf_audit_sql_hash",
                    "idx_dbf_audit_request_id"
            );
        }
    }

    /**
     * 验证确认挑战绑定 user、token、project/env 和 SQL hash。
     *
     * @throws SQLException 数据库元数据读取异常
     */
    @Test
    void confirmationSchemaShouldBindTokenTargetAndSqlHash() throws SQLException {
        try (Connection connection = migratedConnection()) {
            Set<String> confirmationColumns = readColumnNames(connection, "dbf_confirmation_challenges");
            Set<String> confirmationIndexes = readIndexNames(connection, "dbf_confirmation_challenges");

            assertThat(confirmationColumns)
                    .contains(
                            "user_id",
                            "token_id",
                            "environment_id",
                            "project_key",
                            "environment_key",
                            "confirmation_id",
                            "sql_hash",
                            "risk_level",
                            "expires_at",
                            "confirmed_at"
                    );
            assertThat(confirmationIndexes).contains(
                    "idx_dbf_confirmation_user_status",
                    "idx_dbf_confirmation_environment_status",
                    "idx_dbf_confirmation_target_status"
            );
        }
    }

    /**
     * 验证关键唯一索引和约束名称存在，便于后续故障诊断。
     *
     * @throws SQLException 数据库元数据读取异常
     */
    @Test
    void schemaShouldExposeNamedKeyConstraintsAndIndexes() throws SQLException {
        try (Connection connection = migratedConnection()) {
            assertThat(readConstraintNames(connection, "dbf_api_tokens"))
                    .contains("uk_dbf_api_tokens_hash", "uk_dbf_api_tokens_user_active");
            assertThat(readConstraintNames(connection, "dbf_user_env_grants"))
                    .contains("uk_dbf_user_env_grants_user_env");
            assertThat(readConstraintNames(connection, "dbf_projects"))
                    .contains("uk_dbf_projects_project_key");
            assertThat(readConstraintNames(connection, "dbf_environments"))
                    .contains("uk_dbf_environments_project_env");

            assertThat(readIndexNames(connection, "dbf_api_tokens"))
                    .contains("idx_dbf_api_tokens_user_status");
        }
    }

    /**
     * 创建已经执行 V1 migration 的独立 H2 连接。
     *
     * @return 已迁移完成的数据库连接
     * @throws SQLException 数据库连接异常
     */
    private Connection migratedConnection() throws SQLException {
        String databaseName = "dbflow_" + UUID.randomUUID().toString().replace("-", "");
        String jdbcUrl = "jdbc:h2:mem:" + databaseName
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    /**
     * 读取指定连接中的表名集合。
     *
     * @param connection 数据库连接
     * @return 小写表名集合
     * @throws SQLException 数据库元数据读取异常
     */
    private Set<String> readTableNames(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            return resultSetValues(tables, "TABLE_NAME");
        }
    }

    /**
     * 读取指定表的列名集合。
     *
     * @param connection 数据库连接
     * @param tableName  表名
     * @return 小写列名集合
     * @throws SQLException 数据库元数据读取异常
     */
    private Set<String> readColumnNames(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
            return resultSetValues(columns, "COLUMN_NAME");
        }
    }

    /**
     * 读取指定表的索引名集合。
     *
     * @param connection 数据库连接
     * @param tableName  表名
     * @return 小写索引名集合
     * @throws SQLException 数据库元数据读取异常
     */
    private Set<String> readIndexNames(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, false, false)) {
            return resultSetValues(indexes, "INDEX_NAME");
        }
    }

    /**
     * 读取指定表的约束名集合。
     *
     * @param connection 数据库连接
     * @param tableName  表名
     * @return 小写约束名集合
     * @throws SQLException 数据库元数据读取异常
     */
    private Set<String> readConstraintNames(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT constraint_name FROM information_schema.table_constraints WHERE table_name = ?"
        )) {
            statement.setString(1, tableName);
            try (ResultSet constraints = statement.executeQuery()) {
                return resultSetValues(constraints, "CONSTRAINT_NAME");
            }
        }
    }

    /**
     * 将 ResultSet 指定列读取成小写字符串集合。
     *
     * @param resultSet   查询结果集
     * @param columnLabel 列标签
     * @return 小写字符串集合
     * @throws SQLException 结果集读取异常
     */
    private Set<String> resultSetValues(ResultSet resultSet, String columnLabel) throws SQLException {
        Set<String> values = new java.util.LinkedHashSet<>();
        while (resultSet.next()) {
            String value = resultSet.getString(columnLabel);
            if (value != null) {
                values.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return values;
    }

    /**
     * 插入测试用户。
     *
     * @param connection 数据库连接
     * @param username   用户名
     * @return 用户主键
     * @throws SQLException 数据库操作异常
     */
    private long insertUser(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dbf_users (username, display_name, status) VALUES (?, ?, 'ACTIVE')",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, username);
            statement.setString(2, username);
            statement.executeUpdate();
            return readGeneratedId(statement);
        }
    }

    /**
     * 插入测试项目环境。
     *
     * @param connection     数据库连接
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @return 环境主键
     * @throws SQLException 数据库操作异常
     */
    private long insertEnvironment(Connection connection, String projectKey, String environmentKey) throws SQLException {
        long projectId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dbf_projects (project_key, name, status) VALUES (?, ?, 'ACTIVE')",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, projectKey);
            statement.setString(2, projectKey);
            statement.executeUpdate();
            projectId = readGeneratedId(statement);
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dbf_environments (project_id, environment_key, name, status) VALUES (?, ?, ?, 'ACTIVE')",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setLong(1, projectId);
            statement.setString(2, environmentKey);
            statement.setString(3, environmentKey);
            statement.executeUpdate();
            return readGeneratedId(statement);
        }
    }

    /**
     * 插入测试 API token 元数据。
     *
     * @param connection  数据库连接
     * @param userId      用户主键
     * @param tokenHash   token hash
     * @param tokenPrefix token 前缀
     * @param status      token 状态
     * @param activeFlag  active 唯一约束标记
     * @throws SQLException 数据库操作异常
     */
    private void insertToken(
            Connection connection,
            long userId,
            String tokenHash,
            String tokenPrefix,
            String status,
            Integer activeFlag
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dbf_api_tokens (user_id, token_hash, token_prefix, status, active_flag) "
                        + "VALUES (?, ?, ?, ?, ?)"
        )) {
            statement.setLong(1, userId);
            statement.setString(2, tokenHash);
            statement.setString(3, tokenPrefix);
            statement.setString(4, status);
            if (activeFlag == null) {
                statement.setNull(5, java.sql.Types.TINYINT);
            } else {
                statement.setInt(5, activeFlag);
            }
            statement.executeUpdate();
        }
    }

    /**
     * 插入测试授权关系。
     *
     * @param connection    数据库连接
     * @param userId        用户主键
     * @param environmentId 环境主键
     * @throws SQLException 数据库操作异常
     */
    private void insertGrant(Connection connection, long userId, long environmentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dbf_user_env_grants (user_id, environment_id, grant_type, status) "
                        + "VALUES (?, ?, 'WRITE', 'ACTIVE')"
        )) {
            statement.setLong(1, userId);
            statement.setLong(2, environmentId);
            statement.executeUpdate();
        }
    }

    /**
     * 读取自增主键。
     *
     * @param statement 已执行插入的 PreparedStatement
     * @return 自增主键
     * @throws SQLException 主键读取异常
     */
    private long readGeneratedId(PreparedStatement statement) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            assertThat(generatedKeys.next()).isTrue();
            return generatedKeys.getLong(1);
        }
    }
}
