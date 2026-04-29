package com.refinex.dbflow.executor;

import com.refinex.dbflow.access.service.AccessDecision;
import com.refinex.dbflow.access.service.AccessDecisionReason;
import com.refinex.dbflow.access.service.AccessDecisionRequest;
import com.refinex.dbflow.access.service.AccessDecisionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 基于 information_schema 的受控 schema inspect 服务。
 *
 * @author refinex
 */
@Service
public class SchemaInspectService {

    /**
     * 已完成 inspect 状态。
     */
    private static final String STATUS_INSPECTED = "INSPECTED";

    /**
     * 拒绝状态。
     */
    private static final String STATUS_DENIED = "DENIED";

    /**
     * 失败状态。
     */
    private static final String STATUS_FAILED = "FAILED";

    /**
     * 默认最大条目数。
     */
    private static final int DEFAULT_MAX_ITEMS = 100;

    /**
     * 最大允许条目数。
     */
    private static final int HARD_MAX_ITEMS = 500;

    /**
     * 项目环境访问判断服务。
     */
    private final AccessDecisionService accessDecisionService;

    /**
     * 目标库数据源注册表。
     */
    private final ProjectEnvironmentDataSourceRegistry dataSourceRegistry;

    /**
     * 创建 schema inspect 服务。
     *
     * @param accessDecisionService 项目环境访问判断服务
     * @param dataSourceRegistry    目标库数据源注册表
     */
    public SchemaInspectService(
            AccessDecisionService accessDecisionService,
            ProjectEnvironmentDataSourceRegistry dataSourceRegistry
    ) {
        this.accessDecisionService = accessDecisionService;
        this.dataSourceRegistry = dataSourceRegistry;
    }

    /**
     * 执行 schema inspect。
     *
     * @param request inspect 请求
     * @return inspect 结果
     */
    public SchemaInspectResult inspect(SchemaInspectRequest request) {
        Objects.requireNonNull(request, "request");
        int maxItems = effectiveMaxItems(request.maxItems());
        AccessDecision decision = authorize(request);
        if (!decision.allowed()) {
            return denied(request, maxItems, decision.reason().name(),
                    "授权拒绝: " + decision.reason().name() + " - " + decision.message());
        }
        long started = System.nanoTime();
        try (Connection connection = dataSourceRegistry.getDataSource(request.projectKey(), request.environmentKey())
                .getConnection()) {
            String schema = effectiveSchema(connection, request.schema());
            String table = trimToNull(request.table());
            List<SchemaDatabaseMetadata> rawSchemas = querySchemas(connection, schema, maxItems + 1);
            List<SchemaTableMetadata> rawTables = queryTables(connection, schema, table, maxItems + 1);
            List<SchemaColumnMetadata> rawColumns = queryColumns(connection, schema, table, maxItems + 1);
            List<SchemaIndexMetadata> rawIndexes = queryIndexes(connection, schema, table, maxItems + 1);
            List<SchemaViewMetadata> rawViews = queryViews(connection, schema, table, maxItems + 1);
            List<SchemaRoutineMetadata> rawRoutines = queryRoutines(connection, schema, maxItems + 1);
            boolean truncated = isTruncated(rawSchemas, rawTables, rawColumns, rawIndexes, rawViews, rawRoutines,
                    maxItems);
            return new SchemaInspectResult(
                    request.projectKey(),
                    request.environmentKey(),
                    true,
                    STATUS_INSPECTED,
                    schema,
                    table,
                    maxItems,
                    truncated,
                    bounded(rawSchemas, maxItems),
                    bounded(rawTables, maxItems),
                    bounded(rawColumns, maxItems),
                    bounded(rawIndexes, maxItems),
                    bounded(rawViews, maxItems),
                    bounded(rawRoutines, maxItems),
                    elapsedMillis(started),
                    null,
                    null
            );
        } catch (SQLException exception) {
            return new SchemaInspectResult(
                    request.projectKey(),
                    request.environmentKey(),
                    false,
                    STATUS_FAILED,
                    trimToNull(request.schema()),
                    trimToNull(request.table()),
                    maxItems,
                    false,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    elapsedMillis(started),
                    exception.getSQLState(),
                    sanitize(exception)
            );
        }
    }

    /**
     * 执行授权检查。
     *
     * @param request inspect 请求
     * @return 授权结果
     */
    private AccessDecision authorize(SchemaInspectRequest request) {
        if (request.userId() == null || request.tokenId() == null) {
            return AccessDecision.deny(AccessDecisionReason.TOKEN_NOT_FOUND, "MCP Token 上下文缺失");
        }
        return accessDecisionService.decide(new AccessDecisionRequest(
                request.userId(),
                request.tokenId(),
                request.projectKey(),
                request.environmentKey()
        ));
    }

    /**
     * 查询 schema 元数据。
     *
     * @param connection 连接
     * @param schema     schema 过滤
     * @param limit      返回上限
     * @return schema 元数据
     * @throws SQLException 查询失败时抛出
     */
    private List<SchemaDatabaseMetadata> querySchemas(Connection connection, String schema, int limit)
            throws SQLException {
        String sql = """
                SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
                FROM INFORMATION_SCHEMA.SCHEMATA
                WHERE SCHEMA_NAME = ?
                ORDER BY SCHEMA_NAME
                LIMIT ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SchemaDatabaseMetadata> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new SchemaDatabaseMetadata(
                            resultSet.getString("SCHEMA_NAME"),
                            resultSet.getString("DEFAULT_CHARACTER_SET_NAME"),
                            resultSet.getString("DEFAULT_COLLATION_NAME")
                    ));
                }
                return rows;
            }
        }
    }

    /**
     * 查询表元数据。
     *
     * @param connection 连接
     * @param schema     schema 过滤
     * @param table      table 过滤
     * @param limit      返回上限
     * @return 表元数据
     * @throws SQLException 查询失败时抛出
     */
    private List<SchemaTableMetadata> queryTables(Connection connection, String schema, String table, int limit)
            throws SQLException {
        String sql = """
                SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE, ENGINE, TABLE_ROWS, TABLE_COMMENT
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = ?
                %s
                ORDER BY TABLE_SCHEMA, TABLE_NAME
                LIMIT ?
                """.formatted(StringUtils.hasText(table) ? "AND TABLE_NAME = ?" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSchemaTableLimit(statement, schema, table, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SchemaTableMetadata> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new SchemaTableMetadata(
                            resultSet.getString("TABLE_SCHEMA"),
                            resultSet.getString("TABLE_NAME"),
                            resultSet.getString("TABLE_TYPE"),
                            resultSet.getString("ENGINE"),
                            longValue(resultSet, "TABLE_ROWS"),
                            resultSet.getString("TABLE_COMMENT")
                    ));
                }
                return rows;
            }
        }
    }

    /**
     * 查询字段元数据。
     *
     * @param connection 连接
     * @param schema     schema 过滤
     * @param table      table 过滤
     * @param limit      返回上限
     * @return 字段元数据
     * @throws SQLException 查询失败时抛出
     */
    private List<SchemaColumnMetadata> queryColumns(Connection connection, String schema, String table, int limit)
            throws SQLException {
        String sql = """
                SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION, DATA_TYPE, COLUMN_TYPE,
                       IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT, COLUMN_KEY, EXTRA,
                       CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = ?
                %s
                ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION
                LIMIT ?
                """.formatted(StringUtils.hasText(table) ? "AND TABLE_NAME = ?" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSchemaTableLimit(statement, schema, table, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SchemaColumnMetadata> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new SchemaColumnMetadata(
                            resultSet.getString("TABLE_SCHEMA"),
                            resultSet.getString("TABLE_NAME"),
                            resultSet.getString("COLUMN_NAME"),
                            intValue(resultSet, "ORDINAL_POSITION"),
                            resultSet.getString("DATA_TYPE"),
                            resultSet.getString("COLUMN_TYPE"),
                            "YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE")),
                            resultSet.getString("COLUMN_DEFAULT"),
                            resultSet.getString("COLUMN_COMMENT"),
                            resultSet.getString("COLUMN_KEY"),
                            resultSet.getString("EXTRA"),
                            longValue(resultSet, "CHARACTER_MAXIMUM_LENGTH"),
                            longValue(resultSet, "NUMERIC_PRECISION"),
                            longValue(resultSet, "NUMERIC_SCALE")
                    ));
                }
                return rows;
            }
        }
    }

    /**
     * 查询索引元数据。
     *
     * @param connection 连接
     * @param schema     schema 过滤
     * @param table      table 过滤
     * @param limit      返回上限
     * @return 索引元数据
     * @throws SQLException 查询失败时抛出
     */
    private List<SchemaIndexMetadata> queryIndexes(Connection connection, String schema, String table, int limit)
            throws SQLException {
        String sql = """
                SELECT TABLE_SCHEMA, TABLE_NAME, NON_UNIQUE, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME,
                       INDEX_TYPE, CARDINALITY, NULLABLE, COMMENT, INDEX_COMMENT
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = ?
                %s
                ORDER BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX
                LIMIT ?
                """.formatted(StringUtils.hasText(table) ? "AND TABLE_NAME = ?" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSchemaTableLimit(statement, schema, table, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SchemaIndexMetadata> rows = new ArrayList<>();
                while (resultSet.next()) {
                    boolean nonUnique = intValue(resultSet, "NON_UNIQUE") == 1;
                    rows.add(new SchemaIndexMetadata(
                            resultSet.getString("TABLE_SCHEMA"),
                            resultSet.getString("TABLE_NAME"),
                            resultSet.getString("INDEX_NAME"),
                            nonUnique,
                            !nonUnique,
                            intValue(resultSet, "SEQ_IN_INDEX"),
                            resultSet.getString("COLUMN_NAME"),
                            resultSet.getString("INDEX_TYPE"),
                            longValue(resultSet, "CARDINALITY"),
                            resultSet.getString("NULLABLE"),
                            resultSet.getString("COMMENT"),
                            resultSet.getString("INDEX_COMMENT")
                    ));
                }
                return rows;
            }
        }
    }

    /**
     * 查询视图元数据。
     *
     * @param connection 连接
     * @param schema     schema 过滤
     * @param table      table 过滤
     * @param limit      返回上限
     * @return 视图元数据
     * @throws SQLException 查询失败时抛出
     */
    private List<SchemaViewMetadata> queryViews(Connection connection, String schema, String table, int limit)
            throws SQLException {
        String sql = """
                SELECT TABLE_SCHEMA, TABLE_NAME, VIEW_DEFINITION, CHECK_OPTION, IS_UPDATABLE, SECURITY_TYPE
                FROM INFORMATION_SCHEMA.VIEWS
                WHERE TABLE_SCHEMA = ?
                %s
                ORDER BY TABLE_SCHEMA, TABLE_NAME
                LIMIT ?
                """.formatted(StringUtils.hasText(table) ? "AND TABLE_NAME = ?" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSchemaTableLimit(statement, schema, table, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SchemaViewMetadata> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new SchemaViewMetadata(
                            resultSet.getString("TABLE_SCHEMA"),
                            resultSet.getString("TABLE_NAME"),
                            resultSet.getString("CHECK_OPTION"),
                            "YES".equalsIgnoreCase(resultSet.getString("IS_UPDATABLE")),
                            resultSet.getString("SECURITY_TYPE"),
                            resultSet.getString("VIEW_DEFINITION")
                    ));
                }
                return rows;
            }
        }
    }

    /**
     * 查询 routine 元数据。
     *
     * @param connection 连接
     * @param schema     schema 过滤
     * @param limit      返回上限
     * @return routine 元数据
     * @throws SQLException 查询失败时抛出
     */
    private List<SchemaRoutineMetadata> queryRoutines(Connection connection, String schema, int limit)
            throws SQLException {
        String sql = """
                SELECT ROUTINE_SCHEMA, ROUTINE_NAME, ROUTINE_TYPE, DATA_TYPE, ROUTINE_COMMENT,
                       SQL_DATA_ACCESS, SECURITY_TYPE
                FROM INFORMATION_SCHEMA.ROUTINES
                WHERE ROUTINE_SCHEMA = ?
                ORDER BY ROUTINE_SCHEMA, ROUTINE_TYPE, ROUTINE_NAME
                LIMIT ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SchemaRoutineMetadata> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new SchemaRoutineMetadata(
                            resultSet.getString("ROUTINE_SCHEMA"),
                            resultSet.getString("ROUTINE_NAME"),
                            resultSet.getString("ROUTINE_TYPE"),
                            resultSet.getString("DATA_TYPE"),
                            resultSet.getString("ROUTINE_COMMENT"),
                            resultSet.getString("SQL_DATA_ACCESS"),
                            resultSet.getString("SECURITY_TYPE")
                    ));
                }
                return rows;
            }
        }
    }

    /**
     * 创建拒绝结果。
     *
     * @param request      inspect 请求
     * @param maxItems     最大条目数
     * @param errorCode    错误码
     * @param errorMessage 错误摘要
     * @return 拒绝结果
     */
    private SchemaInspectResult denied(
            SchemaInspectRequest request,
            int maxItems,
            String errorCode,
            String errorMessage
    ) {
        return new SchemaInspectResult(
                request.projectKey(),
                request.environmentKey(),
                false,
                STATUS_DENIED,
                trimToNull(request.schema()),
                trimToNull(request.table()),
                maxItems,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0L,
                errorCode,
                errorMessage
        );
    }

    /**
     * 绑定 schema、table 和 limit 参数。
     *
     * @param statement prepared statement
     * @param schema    schema 过滤
     * @param table     table 过滤
     * @param limit     返回上限
     * @throws SQLException 绑定失败时抛出
     */
    private void bindSchemaTableLimit(PreparedStatement statement, String schema, String table, int limit)
            throws SQLException {
        int index = 1;
        statement.setString(index++, schema);
        if (StringUtils.hasText(table)) {
            statement.setString(index++, table);
        }
        statement.setInt(index, limit);
    }

    /**
     * 计算有效 schema。
     *
     * @param connection 连接
     * @param schema     请求 schema
     * @return 有效 schema
     * @throws SQLException 读取连接 catalog 失败时抛出
     */
    private String effectiveSchema(Connection connection, String schema) throws SQLException {
        String requestedSchema = trimToNull(schema);
        if (StringUtils.hasText(requestedSchema)) {
            return requestedSchema;
        }
        String catalog = trimToNull(connection.getCatalog());
        if (StringUtils.hasText(catalog)) {
            return catalog;
        }
        throw new SQLException("未指定 schema，且目标连接没有默认 catalog");
    }

    /**
     * 计算有效最大条目数。
     *
     * @param maxItems 请求最大条目数
     * @return 有效最大条目数
     */
    private int effectiveMaxItems(int maxItems) {
        if (maxItems <= 0) {
            return DEFAULT_MAX_ITEMS;
        }
        return Math.min(maxItems, HARD_MAX_ITEMS);
    }

    /**
     * 截断列表。
     *
     * @param rows     原始列表
     * @param maxItems 最大条目数
     * @param <T>      元数据类型
     * @return 截断后列表
     */
    private <T> List<T> bounded(List<T> rows, int maxItems) {
        if (rows.size() <= maxItems) {
            return List.copyOf(rows);
        }
        return List.copyOf(rows.subList(0, maxItems));
    }

    /**
     * 判断是否发生截断。
     *
     * @param schemas  schema 元数据
     * @param tables   表元数据
     * @param columns  字段元数据
     * @param indexes  索引元数据
     * @param views    视图元数据
     * @param routines routine 元数据
     * @param maxItems 最大条目数
     * @return 截断时返回 true
     */
    private boolean isTruncated(
            List<?> schemas,
            List<?> tables,
            List<?> columns,
            List<?> indexes,
            List<?> views,
            List<?> routines,
            int maxItems
    ) {
        return schemas.size() > maxItems
                || tables.size() > maxItems
                || columns.size() > maxItems
                || indexes.size() > maxItems
                || views.size() > maxItems
                || routines.size() > maxItems;
    }

    /**
     * 读取可空整数。
     *
     * @param resultSet 结果集
     * @param column    列名
     * @return 整数值
     * @throws SQLException 读取失败时抛出
     */
    private Integer intValue(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    /**
     * 读取可空长整数。
     *
     * @param resultSet 结果集
     * @param column    列名
     * @return 长整数值
     * @throws SQLException 读取失败时抛出
     */
    private Long longValue(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    /**
     * 去除空白并把空字符串转为 null。
     *
     * @param value 原始值
     * @return 标准化值
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.strip() : null;
    }

    /**
     * 计算耗时毫秒。
     *
     * @param startedNanos 开始时间
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    /**
     * 清理异常摘要。
     *
     * @param exception SQL 异常
     * @return 错误摘要
     */
    private String sanitize(SQLException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return exception.getClass().getSimpleName();
        }
        String singleLine = message.replaceAll("\\s+", " ").strip();
        return singleLine.length() <= 240 ? singleLine : singleLine.substring(0, 240);
    }
}
