package com.refinex.dbflow.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DBFlow MCP prompt skeleton。
 *
 * @author refinex
 */
@Component
public class DbflowMcpPrompts {

    /**
     * 生成安全 MySQL 变更 prompt skeleton。
     *
     * @param project       项目标识
     * @param env           环境标识
     * @param changeRequest 变更请求
     * @return prompt 结果
     */
    @McpPrompt(
            name = DbflowMcpNames.PROMPT_SAFE_MYSQL_CHANGE,
            title = "Safe MySQL change",
            description = "Guide an AI client to prepare a safe DBFlow MySQL change request before using SQL tools."
    )
    public McpSchema.GetPromptResult safeMysqlChange(
            @McpArg(name = "project", description = "Project key configured in DBFlow.", required = true) String project,
            @McpArg(name = "env", description = "Environment key configured under the project.", required = true) String env,
            @McpArg(name = "change_request", description = "Natural-language description of the intended database change.", required = true) String changeRequest
    ) {
        String message = """
                你是 DBFlow 安全 MySQL 变更助手。请先明确 project=%s、env=%s 的变更目标，再按以下顺序工作：
                1. 使用 dbflow_get_effective_policy 查看当前策略边界。
                2. 对候选 SQL 使用 dbflow_explain_sql 获取执行计划。
                3. 对高风险 SQL 标记 confirmation 需求，不要绕过服务端授权、策略、审计和确认。
                4. 当前请求：%s
                """.formatted(project, env, changeRequest);
        return prompt("DBFlow 安全 MySQL 变更流程 skeleton", message);
    }

    /**
     * 生成 EXPLAIN 计划评审 prompt skeleton。
     *
     * @param sql         SQL 原文
     * @param explainPlan EXPLAIN 计划文本或 JSON
     * @param riskFocus   重点关注风险
     * @return prompt 结果
     */
    @McpPrompt(
            name = DbflowMcpNames.PROMPT_EXPLAIN_PLAN_REVIEW,
            title = "Explain plan review",
            description = "Guide an AI client to review a MySQL EXPLAIN plan before SQL execution."
    )
    public McpSchema.GetPromptResult explainPlanReview(
            @McpArg(name = "sql", description = "SQL text being reviewed.", required = true) String sql,
            @McpArg(name = "explain_plan", description = "EXPLAIN output as text or JSON.", required = false) String explainPlan,
            @McpArg(name = "risk_focus", description = "Optional risk focus such as lock, full scan, filesort, or rows examined.", required = false) String riskFocus
    ) {
        String message = """
                你是 DBFlow MySQL EXPLAIN 评审助手。请基于 SQL 与执行计划给出结构化评审：
                - 是否存在全表扫描、错误索引、filesort、临时表、锁放大或行数估算异常。
                - 是否需要改写 SQL、补充索引或改为分批执行。
                - 是否建议继续执行、要求人工确认，或直接拒绝。
                SQL:
                %s
                EXPLAIN:
                %s
                重点风险:
                %s
                """.formatted(sql, valueOrPlaceholder(explainPlan), valueOrPlaceholder(riskFocus));
        return prompt("DBFlow EXPLAIN 计划评审 skeleton", message);
    }

    /**
     * 创建 prompt 结果。
     *
     * @param description prompt 描述
     * @param message     prompt 消息
     * @return prompt 结果
     */
    private McpSchema.GetPromptResult prompt(String description, String message) {
        return new McpSchema.GetPromptResult(
                description,
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }

    /**
     * 返回非空文本或占位符。
     *
     * @param value 输入文本
     * @return 非空文本或占位符
     */
    private String valueOrPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return "未提供";
        }
        return value;
    }
}
