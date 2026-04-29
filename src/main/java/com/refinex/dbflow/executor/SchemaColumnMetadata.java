package com.refinex.dbflow.executor;

/**
 * 字段元数据。
 *
 * @param schemaName             schema 名称
 * @param tableName              表名
 * @param name                   字段名
 * @param ordinalPosition        字段序号
 * @param dataType               基础数据类型
 * @param columnType             完整字段类型
 * @param nullable               是否允许 null
 * @param defaultValue           默认值
 * @param comment                字段注释
 * @param columnKey              键类型
 * @param extra                  额外属性
 * @param characterMaximumLength 字符最大长度
 * @param numericPrecision       数字精度
 * @param numericScale           数字小数位
 * @author refinex
 */
public record SchemaColumnMetadata(
        String schemaName,
        String tableName,
        String name,
        Integer ordinalPosition,
        String dataType,
        String columnType,
        boolean nullable,
        String defaultValue,
        String comment,
        String columnKey,
        String extra,
        Long characterMaximumLength,
        Long numericPrecision,
        Long numericScale
) {
}
