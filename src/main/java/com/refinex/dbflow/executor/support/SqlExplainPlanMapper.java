package com.refinex.dbflow.executor.support;

import com.refinex.dbflow.executor.dto.SqlExplainPlanRow;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * SQL EXPLAIN 传统格式结果映射器。
 *
 * @author refinex
 */
public final class SqlExplainPlanMapper {

    /**
     * 工具类不允许实例化。
     */
    private SqlExplainPlanMapper() {
    }

    /**
     * 将 EXPLAIN 结果集映射为标准化计划行。
     *
     * @param resultSet EXPLAIN 结果集
     * @return 标准化执行计划行
     * @throws SQLException 读取失败时抛出
     */
    public static List<SqlExplainPlanRow> mapRows(ResultSet resultSet) throws SQLException {
        List<SqlExplainPlanRow> rows = new ArrayList<>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        List<String> columns = columns(metadata);
        while (resultSet.next()) {
            rows.add(planRow(resultSet, columns));
        }
        return List.copyOf(rows);
    }

    /**
     * 创建标准化执行计划行。
     *
     * @param resultSet 结果集
     * @param columns   结果列
     * @return 标准化执行计划行
     * @throws SQLException 读取失败时抛出
     */
    private static SqlExplainPlanRow planRow(ResultSet resultSet, List<String> columns) throws SQLException {
        Map<String, Object> raw = rawRow(resultSet, columns);
        return new SqlExplainPlanRow(
                integerValue(raw, "id"),
                stringValue(raw, "select_type"),
                stringValue(raw, "table"),
                stringValue(raw, "type"),
                stringValue(raw, "possible_keys"),
                stringValue(raw, "key"),
                stringValue(raw, "key_len"),
                stringValue(raw, "ref"),
                longValue(raw, "rows"),
                decimalValue(raw, "filtered"),
                stringValue(raw, "extra"),
                raw
        );
    }

    /**
     * 读取结果集列名。
     *
     * @param metadata 结果集元数据
     * @return 列名列表
     * @throws SQLException 读取失败时抛出
     */
    private static List<String> columns(ResultSetMetaData metadata) throws SQLException {
        List<String> columns = new ArrayList<>();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            columns.add(metadata.getColumnLabel(index));
        }
        return List.copyOf(columns);
    }

    /**
     * 读取原始行数据。
     *
     * @param resultSet 结果集
     * @param columns   列名
     * @return 原始行数据
     * @throws SQLException 读取失败时抛出
     */
    private static Map<String, Object> rawRow(ResultSet resultSet, List<String> columns) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < columns.size(); index++) {
            row.put(columns.get(index), resultSet.getObject(index + 1));
        }
        return Collections.unmodifiableMap(row);
    }

    /**
     * 按忽略大小写的 key 读取值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 字段值
     */
    private static Object rawValue(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 读取字符串值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 字符串值
     */
    private static String stringValue(Map<String, Object> row, String key) {
        Object value = rawValue(row, key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : null;
    }

    /**
     * 读取整数值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 整数值
     */
    private static Integer integerValue(Map<String, Object> row, String key) {
        Long value = longValue(row, key);
        return value == null ? null : Math.toIntExact(value);
    }

    /**
     * 读取长整数值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 长整数值
     */
    private static Long longValue(Map<String, Object> row, String key) {
        Object value = rawValue(row, key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return Long.parseLong(String.valueOf(value));
        }
        return null;
    }

    /**
     * 读取小数值。
     *
     * @param row 原始行
     * @param key 字段名
     * @return 小数值
     */
    private static BigDecimal decimalValue(Map<String, Object> row, String key) {
        Object value = rawValue(row, key);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return new BigDecimal(String.valueOf(value));
        }
        return null;
    }
}
