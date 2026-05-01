package com.refinex.dbflow.sqlpolicy.service;

import com.refinex.dbflow.sqlpolicy.dto.SqlClassification;
import com.refinex.dbflow.sqlpolicy.model.SqlOperation;
import com.refinex.dbflow.sqlpolicy.model.SqlParseStatus;
import com.refinex.dbflow.sqlpolicy.model.SqlRiskLevel;
import com.refinex.dbflow.sqlpolicy.model.SqlStatementType;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 JSQLParser 的 SQL 解析与风险分类服务。
 *
 * @author refinex
 */
@Service
public class SqlClassifier {

    /**
     * LOAD DATA 目标表提取表达式。
     */
    private static final Pattern LOAD_DATA_TARGET_PATTERN = Pattern.compile("(?is)\\bINTO\\s+TABLE\\s+(`?\\w+`?(?:\\.`?\\w+`?)?)");

    /**
     * GRANT 目标对象提取表达式。
     */
    private static final Pattern GRANT_TARGET_PATTERN = Pattern.compile("(?is)\\bON\\s+(`?\\w+`?(?:\\.`?\\w+`?)?|\\*)\\s+TO\\b");

    /**
     * SHOW 目标表提取表达式。
     */
    private static final Pattern SHOW_TARGET_PATTERN = Pattern.compile("(?is)\\bFROM\\s+(`?\\w+`?(?:\\.`?\\w+`?)?)");

    /**
     * DROP DATABASE 目标库提取表达式。
     */
    private static final Pattern DROP_DATABASE_PATTERN = Pattern.compile("(?is)^\\s*DROP\\s+(?:DATABASE|SCHEMA)\\s+(?:IF\\s+EXISTS\\s+)?(`?\\w+`?)");

    /**
     * 通用首个对象提取表达式。
     */
    private static final Pattern FIRST_OBJECT_PATTERN = Pattern.compile("(?is)^\\s*\\w+(?:\\s+\\w+)?\\s+(?:IF\\s+(?:NOT\\s+)?EXISTS\\s+)?(`?\\w+`?(?:\\.`?\\w+`?)?)");

    /**
     * 对 SQL 文本进行解析和风险分类。
     *
     * @param sql SQL 文本
     * @return SQL 分类结果
     */
    public SqlClassification classify(String sql) {
        if (!StringUtils.hasText(sql)) {
            return rejectedUnknown(SqlParseStatus.PARSE_FAILED, "SQL 为空");
        }

        String normalizedSql = sql.strip();
        if (hasMultipleStatements(normalizedSql)) {
            return new SqlClassification(
                    SqlStatementType.UNKNOWN,
                    SqlOperation.UNKNOWN,
                    SqlRiskLevel.REJECTED,
                    null,
                    null,
                    false,
                    false,
                    SqlParseStatus.MULTI_STATEMENT_REJECTED,
                    true,
                    "检测到多语句，默认拒绝");
        }

        try {
            Statements statements = CCJSqlParserUtil.parseStatements(normalizedSql);
            if (statements.size() > 1) {
                return new SqlClassification(
                        SqlStatementType.UNKNOWN,
                        SqlOperation.UNKNOWN,
                        SqlRiskLevel.REJECTED,
                        null,
                        null,
                        false,
                        false,
                        SqlParseStatus.MULTI_STATEMENT_REJECTED,
                        true,
                        "JSQLParser 检测到多语句，默认拒绝");
            }
            if (statements.isEmpty()) {
                return rejectedUnknown(SqlParseStatus.PARSE_FAILED, "未解析到 SQL 语句");
            }
            return classifyParsedStatement(statements.getFirst(), normalizedSql);
        } catch (JSQLParserException exception) {
            return classifyParseFailure(normalizedSql);
        }
    }

    /**
     * 分类已解析语句。
     *
     * @param statement 已解析语句
     * @param sql       SQL 文本
     * @return SQL 分类结果
     */
    private SqlClassification classifyParsedStatement(Statement statement, String sql) {
        Objects.requireNonNull(statement, "statement");
        return switch (statement) {
            case Select select -> query(SqlOperation.SELECT, firstTable(select), SqlParseStatus.SUCCESS, "SELECT 查询");
            case ShowColumnsStatement showColumnsStatement ->
                    query(SqlOperation.SHOW, tableName(showColumnsStatement.getTableName()), SqlParseStatus.SUCCESS,
                            "SHOW 检查");
            case DescribeStatement describeStatement ->
                    query(SqlOperation.DESCRIBE, tableName(describeStatement.getTable()), SqlParseStatus.SUCCESS,
                            "DESCRIBE 检查");
            case ExplainStatement explainStatement ->
                    query(SqlOperation.EXPLAIN, explainTarget(explainStatement), SqlParseStatus.SUCCESS,
                            "EXPLAIN 检查");
            case UnsupportedStatement unsupportedStatement -> classifyUnsupportedStatement(sql);
            case Insert insert -> dml(SqlOperation.INSERT, tableName(insert.getTable()), SqlRiskLevel.MEDIUM,
                    SqlParseStatus.SUCCESS, false, "INSERT 写入");
            case Update update -> dml(SqlOperation.UPDATE, tableName(update.getTable()), SqlRiskLevel.MEDIUM,
                    SqlParseStatus.SUCCESS, false, "UPDATE 更新");
            case Delete delete -> dml(SqlOperation.DELETE, tableName(delete.getTable()), SqlRiskLevel.HIGH,
                    SqlParseStatus.SUCCESS, false, "DELETE 删除");
            case CreateTable createTable ->
                    ddl(SqlOperation.CREATE, tableName(createTable.getTable()), SqlRiskLevel.HIGH,
                            SqlParseStatus.SUCCESS, false, "CREATE TABLE 结构创建");
            case Alter alter -> ddl(SqlOperation.ALTER, tableName(alter.getTable()), SqlRiskLevel.HIGH,
                    SqlParseStatus.SUCCESS, false, "ALTER TABLE 结构变更");
            case Drop drop -> classifyDrop(drop);
            case Truncate truncate -> ddl(SqlOperation.TRUNCATE, tableName(truncate.getTable()), SqlRiskLevel.CRITICAL,
                    SqlParseStatus.SUCCESS, false, "TRUNCATE 清空表");
            case Grant grant -> admin(SqlOperation.GRANT, tableName(grant.getObjectName()), SqlRiskLevel.CRITICAL,
                    SqlParseStatus.SUCCESS, false, "GRANT 授权");
            default -> classifyParseFailure(sql);
        };
    }

    /**
     * 分类 JSQLParser 不支持但已保留文本的语句。
     *
     * @param sql SQL 文本
     * @return SQL 分类结果
     */
    private SqlClassification classifyUnsupportedStatement(String sql) {
        String firstKeyword = firstKeyword(sql);
        if ("SHOW".equals(firstKeyword)) {
            return query(SqlOperation.SHOW, showTarget(sql), SqlParseStatus.SUCCESS, "SHOW 检查");
        }
        return classifyParseFailure(sql);
    }

    /**
     * 分类解析失败语句。
     *
     * @param sql SQL 文本
     * @return SQL 分类结果
     */
    private SqlClassification classifyParseFailure(String sql) {
        String firstKeyword = firstKeyword(sql);
        return switch (firstKeyword) {
            case "SELECT" ->
                    query(SqlOperation.SELECT, null, SqlParseStatus.PARSE_FAILED, "SELECT 解析失败但为只读命令");
            case "SHOW" -> query(SqlOperation.SHOW, showTarget(sql), SqlParseStatus.PARSE_FAILED,
                    "SHOW 解析失败但为只读命令");
            case "DESC", "DESCRIBE" -> query(SqlOperation.DESCRIBE, fallbackObject(sql), SqlParseStatus.PARSE_FAILED,
                    "DESCRIBE 解析失败但为只读命令");
            case "EXPLAIN" -> query(SqlOperation.EXPLAIN, fallbackObject(sql), SqlParseStatus.PARSE_FAILED,
                    "EXPLAIN 解析失败但为只读命令");
            case "INSERT" -> dml(SqlOperation.INSERT, fallbackObject(sql), SqlRiskLevel.REJECTED,
                    SqlParseStatus.PARSE_FAILED, true, "INSERT 解析失败，默认拒绝");
            case "UPDATE" -> dml(SqlOperation.UPDATE, fallbackObject(sql), SqlRiskLevel.REJECTED,
                    SqlParseStatus.PARSE_FAILED, true, "UPDATE 解析失败，默认拒绝");
            case "DELETE" -> dml(SqlOperation.DELETE, fallbackObject(sql), SqlRiskLevel.REJECTED,
                    SqlParseStatus.PARSE_FAILED, true, "DELETE 解析失败，默认拒绝");
            case "LOAD" -> dml(SqlOperation.LOAD_DATA, loadDataTarget(sql), SqlRiskLevel.REJECTED,
                    SqlParseStatus.PARSE_FAILED, true, "LOAD DATA 解析失败，默认拒绝");
            case "CREATE" -> ddl(SqlOperation.CREATE, fallbackObject(sql), SqlRiskLevel.REJECTED,
                    SqlParseStatus.PARSE_FAILED, true, "CREATE 解析失败，默认拒绝");
            case "ALTER" -> ddl(SqlOperation.ALTER, fallbackObject(sql), SqlRiskLevel.REJECTED,
                    SqlParseStatus.PARSE_FAILED, true, "ALTER 解析失败，默认拒绝");
            case "DROP" -> classifyFailedDrop(sql);
            case "TRUNCATE" -> ddl(SqlOperation.TRUNCATE, fallbackObject(sql), SqlRiskLevel.REJECTED,
                    SqlParseStatus.PARSE_FAILED, true, "TRUNCATE 解析失败，默认拒绝");
            case "GRANT" -> admin(SqlOperation.GRANT, grantTarget(sql), SqlRiskLevel.REJECTED,
                    SqlParseStatus.PARSE_FAILED, true, "GRANT 解析失败，默认拒绝");
            default -> rejectedUnknown(SqlParseStatus.PARSE_FAILED, "未知 SQL 解析失败，默认拒绝");
        };
    }

    /**
     * 分类已解析 DROP 语句。
     *
     * @param drop DROP 语句
     * @return SQL 分类结果
     */
    private SqlClassification classifyDrop(Drop drop) {
        String type = normalize(drop.getType());
        if ("TABLE".equals(type) || "TEMPORARY TABLE".equals(type)) {
            return ddl(SqlOperation.DROP_TABLE, tableName(drop.getName()), SqlRiskLevel.CRITICAL,
                    SqlParseStatus.SUCCESS, false, "DROP TABLE 删除表");
        }
        if ("DATABASE".equals(type) || "SCHEMA".equals(type)) {
            return ddl(SqlOperation.DROP_DATABASE, schemaOnly(drop.getName()), SqlRiskLevel.CRITICAL,
                    SqlParseStatus.SUCCESS, false, "DROP DATABASE 删除库");
        }
        return ddl(SqlOperation.DROP, tableName(drop.getName()), SqlRiskLevel.CRITICAL,
                SqlParseStatus.SUCCESS, false, "DROP 操作");
    }

    /**
     * 分类解析失败的 DROP 语句。
     *
     * @param sql SQL 文本
     * @return SQL 分类结果
     */
    private SqlClassification classifyFailedDrop(String sql) {
        Matcher matcher = DROP_DATABASE_PATTERN.matcher(sql);
        if (matcher.find()) {
            return ddl(SqlOperation.DROP_DATABASE, new SqlTarget(cleanIdentifier(matcher.group(1)), null),
                    SqlRiskLevel.REJECTED, SqlParseStatus.PARSE_FAILED, true, "DROP DATABASE 解析失败，默认拒绝");
        }
        return ddl(SqlOperation.DROP, fallbackObject(sql), SqlRiskLevel.REJECTED,
                SqlParseStatus.PARSE_FAILED, true, "DROP 解析失败，默认拒绝");
    }

    /**
     * 构建查询分类结果。
     *
     * @param operation   操作类型
     * @param target      目标对象
     * @param parseStatus 解析状态
     * @param auditReason 审计原因
     * @return SQL 分类结果
     */
    private SqlClassification query(
            SqlOperation operation,
            SqlTarget target,
            SqlParseStatus parseStatus,
            String auditReason) {
        SqlTarget effectiveTarget = emptyIfNull(target);
        return new SqlClassification(
                SqlStatementType.QUERY,
                operation,
                SqlRiskLevel.LOW,
                effectiveTarget.schema(),
                effectiveTarget.table(),
                false,
                false,
                parseStatus,
                false,
                auditReason);
    }

    /**
     * 构建 DML 分类结果。
     *
     * @param operation         操作类型
     * @param target            目标对象
     * @param riskLevel         风险等级
     * @param parseStatus       解析状态
     * @param rejectedByDefault 是否默认拒绝
     * @param auditReason       审计原因
     * @return SQL 分类结果
     */
    private SqlClassification dml(
            SqlOperation operation,
            SqlTarget target,
            SqlRiskLevel riskLevel,
            SqlParseStatus parseStatus,
            boolean rejectedByDefault,
            String auditReason) {
        SqlTarget effectiveTarget = emptyIfNull(target);
        return new SqlClassification(
                SqlStatementType.DML,
                operation,
                riskLevel,
                effectiveTarget.schema(),
                effectiveTarget.table(),
                false,
                true,
                parseStatus,
                rejectedByDefault,
                auditReason);
    }

    /**
     * 构建 DDL 分类结果。
     *
     * @param operation         操作类型
     * @param target            目标对象
     * @param riskLevel         风险等级
     * @param parseStatus       解析状态
     * @param rejectedByDefault 是否默认拒绝
     * @param auditReason       审计原因
     * @return SQL 分类结果
     */
    private SqlClassification ddl(
            SqlOperation operation,
            SqlTarget target,
            SqlRiskLevel riskLevel,
            SqlParseStatus parseStatus,
            boolean rejectedByDefault,
            String auditReason) {
        SqlTarget effectiveTarget = emptyIfNull(target);
        return new SqlClassification(
                SqlStatementType.DDL,
                operation,
                riskLevel,
                effectiveTarget.schema(),
                effectiveTarget.table(),
                true,
                false,
                parseStatus,
                rejectedByDefault,
                auditReason);
    }

    /**
     * 构建管理语句分类结果。
     *
     * @param operation         操作类型
     * @param target            目标对象
     * @param riskLevel         风险等级
     * @param parseStatus       解析状态
     * @param rejectedByDefault 是否默认拒绝
     * @param auditReason       审计原因
     * @return SQL 分类结果
     */
    private SqlClassification admin(
            SqlOperation operation,
            SqlTarget target,
            SqlRiskLevel riskLevel,
            SqlParseStatus parseStatus,
            boolean rejectedByDefault,
            String auditReason) {
        SqlTarget effectiveTarget = emptyIfNull(target);
        return new SqlClassification(
                SqlStatementType.ADMIN,
                operation,
                riskLevel,
                effectiveTarget.schema(),
                effectiveTarget.table(),
                false,
                false,
                parseStatus,
                rejectedByDefault,
                auditReason);
    }

    /**
     * 构建未知拒绝结果。
     *
     * @param parseStatus 解析状态
     * @param auditReason 审计原因
     * @return SQL 分类结果
     */
    private SqlClassification rejectedUnknown(SqlParseStatus parseStatus, String auditReason) {
        return new SqlClassification(
                SqlStatementType.UNKNOWN,
                SqlOperation.UNKNOWN,
                SqlRiskLevel.REJECTED,
                null,
                null,
                false,
                false,
                parseStatus,
                true,
                auditReason);
    }

    /**
     * 获取 EXPLAIN 目标对象。
     *
     * @param explainStatement EXPLAIN 语句
     * @return 目标对象
     */
    private SqlTarget explainTarget(ExplainStatement explainStatement) {
        if (explainStatement.getTable() != null) {
            return tableName(explainStatement.getTable());
        }
        if (explainStatement.getStatement() != null) {
            return firstTable(explainStatement.getStatement());
        }
        return SqlTarget.EMPTY;
    }

    /**
     * 将空目标对象转换为显式空值对象。
     *
     * @param target 原始目标对象
     * @return 非空目标对象
     */
    private SqlTarget emptyIfNull(SqlTarget target) {
        return target == null ? SqlTarget.EMPTY : target;
    }

    /**
     * 获取 SELECT 中第一个表。
     *
     * @param statement SQL 语句
     * @return 目标对象
     */
    private SqlTarget firstTable(Statement statement) {
        Set<String> tables = new TablesNamesFinder<>().getTables(statement);
        if (tables.isEmpty()) {
            return SqlTarget.EMPTY;
        }
        return tableName(tables.iterator().next());
    }

    /**
     * 从 JSQLParser 表对象提取目标对象。
     *
     * @param table 表对象
     * @return 目标对象
     */
    private SqlTarget tableName(Table table) {
        if (table == null) {
            return SqlTarget.EMPTY;
        }
        return new SqlTarget(cleanIdentifier(table.getUnquotedSchemaName()), cleanIdentifier(table.getUnquotedName()));
    }

    /**
     * 从表名文本提取目标对象。
     *
     * @param value 表名文本
     * @return 目标对象
     */
    private SqlTarget tableName(String value) {
        if (!StringUtils.hasText(value)) {
            return SqlTarget.EMPTY;
        }
        String cleaned = cleanIdentifier(value);
        if (!StringUtils.hasText(cleaned)) {
            return SqlTarget.EMPTY;
        }
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex < 0) {
            return new SqlTarget(null, cleaned);
        }
        return new SqlTarget(cleanIdentifier(cleaned.substring(0, dotIndex)),
                cleanIdentifier(cleaned.substring(dotIndex + 1)));
    }

    /**
     * 从 DROP DATABASE 的表对象中提取 schema。
     *
     * @param table 表对象
     * @return 目标对象
     */
    private SqlTarget schemaOnly(Table table) {
        if (table == null) {
            return SqlTarget.EMPTY;
        }
        String schema = cleanIdentifier(table.getUnquotedName());
        return new SqlTarget(schema, null);
    }

    /**
     * 提取 LOAD DATA 目标对象。
     *
     * @param sql SQL 文本
     * @return 目标对象
     */
    private SqlTarget loadDataTarget(String sql) {
        Matcher matcher = LOAD_DATA_TARGET_PATTERN.matcher(sql);
        if (matcher.find()) {
            return tableName(matcher.group(1));
        }
        return SqlTarget.EMPTY;
    }

    /**
     * 提取 GRANT 目标对象。
     *
     * @param sql SQL 文本
     * @return 目标对象
     */
    private SqlTarget grantTarget(String sql) {
        Matcher matcher = GRANT_TARGET_PATTERN.matcher(sql);
        if (matcher.find()) {
            return tableName(matcher.group(1));
        }
        return SqlTarget.EMPTY;
    }

    /**
     * 提取 SHOW 目标对象。
     *
     * @param sql SQL 文本
     * @return 目标对象
     */
    private SqlTarget showTarget(String sql) {
        Matcher matcher = SHOW_TARGET_PATTERN.matcher(sql);
        if (matcher.find()) {
            return tableName(matcher.group(1));
        }
        return SqlTarget.EMPTY;
    }

    /**
     * 通过常见对象位置提取目标对象。
     *
     * @param sql SQL 文本
     * @return 目标对象
     */
    private SqlTarget fallbackObject(String sql) {
        Matcher matcher = FIRST_OBJECT_PATTERN.matcher(sql);
        if (matcher.find()) {
            return tableName(matcher.group(1));
        }
        return SqlTarget.EMPTY;
    }

    /**
     * 判断 SQL 是否包含多语句。
     *
     * @param sql SQL 文本
     * @return 包含多语句时返回 true
     */
    private boolean hasMultipleStatements(String sql) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            } else if (current == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                String tail = sql.substring(index + 1).strip();
                return StringUtils.hasText(tail);
            }
        }
        return false;
    }

    /**
     * 提取首个 SQL 关键字。
     *
     * @param sql SQL 文本
     * @return 大写关键字
     */
    private String firstKeyword(String sql) {
        String trimmed = sql.stripLeading();
        if (!StringUtils.hasText(trimmed)) {
            return "";
        }
        int endIndex = 0;
        while (endIndex < trimmed.length() && Character.isLetter(trimmed.charAt(endIndex))) {
            endIndex++;
        }
        return trimmed.substring(0, endIndex).toUpperCase(Locale.ROOT);
    }

    /**
     * 标准化类型文本。
     *
     * @param value 原始文本
     * @return 大写文本
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }

    /**
     * 清理标识符引号和空白。
     *
     * @param value 原始标识符
     * @return 标准化标识符
     */
    private String cleanIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.strip();
        if (cleaned.startsWith("`") && cleaned.endsWith("`") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }

    /**
     * SQL 目标对象。
     *
     * @param schema 目标 schema
     * @param table  目标表
     */
    private record SqlTarget(String schema, String table) {

        /**
         * 空目标对象。
         */
        private static final SqlTarget EMPTY = new SqlTarget(null, null);
    }
}
