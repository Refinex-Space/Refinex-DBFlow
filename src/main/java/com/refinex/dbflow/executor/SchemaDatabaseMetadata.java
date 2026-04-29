package com.refinex.dbflow.executor;

/**
 * schema 元数据。
 *
 * @param name                    schema 名称
 * @param defaultCharacterSetName 默认字符集
 * @param defaultCollationName    默认排序规则
 * @author refinex
 */
public record SchemaDatabaseMetadata(
        String name,
        String defaultCharacterSetName,
        String defaultCollationName
) {
}
