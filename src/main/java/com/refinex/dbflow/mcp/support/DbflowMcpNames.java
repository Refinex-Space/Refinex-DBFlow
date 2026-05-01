package com.refinex.dbflow.mcp.support;

/**
 * DBFlow MCP 暴露面稳定名称常量。
 *
 * @author refinex
 */
public final class DbflowMcpNames {

    /**
     * 列出可访问目标库工具名。
     */
    public static final String TOOL_LIST_TARGETS = "dbflow_list_targets";

    /**
     * 查看目标库 schema 工具名。
     */
    public static final String TOOL_INSPECT_SCHEMA = "dbflow_inspect_schema";

    /**
     * 获取有效 SQL policy 工具名。
     */
    public static final String TOOL_GET_EFFECTIVE_POLICY = "dbflow_get_effective_policy";

    /**
     * 解释 SQL 执行计划工具名。
     */
    public static final String TOOL_EXPLAIN_SQL = "dbflow_explain_sql";

    /**
     * 执行 SQL 工具名。
     */
    public static final String TOOL_EXECUTE_SQL = "dbflow_execute_sql";

    /**
     * 确认高风险 SQL 工具名。
     */
    public static final String TOOL_CONFIRM_SQL = "dbflow_confirm_sql";

    /**
     * 目标列表资源 URI。
     */
    public static final String RESOURCE_TARGETS = "dbflow://targets";

    /**
     * schema 资源 URI 模板。
     */
    public static final String RESOURCE_SCHEMA = "dbflow://projects/{project}/envs/{env}/schema";

    /**
     * policy 资源 URI 模板。
     */
    public static final String RESOURCE_POLICY = "dbflow://projects/{project}/envs/{env}/policy";

    /**
     * 安全 MySQL 变更 prompt 名称。
     */
    public static final String PROMPT_SAFE_MYSQL_CHANGE = "dbflow_safe_mysql_change";

    /**
     * EXPLAIN 计划评审 prompt 名称。
     */
    public static final String PROMPT_EXPLAIN_PLAN_REVIEW = "dbflow_explain_plan_review";

    /**
     * 禁止实例化常量类。
     */
    private DbflowMcpNames() {
    }
}
