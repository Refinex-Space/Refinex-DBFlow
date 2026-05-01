package com.refinex.dbflow.executor.support;

import com.refinex.dbflow.executor.dto.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * information_schema 元数据行映射器。
 *
 * @author refinex
 */
public final class SchemaMetadataMapper {

    /**
     * 工具类不允许实例化。
     */
    private SchemaMetadataMapper() {
    }

    /**
     * 映射 schema 元数据。
     *
     * @param resultSet 结果集
     * @return schema 元数据
     * @throws SQLException 读取失败时抛出
     */
    public static SchemaDatabaseMetadata database(ResultSet resultSet) throws SQLException {
        return new SchemaDatabaseMetadata(
                resultSet.getString("SCHEMA_NAME"),
                resultSet.getString("DEFAULT_CHARACTER_SET_NAME"),
                resultSet.getString("DEFAULT_COLLATION_NAME")
        );
    }

    /**
     * 映射表元数据。
     *
     * @param resultSet 结果集
     * @return 表元数据
     * @throws SQLException 读取失败时抛出
     */
    public static SchemaTableMetadata table(ResultSet resultSet) throws SQLException {
        return new SchemaTableMetadata(
                resultSet.getString("TABLE_SCHEMA"),
                resultSet.getString("TABLE_NAME"),
                resultSet.getString("TABLE_TYPE"),
                resultSet.getString("ENGINE"),
                longValue(resultSet, "TABLE_ROWS"),
                resultSet.getString("TABLE_COMMENT")
        );
    }

    /**
     * 映射字段元数据。
     *
     * @param resultSet 结果集
     * @return 字段元数据
     * @throws SQLException 读取失败时抛出
     */
    public static SchemaColumnMetadata column(ResultSet resultSet) throws SQLException {
        return new SchemaColumnMetadata(
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
        );
    }

    /**
     * 映射索引元数据。
     *
     * @param resultSet 结果集
     * @return 索引元数据
     * @throws SQLException 读取失败时抛出
     */
    public static SchemaIndexMetadata index(ResultSet resultSet) throws SQLException {
        boolean nonUnique = intValue(resultSet, "NON_UNIQUE") == 1;
        return new SchemaIndexMetadata(
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
        );
    }

    /**
     * 映射视图元数据。
     *
     * @param resultSet 结果集
     * @return 视图元数据
     * @throws SQLException 读取失败时抛出
     */
    public static SchemaViewMetadata view(ResultSet resultSet) throws SQLException {
        return new SchemaViewMetadata(
                resultSet.getString("TABLE_SCHEMA"),
                resultSet.getString("TABLE_NAME"),
                resultSet.getString("CHECK_OPTION"),
                "YES".equalsIgnoreCase(resultSet.getString("IS_UPDATABLE")),
                resultSet.getString("SECURITY_TYPE"),
                resultSet.getString("VIEW_DEFINITION")
        );
    }

    /**
     * 映射 routine 元数据。
     *
     * @param resultSet 结果集
     * @return routine 元数据
     * @throws SQLException 读取失败时抛出
     */
    public static SchemaRoutineMetadata routine(ResultSet resultSet) throws SQLException {
        return new SchemaRoutineMetadata(
                resultSet.getString("ROUTINE_SCHEMA"),
                resultSet.getString("ROUTINE_NAME"),
                resultSet.getString("ROUTINE_TYPE"),
                resultSet.getString("DATA_TYPE"),
                resultSet.getString("ROUTINE_COMMENT"),
                resultSet.getString("SQL_DATA_ACCESS"),
                resultSet.getString("SECURITY_TYPE")
        );
    }

    /**
     * 截断列表。
     *
     * @param rows     原始列表
     * @param maxItems 最大条目数
     * @param <T>      元数据类型
     * @return 截断后列表
     */
    public static <T> List<T> bounded(List<T> rows, int maxItems) {
        if (rows.size() <= maxItems) {
            return List.copyOf(rows);
        }
        return List.copyOf(rows.subList(0, maxItems));
    }

    /**
     * 判断是否发生截断。
     *
     * @param maxItems 最大条目数
     * @param lists    待检查列表
     * @return 任一列表超过最大条目数时返回 true
     */
    public static boolean isTruncated(int maxItems, List<?>... lists) {
        for (List<?> list : lists) {
            if (list.size() > maxItems) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取可空整数。
     *
     * @param resultSet 结果集
     * @param column    列名
     * @return 整数值
     * @throws SQLException 读取失败时抛出
     */
    private static Integer intValue(ResultSet resultSet, String column) throws SQLException {
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
    private static Long longValue(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
