package com.refinex.dbflow.executor.dto;

/**
 * 表或视图基础元数据。
 *
 * @param schemaName schema 名称
 * @param name       表名
 * @param type       表类型
 * @param engine     存储引擎
 * @param rows       估算行数
 * @param comment    表注释
 * @author refinex
 */
public record SchemaTableMetadata(
        String schemaName,
        String name,
        String type,
        String engine,
        Long rows,
        String comment
) {
}
