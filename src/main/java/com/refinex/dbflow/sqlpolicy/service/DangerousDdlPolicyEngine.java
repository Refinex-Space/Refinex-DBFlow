package com.refinex.dbflow.sqlpolicy.service;

import com.refinex.dbflow.config.model.DangerousDdlOperation;
import com.refinex.dbflow.config.properties.DbflowProperties;
import com.refinex.dbflow.sqlpolicy.dto.DangerousDdlPolicyDecision;
import com.refinex.dbflow.sqlpolicy.dto.SqlClassification;
import com.refinex.dbflow.sqlpolicy.model.DangerousDdlPolicyReasonCode;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * DROP DATABASE 和 DROP TABLE 高危 DDL 白名单策略引擎。
 *
 * @author refinex
 */
@Service
public class DangerousDdlPolicyEngine {

    /**
     * 星号通配符。
     */
    private static final String WILDCARD = "*";

    /**
     * DBFlow 配置属性。
     */
    private final DbflowProperties dbflowProperties;

    /**
     * 创建高危 DDL 策略引擎。
     *
     * @param dbflowProperties DBFlow 配置属性
     */
    public DangerousDdlPolicyEngine(DbflowProperties dbflowProperties) {
        this.dbflowProperties = Objects.requireNonNull(dbflowProperties, "dbflowProperties");
    }

    /**
     * 根据项目、环境和 SQL 分类结果判定 DROP 高危 DDL 是否允许继续。
     *
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param classification SQL 分类结果
     * @return 高危 DDL 策略判定结果
     */
    public DangerousDdlPolicyDecision decide(
            String projectKey,
            String environmentKey,
            SqlClassification classification) {
        Objects.requireNonNull(classification, "classification");
        DangerousDdlOperation operation = toDangerousDdlOperation(classification.operation());
        if (operation == null) {
            return decision(projectKey, environmentKey, classification, true,
                    DangerousDdlPolicyReasonCode.NOT_APPLICABLE, "非 DROP DATABASE 或 DROP TABLE 操作，不适用高危 DROP 白名单",
                    false);
        }
        if (!hasRequiredTarget(operation, classification)) {
            return decision(projectKey, environmentKey, classification, false,
                    DangerousDdlPolicyReasonCode.MISSING_TARGET, "DROP 操作缺少 schema 或 table 目标，默认拒绝",
                    false);
        }
        if (classification.rejectedByDefault() || classification.riskLevel() == SqlRiskLevel.REJECTED) {
            return decision(projectKey, environmentKey, classification, false,
                    DangerousDdlPolicyReasonCode.CLASSIFICATION_REJECTED,
                    "SQL 分类阶段已默认拒绝该 DROP 操作，白名单策略不覆盖解析拒绝结果",
                    false);
        }

        Optional<DbflowProperties.WhitelistEntry> matchedEntry = dbflowProperties.getPolicies()
                .getDangerousDdl()
                .getWhitelist()
                .stream()
                .filter(entry -> matches(entry, projectKey, environmentKey, classification, operation))
                .findFirst();
        if (matchedEntry.isEmpty()) {
            return decision(projectKey, environmentKey, classification, false,
                    DangerousDdlPolicyReasonCode.DEFAULT_DENY, "DROP 高危 DDL 未命中 YAML 白名单，默认拒绝",
                    false);
        }
        if (isProdEnvironment(environmentKey) && !matchedEntry.get().isAllowProdDangerousDdl()) {
            return decision(projectKey, environmentKey, classification, false,
                    DangerousDdlPolicyReasonCode.PROD_REQUIRES_EXPLICIT_ALLOW,
                    "prod 环境命中白名单，但未显式配置 allow-prod-dangerous-ddl=true，默认拒绝",
                    true);
        }
        return decision(projectKey, environmentKey, classification, true,
                DangerousDdlPolicyReasonCode.WHITELIST_MATCH, "DROP 高危 DDL 命中 YAML 白名单",
                true);
    }

    /**
     * 将 SQL 操作映射为高危 DDL 操作。
     *
     * @param operation SQL 操作
     * @return 高危 DDL 操作；不适用时返回 null
     */
    private DangerousDdlOperation toDangerousDdlOperation(SqlOperation operation) {
        return switch (operation) {
            case DROP_TABLE -> DangerousDdlOperation.DROP_TABLE;
            case DROP_DATABASE -> DangerousDdlOperation.DROP_DATABASE;
            default -> null;
        };
    }

    /**
     * 判断分类结果是否包含策略判定所需目标信息。
     *
     * @param operation      高危 DDL 操作
     * @param classification SQL 分类结果
     * @return 目标信息完整时返回 true
     */
    private boolean hasRequiredTarget(DangerousDdlOperation operation, SqlClassification classification) {
        if (!StringUtils.hasText(classification.targetSchema())) {
            return false;
        }
        return operation != DangerousDdlOperation.DROP_TABLE || StringUtils.hasText(classification.targetTable());
    }

    /**
     * 判断白名单条目是否匹配当前请求。
     *
     * @param entry          白名单条目
     * @param projectKey     项目标识
     * @param environmentKey 环境标识
     * @param classification SQL 分类结果
     * @param operation      高危 DDL 操作
     * @return 匹配时返回 true
     */
    private boolean matches(
            DbflowProperties.WhitelistEntry entry,
            String projectKey,
            String environmentKey,
            SqlClassification classification,
            DangerousDdlOperation operation) {
        if (entry.getOperation() != operation) {
            return false;
        }
        if (!matchesField(entry.getProjectKey(), projectKey)
                || !matchesField(entry.getEnvironmentKey(), environmentKey)
                || !matchesField(entry.getSchemaName(), classification.targetSchema())) {
            return false;
        }
        return operation == DangerousDdlOperation.DROP_DATABASE
                || matchesField(entry.getTableName(), classification.targetTable());
    }

    /**
     * 判断配置字段是否匹配目标字段。
     *
     * @param pattern 配置字段
     * @param value   目标字段
     * @return 匹配时返回 true
     */
    private boolean matchesField(String pattern, String value) {
        return WILDCARD.equals(pattern) || Objects.equals(pattern, value);
    }

    /**
     * 判断环境是否为生产环境。
     *
     * @param environmentKey 环境标识
     * @return 生产环境返回 true
     */
    private boolean isProdEnvironment(String environmentKey) {
        return "prod".equalsIgnoreCase(environmentKey) || "production".equalsIgnoreCase(environmentKey);
    }

    /**
     * 创建策略判定结果。
     *
     * @param projectKey       项目标识
     * @param environmentKey   环境标识
     * @param classification   SQL 分类结果
     * @param allowed          是否允许
     * @param reasonCode       原因码
     * @param reason           原因说明
     * @param matchedWhitelist 是否命中白名单
     * @return 策略判定结果
     */
    private DangerousDdlPolicyDecision decision(
            String projectKey,
            String environmentKey,
            SqlClassification classification,
            boolean allowed,
            DangerousDdlPolicyReasonCode reasonCode,
            String reason,
            boolean matchedWhitelist) {
        return new DangerousDdlPolicyDecision(
                projectKey,
                environmentKey,
                classification.operation(),
                classification.targetSchema(),
                classification.targetTable(),
                allowed,
                reasonCode,
                reason,
                matchedWhitelist,
                true);
    }
}
