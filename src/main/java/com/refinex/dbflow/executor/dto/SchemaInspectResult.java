package com.refinex.dbflow.executor.dto;

import java.util.List;

/**
 * schema inspect 结果。
 *
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param allowed        是否允许 inspect
 * @param status         结果状态
 * @param schemaFilter   schema 过滤
 * @param tableFilter    table 过滤
 * @param maxItems       每类元数据最大条目数
 * @param truncated      是否截断
 * @param schemas        schema 元数据
 * @param tables         表元数据
 * @param columns        字段元数据
 * @param indexes        索引元数据
 * @param views          视图元数据
 * @param routines       routine 元数据
 * @param durationMillis 执行耗时，单位毫秒
 * @param errorCode      错误码
 * @param errorMessage   错误摘要
 * @author refinex
 */
public record SchemaInspectResult(
        String projectKey,
        String environmentKey,
        boolean allowed,
        String status,
        String schemaFilter,
        String tableFilter,
        int maxItems,
        boolean truncated,
        List<SchemaDatabaseMetadata> schemas,
        List<SchemaTableMetadata> tables,
        List<SchemaColumnMetadata> columns,
        List<SchemaIndexMetadata> indexes,
        List<SchemaViewMetadata> views,
        List<SchemaRoutineMetadata> routines,
        long durationMillis,
        String errorCode,
        String errorMessage
) {
}
