# Execution Plan: DBFlow MCP Surface Skeleton

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

建立 DBFlow MCP 对外暴露面的稳定 skeleton，包括六个 tools、三个 resources 和两个 prompts，并通过 discovery 测试固定名称与
URI。

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/mcp/`
- `src/test/java/com/refinex/dbflow/mcp/`
- `docs/ARCHITECTURE.md`
- `docs/OBSERVABILITY.md`

**Out of scope:**

- MCP Bearer Token HTTP filter
- 真实目标数据库 schema inspect、EXPLAIN、执行和 confirmation 业务逻辑
- SQL parser、dangerous DDL policy enforcement、audit 写入
- 管理端 UI

## Constraints

- 使用 Context7 与本地 `/Users/refinex/develop/code/spring-ai` 核验 Spring AI MCP tools/resources/prompts 注册方式。
- Skeleton 可以返回 mock/empty 结构，但必须通过认证上下文和授权服务接口预留边界。
- Tool 名称稳定：`dbflow_list_targets`、`dbflow_inspect_schema`、`dbflow_get_effective_policy`、`dbflow_explain_sql`、
  `dbflow_execute_sql`、`dbflow_confirm_sql`。
- Resources URI 稳定：`dbflow://targets`、`dbflow://projects/{project}/envs/{env}/schema`、
  `dbflow://projects/{project}/envs/{env}/policy`。
- Prompts 名称稳定：`dbflow_safe_mysql_change`、`dbflow_explain_plan_review`。
- 新增 Java 类、方法、属性保持中文 JavaDoc。

## Acceptance Criteria

- [x] AC-1: 六个 DBFlow MCP tools 可以通过 `tools/list` 发现，且 input schema 和 description 面向 AI 客户端清晰。
- [x] AC-2: 三个 DBFlow MCP resources 可以通过 resource/resource-template discovery 发现。
- [x] AC-3: 两个 DBFlow MCP prompts 可以通过 `prompts/list` 发现，且 prompt arguments 明确。
- [x] AC-4: 每个 tool skeleton 调用认证上下文 resolver 和授权边界 service，返回结构包含边界状态。
- [x] AC-5: `./mvnw test` 与 `python3 scripts/check_harness.py` 通过。

## Risk Notes

| Risk                                                                      | Likelihood | Mitigation                                                                                                  |
|---------------------------------------------------------------------------|------------|-------------------------------------------------------------------------------------------------------------|
| Spring AI annotation scanner 对 resources/templates discovery 行为与 tools 不同 | Medium     | 用 HTTP JSON-RPC discovery 测试分别覆盖 `tools/list`、`resources/list`、`resources/templates/list` 和 `prompts/list`。 |
| Skeleton 返回结构过早绑定业务模型                                                     | Medium     | 使用轻量 MCP-facing records，只表达 `status`、`boundary`、`data`、`warnings`。                                          |
| 认证未实现时误导为已授权执行                                                            | Low        | 默认 authentication context 标记为 `ANONYMOUS`，授权边界返回 `AUTHENTICATION_REQUIRED`，执行类 tools 只返回 `SKELETON`。        |

## Implementation Steps

### Step 1: Add MCP boundary models

**Files:** `src/main/java/com/refinex/dbflow/mcp/*`
**Verification:** `./mvnw -Dtest=DbflowMcpServerTests test`

Status: ✅ Done
Evidence: Added `McpAuthenticationContextResolver`, anonymous default context, `McpAccessBoundaryService`,
`DefaultMcpAccessBoundaryService`, and skeleton response records.
Deviations:

### Step 2: Add tool skeletons

**Files:** `src/main/java/com/refinex/dbflow/mcp/*`
**Verification:** `tools/list` discovery test includes six stable DBFlow tool names.

Status: ✅ Done
Evidence: `./mvnw -Dtest=DbflowMcpServerTests,DbflowMcpDiscoveryTests test` passed; `tools/list` discovers all six
stable DBFlow tool names.
Deviations:

### Step 3: Add resource and prompt skeletons

**Files:** `src/main/java/com/refinex/dbflow/mcp/*`
**Verification:** discovery test includes three resources/templates and two prompts.

Status: ✅ Done
Evidence: `DbflowMcpDiscoveryTests` discovers `dbflow://targets`, two resource templates, and both DBFlow prompts
through `/mcp`.
Deviations:

### Step 4: Update documentation and archive evidence

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, this plan, `docs/PLANS.md`
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, `git diff --check`

Status: ✅ Done
Evidence: `docs/ARCHITECTURE.md` and `docs/OBSERVABILITY.md` updated with the current MCP surface and boundary behavior.
`./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                          | Notes                                                                           |
|------|--------|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| 1    | ✅      | Boundary classes and services added.                                              | Default context remains anonymous until MCP Bearer Token filter is implemented. |
| 2    | ✅      | Targeted MCP tests passed.                                                        | Six skeleton tools added with stable names and input descriptions.              |
| 3    | ✅      | Discovery test passed through `/mcp`.                                             | One static resource, two resource templates, and two prompts are discoverable.  |
| 4    | ✅      | `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` passed. |                                                                                 |

## Decision Log

| Decision                                 | Context                                                                        | Alternatives Considered                       | Rationale                                                                          |
|------------------------------------------|--------------------------------------------------------------------------------|-----------------------------------------------|------------------------------------------------------------------------------------|
| 使用 Spring AI MCP annotations 暴露 skeleton | Context7 和本地 Spring AI 1.1.4 源码均支持 `@McpTool`、`@McpResource`、`@McpPrompt` 自动注册 | 手工构造 `McpServerFeatures.*Specification` beans | 注解方式能让 input schema、resource template、prompt argument discovery 更贴近正式 MCP surface。 |
| 默认认证上下文为匿名                               | MCP Bearer Token HTTP filter 尚未实现                                              | 让 skeleton 假装授权成功                             | 匿名上下文能保留安全边界，不误导客户端已经具备执行权限。                                                       |

## Completion Summary

Completed: 2026-04-29
Duration: 4 steps
All acceptance criteria: PASS

Summary:

- Added stable DBFlow MCP tool skeletons for list targets, inspect schema, effective policy, explain SQL, execute SQL,
  and confirm SQL.
- Added DBFlow MCP resource skeletons for targets, schema, and policy.
- Added DBFlow MCP prompt skeletons for safe MySQL change and EXPLAIN plan review.
- Added MCP authentication context and access boundary placeholders so every tool response records authentication and
  authorization state.
- Verified discovery through `/mcp` JSON-RPC plus full Maven and Harness validation.
