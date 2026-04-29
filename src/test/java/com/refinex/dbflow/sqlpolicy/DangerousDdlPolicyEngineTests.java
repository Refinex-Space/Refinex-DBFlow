package com.refinex.dbflow.sqlpolicy;

import com.refinex.dbflow.config.DangerousDdlOperation;
import com.refinex.dbflow.config.DbflowProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DROP DATABASE 和 DROP TABLE 高危 DDL YAML 白名单策略测试。
 *
 * @author refinex
 */
class DangerousDdlPolicyEngineTests {

    /**
     * 创建带白名单的 DBFlow 配置。
     *
     * @param entries 白名单条目
     * @return DBFlow 配置
     */
    private static DbflowProperties propertiesWithWhitelist(DbflowProperties.WhitelistEntry... entries) {
        DbflowProperties properties = new DbflowProperties();
        properties.getPolicies().getDangerousDdl().getWhitelist().addAll(java.util.List.of(entries));
        properties.afterPropertiesSet();
        return properties;
    }

    /**
     * 创建白名单条目。
     *
     * @param projectKey            项目标识
     * @param environmentKey        环境标识
     * @param schemaName            schema 名称
     * @param tableName             表名
     * @param operation             高危 DDL 操作
     * @param allowProdDangerousDdl 是否允许生产高危 DDL
     * @return 白名单条目
     */
    private static DbflowProperties.WhitelistEntry whitelist(
            String projectKey,
            String environmentKey,
            String schemaName,
            String tableName,
            DangerousDdlOperation operation,
            boolean allowProdDangerousDdl) {
        DbflowProperties.WhitelistEntry entry = new DbflowProperties.WhitelistEntry();
        entry.setProjectKey(projectKey);
        entry.setEnvironmentKey(environmentKey);
        entry.setSchemaName(schemaName);
        entry.setTableName(tableName);
        entry.setOperation(operation);
        entry.setAllowProdDangerousDdl(allowProdDangerousDdl);
        return entry;
    }

    /**
     * 创建 DROP TABLE 分类结果。
     *
     * @param schema schema 名称
     * @param table  表名
     * @return SQL 分类结果
     */
    private static SqlClassification dropTable(String schema, String table) {
        return new SqlClassification(
                SqlStatementType.DDL,
                SqlOperation.DROP_TABLE,
                SqlRiskLevel.CRITICAL,
                schema,
                table,
                true,
                false,
                SqlParseStatus.SUCCESS,
                false,
                "DROP TABLE 删除表");
    }

    /**
     * 创建 DROP DATABASE 分类结果。
     *
     * @param schema schema 名称
     * @return SQL 分类结果
     */
    private static SqlClassification dropDatabase(String schema) {
        return new SqlClassification(
                SqlStatementType.DDL,
                SqlOperation.DROP_DATABASE,
                SqlRiskLevel.CRITICAL,
                schema,
                null,
                true,
                false,
                SqlParseStatus.SUCCESS,
                false,
                "DROP DATABASE 删除库");
    }

    /**
     * 创建已被分类阶段默认拒绝的 DROP TABLE 分类结果。
     *
     * @param schema schema 名称
     * @param table  表名
     * @return SQL 分类结果
     */
    private static SqlClassification rejectedDropTable(String schema, String table) {
        return new SqlClassification(
                SqlStatementType.DDL,
                SqlOperation.DROP_TABLE,
                SqlRiskLevel.REJECTED,
                schema,
                table,
                true,
                false,
                SqlParseStatus.PARSE_FAILED,
                true,
                "DROP TABLE 解析失败，默认拒绝");
    }

    /**
     * 验证未命中白名单时 DROP TABLE 默认拒绝，并返回可审计原因。
     */
    @Test
    void shouldDenyDropTableWhenWhitelistDoesNotMatch() {
        DangerousDdlPolicyEngine engine = new DangerousDdlPolicyEngine(new DbflowProperties());

        DangerousDdlPolicyDecision decision = engine.decide("demo", "dev", dropTable("sales", "orders"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo(DangerousDdlPolicyReasonCode.DEFAULT_DENY);
        assertThat(decision.reason()).contains("未命中");
        assertThat(decision.matchedWhitelist()).isFalse();
        assertThat(decision.auditRequired()).isTrue();
    }

    /**
     * 验证精确白名单命中时非生产 DROP TABLE 允许继续。
     */
    @Test
    void shouldAllowDropTableWhenExactWhitelistMatches() {
        DbflowProperties properties = propertiesWithWhitelist(
                whitelist("demo", "dev", "sales", "orders", DangerousDdlOperation.DROP_TABLE, false));
        DangerousDdlPolicyEngine engine = new DangerousDdlPolicyEngine(properties);

        DangerousDdlPolicyDecision decision = engine.decide("demo", "dev", dropTable("sales", "orders"));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reasonCode()).isEqualTo(DangerousDdlPolicyReasonCode.WHITELIST_MATCH);
        assertThat(decision.matchedWhitelist()).isTrue();
        assertThat(decision.auditRequired()).isTrue();
    }

    /**
     * 验证精确白名单命中时非生产 DROP DATABASE 允许继续。
     */
    @Test
    void shouldAllowDropDatabaseWhenExactWhitelistMatches() {
        DbflowProperties properties = propertiesWithWhitelist(
                whitelist("demo", "dev", "sales", null, DangerousDdlOperation.DROP_DATABASE, false));
        DangerousDdlPolicyEngine engine = new DangerousDdlPolicyEngine(properties);

        DangerousDdlPolicyDecision decision = engine.decide("demo", "dev", dropDatabase("sales"));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reasonCode()).isEqualTo(DangerousDdlPolicyReasonCode.WHITELIST_MATCH);
        assertThat(decision.matchedWhitelist()).isTrue();
        assertThat(decision.auditRequired()).isTrue();
    }

    /**
     * 验证生产环境即使命中白名单，也必须显式开启生产高危 DDL 允许标记。
     */
    @Test
    void shouldDenyProdDropWhenWhitelistLacksExplicitProdAllow() {
        DbflowProperties properties = propertiesWithWhitelist(
                whitelist("demo", "prod", "sales", "orders", DangerousDdlOperation.DROP_TABLE, false));
        DangerousDdlPolicyEngine engine = new DangerousDdlPolicyEngine(properties);

        DangerousDdlPolicyDecision decision = engine.decide("demo", "prod", dropTable("sales", "orders"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo(DangerousDdlPolicyReasonCode.PROD_REQUIRES_EXPLICIT_ALLOW);
        assertThat(decision.reason()).contains("prod");
        assertThat(decision.matchedWhitelist()).isTrue();
        assertThat(decision.auditRequired()).isTrue();
    }

    /**
     * 验证生产环境命中白名单并显式开启生产允许标记时允许继续。
     */
    @Test
    void shouldAllowProdDropWhenWhitelistHasExplicitProdAllow() {
        DbflowProperties properties = propertiesWithWhitelist(
                whitelist("demo", "prod", "sales", "orders", DangerousDdlOperation.DROP_TABLE, true));
        DangerousDdlPolicyEngine engine = new DangerousDdlPolicyEngine(properties);

        DangerousDdlPolicyDecision decision = engine.decide("demo", "prod", dropTable("sales", "orders"));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reasonCode()).isEqualTo(DangerousDdlPolicyReasonCode.WHITELIST_MATCH);
        assertThat(decision.matchedWhitelist()).isTrue();
        assertThat(decision.auditRequired()).isTrue();
    }

    /**
     * 验证白名单不能覆盖 SQL 分类阶段的默认拒绝结果。
     */
    @Test
    void shouldDenyWhenClassificationWasRejectedByDefault() {
        DbflowProperties properties = propertiesWithWhitelist(
                whitelist("demo", "dev", "sales", "orders", DangerousDdlOperation.DROP_TABLE, false));
        DangerousDdlPolicyEngine engine = new DangerousDdlPolicyEngine(properties);

        DangerousDdlPolicyDecision decision = engine.decide("demo", "dev", rejectedDropTable("sales", "orders"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo(DangerousDdlPolicyReasonCode.CLASSIFICATION_REJECTED);
        assertThat(decision.reason()).contains("分类阶段");
        assertThat(decision.matchedWhitelist()).isFalse();
        assertThat(decision.auditRequired()).isTrue();
    }

    /**
     * 验证 project、environment、schema、table 支持星号通配匹配。
     */
    @Test
    void shouldMatchWildcardWhitelistFields() {
        DbflowProperties properties = propertiesWithWhitelist(
                whitelist("*", "*", "*", "*", DangerousDdlOperation.DROP_TABLE, false));
        DangerousDdlPolicyEngine engine = new DangerousDdlPolicyEngine(properties);

        DangerousDdlPolicyDecision decision = engine.decide("demo", "dev", dropTable("sales", "orders"));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reasonCode()).isEqualTo(DangerousDdlPolicyReasonCode.WHITELIST_MATCH);
        assertThat(decision.matchedWhitelist()).isTrue();
        assertThat(decision.auditRequired()).isTrue();
    }
}
