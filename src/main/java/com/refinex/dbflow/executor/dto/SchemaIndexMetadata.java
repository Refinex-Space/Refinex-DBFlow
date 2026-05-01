package com.refinex.dbflow.executor.dto;

/**
 * 索引字段元数据。
 *
 * @param schemaName   schema 名称
 * @param tableName    表名
 * @param name         索引名
 * @param nonUnique    是否非唯一
 * @param unique       是否唯一
 * @param seqInIndex   索引字段序号
 * @param columnName   字段名
 * @param indexType    索引类型
 * @param cardinality  基数估算
 * @param nullable     索引字段是否可 null
 * @param comment      索引注释
 * @param indexComment 索引级注释
 * @author refinex
 */
public record SchemaIndexMetadata(
        String schemaName,
        String tableName,
        String name,
        boolean nonUnique,
        boolean unique,
        Integer seqInIndex,
        String columnName,
        String indexType,
        Long cardinality,
        String nullable,
        String comment,
        String indexComment
) {
}
