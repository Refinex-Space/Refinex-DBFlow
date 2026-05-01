package com.refinex.dbflow.executor.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 标准化 SQL 执行计划行。
 *
 * @param id           查询块标识
 * @param selectType   查询类型
 * @param table        表名
 * @param type         访问类型
 * @param possibleKeys 可能使用的索引
 * @param key          实际使用的索引
 * @param keyLen       索引长度
 * @param ref          索引引用列
 * @param rows         预估扫描行数
 * @param filtered     过滤比例
 * @param extra        额外信息
 * @param raw          原始行数据
 * @author refinex
 */
public record SqlExplainPlanRow(
        Integer id,
        String selectType,
        String table,
        String type,
        String possibleKeys,
        String key,
        String keyLen,
        String ref,
        Long rows,
        BigDecimal filtered,
        String extra,
        Map<String, Object> raw
) {
}
