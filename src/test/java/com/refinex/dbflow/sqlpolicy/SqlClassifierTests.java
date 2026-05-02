package com.refinex.dbflow.sqlpolicy;

import com.refinex.dbflow.sqlpolicy.dto.SqlClassification;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlParseStatus;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;
import com.refinex.dbflow.sqlpolicy.model.SqlStatementType;
import com.refinex.dbflow.sqlpolicy.service.SqlClassifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL 解析与风险分类服务测试。
 *
 * @author refinex
 */
class SqlClassifierTests {

    /**
     * 被测 SQL 分类器。
     */
    private final SqlClassifier classifier = new SqlClassifier();

    /**
     * 验证 MySQL 8 查询语法会被分类为低风险只读语句。
     */
    @Test
    void shouldClassifyMysql8SelectAsLowRiskRead() {
        SqlClassification classification = classifier.classify(
                "SELECT id, JSON_EXTRACT(payload, '$.user.id') AS user_id FROM audit_events "
                        + "WHERE created_at >= NOW() - INTERVAL 1 DAY");

        assertThat(classification.parseStatus()).isEqualTo(SqlParseStatus.SUCCESS);
        assertThat(classification.statementType()).isEqualTo(SqlStatementType.QUERY);
        assertThat(classification.operation()).isEqualTo(SqlOperation.SELECT);
        assertThat(classification.riskLevel()).isEqualTo(SqlRiskLevel.LOW);
        assertThat(classification.targetTable()).isEqualTo("audit_events");
        assertThat(classification.isDdl()).isFalse();
        assertThat(classification.isDml()).isFalse();
    }

    /**
     * 验证 MySQL 5.7 常见查询辅助语句会被明确分类。
     */
    @Test
    void shouldClassifyMysql57InspectionStatements() {
        assertReadable("SHOW FULL COLUMNS FROM orders", SqlOperation.SHOW, "orders");
        assertReadable("DESCRIBE orders", SqlOperation.DESCRIBE, "orders");
        assertReadable("EXPLAIN SELECT * FROM orders WHERE id = 1", SqlOperation.EXPLAIN, "orders");
    }

    /**
     * 验证 DML 语句会被分类为写操作，并提取目标表。
     */
    @Test
    void shouldClassifyDmlStatements() {
        assertDml("INSERT INTO sales.orders(id, status) VALUES (1, 'NEW')",
                SqlOperation.INSERT, "sales", "orders", SqlRiskLevel.MEDIUM);
        assertDml("UPDATE sales.orders SET status = 'PAID' WHERE id = 1",
                SqlOperation.UPDATE, "sales", "orders", SqlRiskLevel.MEDIUM);
        assertDml("DELETE FROM sales.orders WHERE id = 1",
                SqlOperation.DELETE, "sales", "orders", SqlRiskLevel.HIGH);

        SqlClassification loadData = classifier.classify(
                "LOAD DATA LOCAL INFILE '/tmp/orders.csv' INTO TABLE sales.orders");
        assertThat(loadData.parseStatus()).isEqualTo(SqlParseStatus.PARSE_FAILED);
        assertThat(loadData.statementType()).isEqualTo(SqlStatementType.DML);
        assertThat(loadData.operation()).isEqualTo(SqlOperation.LOAD_DATA);
        assertThat(loadData.riskLevel()).isEqualTo(SqlRiskLevel.REJECTED);
        assertThat(loadData.targetSchema()).isEqualTo("sales");
        assertThat(loadData.targetTable()).isEqualTo("orders");
        assertThat(loadData.isDml()).isTrue();
        assertThat(loadData.rejectedByDefault()).isTrue();
    }

    /**
     * 验证 DDL 和权限语句会被分类为高危或致命风险。
     */
    @Test
    void shouldClassifyDdlAndAdminStatements() {
        assertDdl("CREATE TABLE sales.orders_archive (id BIGINT PRIMARY KEY)",
                SqlOperation.CREATE, "sales", "orders_archive", SqlRiskLevel.HIGH);
        assertDdl("CREATE FUNCTION get_product_sku_by_name(p_product_name VARCHAR(255)) "
                        + "RETURNS VARCHAR(64) NOT DETERMINISTIC READS SQL DATA "
                        + "RETURN (SELECT sku FROM products WHERE name = p_product_name LIMIT 1)",
                SqlOperation.CREATE, null, "get_product_sku_by_name", SqlRiskLevel.HIGH);
        assertDdl("CREATE PROCEDURE p_refresh_product_stats() SELECT 1",
                SqlOperation.CREATE, null, "p_refresh_product_stats", SqlRiskLevel.HIGH);
        assertDdl("ALTER TABLE sales.orders ADD COLUMN note VARCHAR(255)",
                SqlOperation.ALTER, "sales", "orders", SqlRiskLevel.HIGH);
        assertDdl("DROP TABLE sales.orders_archive",
                SqlOperation.DROP_TABLE, "sales", "orders_archive", SqlRiskLevel.CRITICAL);
        assertDdl("TRUNCATE TABLE sales.orders",
                SqlOperation.TRUNCATE, "sales", "orders", SqlRiskLevel.CRITICAL);

        SqlClassification dropDatabase = classifier.classify("DROP DATABASE sales");
        assertThat(dropDatabase.parseStatus()).isEqualTo(SqlParseStatus.PARSE_FAILED);
        assertThat(dropDatabase.statementType()).isEqualTo(SqlStatementType.DDL);
        assertThat(dropDatabase.operation()).isEqualTo(SqlOperation.DROP_DATABASE);
        assertThat(dropDatabase.riskLevel()).isEqualTo(SqlRiskLevel.REJECTED);
        assertThat(dropDatabase.targetSchema()).isEqualTo("sales");
        assertThat(dropDatabase.targetTable()).isNull();
        assertThat(dropDatabase.isDdl()).isTrue();
        assertThat(dropDatabase.rejectedByDefault()).isTrue();

        SqlClassification grant = classifier.classify("GRANT SELECT ON sales.orders TO 'app'@'%'");
        assertThat(grant.parseStatus()).isEqualTo(SqlParseStatus.PARSE_FAILED);
        assertThat(grant.statementType()).isEqualTo(SqlStatementType.ADMIN);
        assertThat(grant.operation()).isEqualTo(SqlOperation.GRANT);
        assertThat(grant.riskLevel()).isEqualTo(SqlRiskLevel.REJECTED);
        assertThat(grant.targetSchema()).isEqualTo("sales");
        assertThat(grant.targetTable()).isEqualTo("orders");
        assertThat(grant.rejectedByDefault()).isTrue();
    }

    /**
     * 验证多语句默认拒绝。
     */
    @Test
    void shouldRejectMultipleStatementsByDefault() {
        SqlClassification classification = classifier.classify("SELECT 1; DELETE FROM orders WHERE id = 1");

        assertThat(classification.parseStatus()).isEqualTo(SqlParseStatus.MULTI_STATEMENT_REJECTED);
        assertThat(classification.statementType()).isEqualTo(SqlStatementType.UNKNOWN);
        assertThat(classification.operation()).isEqualTo(SqlOperation.UNKNOWN);
        assertThat(classification.riskLevel()).isEqualTo(SqlRiskLevel.REJECTED);
        assertThat(classification.isDdl()).isFalse();
        assertThat(classification.isDml()).isFalse();
    }

    /**
     * 验证解析失败的 DDL/DML 默认拒绝，避免危险 SQL 因解析失败被放行。
     */
    @Test
    void shouldFailClosedForFailedDdlOrDmlParsing() {
        SqlClassification dml = classifier.classify("UPDATE orders SET status = 'PAID' WHERE");
        assertThat(dml.parseStatus()).isEqualTo(SqlParseStatus.PARSE_FAILED);
        assertThat(dml.operation()).isEqualTo(SqlOperation.UPDATE);
        assertThat(dml.riskLevel()).isEqualTo(SqlRiskLevel.REJECTED);
        assertThat(dml.isDml()).isTrue();

        SqlClassification ddl = classifier.classify("ALTER TABLE orders ADD COLUMN");
        assertThat(ddl.parseStatus()).isEqualTo(SqlParseStatus.PARSE_FAILED);
        assertThat(ddl.operation()).isEqualTo(SqlOperation.ALTER);
        assertThat(ddl.riskLevel()).isEqualTo(SqlRiskLevel.REJECTED);
        assertThat(ddl.isDdl()).isTrue();
    }

    /**
     * 验证解析失败的可读语句仍按明确只读命令分类，但保留解析失败状态供审计。
     */
    @Test
    void shouldExplicitlyClassifyReadableStatementsWhenParserFails() {
        SqlClassification classification = classifier.classify("SELECT FROM");

        assertThat(classification.parseStatus()).isEqualTo(SqlParseStatus.PARSE_FAILED);
        assertThat(classification.statementType()).isEqualTo(SqlStatementType.QUERY);
        assertThat(classification.operation()).isEqualTo(SqlOperation.SELECT);
        assertThat(classification.riskLevel()).isEqualTo(SqlRiskLevel.LOW);
    }

    /**
     * 断言只读检查语句。
     *
     * @param sql       SQL 文本
     * @param operation 预期操作
     * @param table     预期目标表
     */
    private void assertReadable(String sql, SqlOperation operation, String table) {
        SqlClassification classification = classifier.classify(sql);
        assertThat(classification.statementType()).isEqualTo(SqlStatementType.QUERY);
        assertThat(classification.operation()).isEqualTo(operation);
        assertThat(classification.riskLevel()).isEqualTo(SqlRiskLevel.LOW);
        assertThat(classification.targetTable()).isEqualTo(table);
        assertThat(classification.isDdl()).isFalse();
        assertThat(classification.isDml()).isFalse();
    }

    /**
     * 断言 DML 语句。
     *
     * @param sql       SQL 文本
     * @param operation 预期操作
     * @param schema    预期目标 schema
     * @param table     预期目标表
     * @param riskLevel 预期风险等级
     */
    private void assertDml(
            String sql,
            SqlOperation operation,
            String schema,
            String table,
            SqlRiskLevel riskLevel) {
        SqlClassification classification = classifier.classify(sql);
        assertThat(classification.parseStatus()).isEqualTo(SqlParseStatus.SUCCESS);
        assertThat(classification.statementType()).isEqualTo(SqlStatementType.DML);
        assertThat(classification.operation()).isEqualTo(operation);
        assertThat(classification.riskLevel()).isEqualTo(riskLevel);
        assertThat(classification.targetSchema()).isEqualTo(schema);
        assertThat(classification.targetTable()).isEqualTo(table);
        assertThat(classification.isDdl()).isFalse();
        assertThat(classification.isDml()).isTrue();
    }

    /**
     * 断言 DDL 语句。
     *
     * @param sql       SQL 文本
     * @param operation 预期操作
     * @param schema    预期目标 schema
     * @param table     预期目标表
     * @param riskLevel 预期风险等级
     */
    private void assertDdl(
            String sql,
            SqlOperation operation,
            String schema,
            String table,
            SqlRiskLevel riskLevel) {
        SqlClassification classification = classifier.classify(sql);
        assertThat(classification.parseStatus()).isEqualTo(SqlParseStatus.SUCCESS);
        assertThat(classification.statementType()).isEqualTo(SqlStatementType.DDL);
        assertThat(classification.operation()).isEqualTo(operation);
        assertThat(classification.riskLevel()).isEqualTo(riskLevel);
        assertThat(classification.targetSchema()).isEqualTo(schema);
        assertThat(classification.targetTable()).isEqualTo(table);
        assertThat(classification.isDdl()).isTrue();
        assertThat(classification.isDml()).isFalse();
    }
}
