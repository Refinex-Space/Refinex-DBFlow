package com.refinex.dbflow.sqlpolicy;

/**
 * SQL 操作类型。
 *
 * @author refinex
 */
public enum SqlOperation {

    /**
     * SELECT 查询。
     */
    SELECT,

    /**
     * SHOW 检查。
     */
    SHOW,

    /**
     * DESCRIBE 检查。
     */
    DESCRIBE,

    /**
     * EXPLAIN 检查。
     */
    EXPLAIN,

    /**
     * INSERT 写入。
     */
    INSERT,

    /**
     * UPDATE 更新。
     */
    UPDATE,

    /**
     * DELETE 删除。
     */
    DELETE,

    /**
     * LOAD DATA 批量导入。
     */
    LOAD_DATA,

    /**
     * CREATE 结构创建。
     */
    CREATE,

    /**
     * ALTER 结构变更。
     */
    ALTER,

    /**
     * DROP TABLE 表删除。
     */
    DROP_TABLE,

    /**
     * DROP DATABASE 库删除。
     */
    DROP_DATABASE,

    /**
     * 其它 DROP 操作。
     */
    DROP,

    /**
     * TRUNCATE 清空。
     */
    TRUNCATE,

    /**
     * GRANT 授权。
     */
    GRANT,

    /**
     * 未知操作。
     */
    UNKNOWN
}
