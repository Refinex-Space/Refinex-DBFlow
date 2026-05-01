package com.refinex.dbflow.executor.dto;

/**
 * 存储过程或函数元数据。
 *
 * @param schemaName    schema 名称
 * @param name          routine 名称
 * @param type          routine 类型
 * @param dataType      返回数据类型
 * @param comment       routine 注释
 * @param sqlDataAccess SQL 数据访问类型
 * @param securityType  安全类型
 * @author refinex
 */
public record SchemaRoutineMetadata(
        String schemaName,
        String name,
        String type,
        String dataType,
        String comment,
        String sqlDataAccess,
        String securityType
) {
}
