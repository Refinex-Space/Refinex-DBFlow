package com.refinex.dbflow.executor;

/**
 * 视图元数据。
 *
 * @param schemaName   schema 名称
 * @param name         视图名
 * @param checkOption  检查选项
 * @param updatable    是否可更新
 * @param securityType 安全类型
 * @param definition   视图定义
 * @author refinex
 */
public record SchemaViewMetadata(
        String schemaName,
        String name,
        String checkOption,
        boolean updatable,
        String securityType,
        String definition
) {
}
