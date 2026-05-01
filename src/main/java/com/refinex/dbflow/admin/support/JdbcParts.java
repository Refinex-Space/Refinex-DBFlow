package com.refinex.dbflow.admin.support;

/**
 * JDBC URL 安全展示字段。
 *
 * @param type   数据库类型
 * @param host   主机
 * @param port   端口
 * @param schema 数据库或 schema
 * @author refinex
 */
public record JdbcParts(String type, String host, String port, String schema) {
}
