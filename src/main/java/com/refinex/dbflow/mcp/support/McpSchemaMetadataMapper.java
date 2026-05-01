package com.refinex.dbflow.mcp.support;

import com.refinex.dbflow.executor.dto.*;

import java.util.List;
import java.util.Map;

import static com.refinex.dbflow.mcp.support.McpResponseBuilder.data;

/**
 * MCP schema、SQL 执行和 EXPLAIN 元数据响应映射器。
 *
 * @author refinex
 */
public final class McpSchemaMetadataMapper {

    /**
     * 工具类不允许实例化。
     */
    private McpSchemaMetadataMapper() {
    }

    /**
     * 转换 warning 输出数据。
     *
     * @param warnings warning 摘要
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> warningData(List<SqlExecutionWarning> warnings) {
        return warnings.stream()
                .map(warning -> data(
                        "level", warning.level(),
                        "code", warning.code(),
                        "message", warning.message()
                ))
                .toList();
    }

    /**
     * 转换 schema 输出数据。
     *
     * @param schemas schema 元数据
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> schemaData(List<SchemaDatabaseMetadata> schemas) {
        return schemas.stream()
                .map(schema -> data(
                        "name", schema.name(),
                        "defaultCharacterSetName", schema.defaultCharacterSetName(),
                        "defaultCollationName", schema.defaultCollationName()
                ))
                .toList();
    }

    /**
     * 转换表输出数据。
     *
     * @param tables 表元数据
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> tableData(List<SchemaTableMetadata> tables) {
        return tables.stream()
                .map(table -> data(
                        "schemaName", table.schemaName(),
                        "name", table.name(),
                        "type", table.type(),
                        "engine", table.engine(),
                        "rows", table.rows(),
                        "comment", table.comment()
                ))
                .toList();
    }

    /**
     * 转换字段输出数据。
     *
     * @param columns 字段元数据
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> columnData(List<SchemaColumnMetadata> columns) {
        return columns.stream()
                .map(column -> data(
                        "schemaName", column.schemaName(),
                        "tableName", column.tableName(),
                        "name", column.name(),
                        "ordinalPosition", column.ordinalPosition(),
                        "dataType", column.dataType(),
                        "columnType", column.columnType(),
                        "nullable", column.nullable(),
                        "defaultValue", column.defaultValue(),
                        "comment", column.comment(),
                        "columnKey", column.columnKey(),
                        "extra", column.extra(),
                        "characterMaximumLength", column.characterMaximumLength(),
                        "numericPrecision", column.numericPrecision(),
                        "numericScale", column.numericScale()
                ))
                .toList();
    }

    /**
     * 转换索引输出数据。
     *
     * @param indexes 索引元数据
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> indexData(List<SchemaIndexMetadata> indexes) {
        return indexes.stream()
                .map(index -> data(
                        "schemaName", index.schemaName(),
                        "tableName", index.tableName(),
                        "name", index.name(),
                        "nonUnique", index.nonUnique(),
                        "unique", index.unique(),
                        "seqInIndex", index.seqInIndex(),
                        "columnName", index.columnName(),
                        "indexType", index.indexType(),
                        "cardinality", index.cardinality(),
                        "nullable", index.nullable(),
                        "comment", index.comment(),
                        "indexComment", index.indexComment()
                ))
                .toList();
    }

    /**
     * 转换视图输出数据。
     *
     * @param views 视图元数据
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> viewData(List<SchemaViewMetadata> views) {
        return views.stream()
                .map(view -> data(
                        "schemaName", view.schemaName(),
                        "name", view.name(),
                        "checkOption", view.checkOption(),
                        "updatable", view.updatable(),
                        "securityType", view.securityType(),
                        "definition", view.definition()
                ))
                .toList();
    }

    /**
     * 转换 routine 输出数据。
     *
     * @param routines routine 元数据
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> routineData(List<SchemaRoutineMetadata> routines) {
        return routines.stream()
                .map(routine -> data(
                        "schemaName", routine.schemaName(),
                        "name", routine.name(),
                        "type", routine.type(),
                        "dataType", routine.dataType(),
                        "comment", routine.comment(),
                        "sqlDataAccess", routine.sqlDataAccess(),
                        "securityType", routine.securityType()
                ))
                .toList();
    }

    /**
     * 转换 EXPLAIN plan row 输出数据。
     *
     * @param planRows 执行计划行
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> planRowData(List<SqlExplainPlanRow> planRows) {
        return planRows.stream()
                .map(row -> data(
                        "id", row.id(),
                        "selectType", row.selectType(),
                        "table", row.table(),
                        "type", row.type(),
                        "possibleKeys", row.possibleKeys(),
                        "key", row.key(),
                        "keyLen", row.keyLen(),
                        "ref", row.ref(),
                        "rows", row.rows(),
                        "filtered", row.filtered(),
                        "extra", row.extra(),
                        "raw", row.raw()
                ))
                .toList();
    }

    /**
     * 转换 EXPLAIN 建议输出数据。
     *
     * @param advice 建议列表
     * @return MCP 响应数据
     */
    public static List<Map<String, Object>> adviceData(List<SqlExplainAdvice> advice) {
        return advice.stream()
                .map(item -> data(
                        "code", item.code(),
                        "severity", item.severity(),
                        "table", item.table(),
                        "message", item.message()
                ))
                .toList();
    }
}
