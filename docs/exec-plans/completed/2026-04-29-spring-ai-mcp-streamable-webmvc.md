# Execution Plan: Spring AI MCP Streamable WebMVC

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

接入 Spring AI MCP WebMVC server starter，启用 Streamable HTTP，并暴露一个只用于启动和发现验证的最小 MCP smoke tool。

## Scope

**In scope:**

- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/com/refinex/dbflow/mcp/`
- `src/test/java/com/refinex/dbflow/mcp/`
- `docs/OBSERVABILITY.md`

**Out of scope:**

- 数据库执行 MCP tools
- MCP Bearer Token HTTP 认证链路
- SQL policy、confirmation、audit 的 MCP 集成
- 管理端 UI

## Constraints

- 使用 Context7 与本地 `/Users/refinex/develop/code/spring-ai` 核验 Spring AI 1.1.4 MCP starter、Streamable HTTP 和
  capabilities 配置。
- 不把数据库执行逻辑放入本阶段。
- endpoint 路径、server name、version 必须写入配置和 `docs/OBSERVABILITY.md`。
- 新增 Java 类、方法、属性保留中文 JavaDoc，符合当前 Java 开发规范。

## Acceptance Criteria

- [x] AC-1: `pom.xml` 引入 `spring-ai-starter-mcp-server-webmvc`，版本由 Spring AI BOM 1.1.4 管理。
- [x] AC-2: `application.yml` 配置 MCP server name、version、`protocol=STREAMABLE`、capabilities tool/resource/prompt 和
  Streamable HTTP endpoint。
- [x] AC-3: `com.refinex.dbflow.mcp` 暴露最小 smoke tool，不包含数据库执行逻辑。
- [x] AC-4: 测试覆盖 MCP 属性绑定、WebMVC Streamable 自动配置 bean 和 smoke tool 注册。
- [x] AC-5: `docs/OBSERVABILITY.md` 记录本地启动、endpoint、server name、version 与 smoke discovery 方式。
- [x] AC-6: `./mvnw test` 与 `python3 scripts/check_harness.py` 通过。

## Risk Notes

| Risk                                          | Likelihood | Mitigation                                                             |
|-----------------------------------------------|------------|------------------------------------------------------------------------|
| Spring AI MCP endpoint 协议测试需要完整 MCP client 握手 | Medium     | 优先用自动配置 bean、ToolCallbackProvider 与本地启动 smoke 证明，避免手写易漂移的 JSON-RPC 握手。 |
| MCP starter 引入自动配置导致现有 context 测试受影响          | Medium     | 增加 targeted SpringBootTest 断言属性和 transport bean，失败时只调整 starter 必需配置。   |
| 管理端 security chain 误拦截 MCP endpoint           | Low        | 当前管理端 chain 已限定 `/admin/**`、`/login`、`/logout`；本阶段不新增 MCP 认证链。         |

## Implementation Steps

### Step 1: Add MCP starter and server configuration

**Files:** `pom.xml`, `src/main/resources/application.yml`
**Verification:** `./mvnw -q -DskipTests compile`

Status: ✅ Done
Evidence: `./mvnw -q -DskipTests compile` passed after adding the starter dependency and Streamable HTTP properties.
Deviations:

### Step 2: Register smoke MCP tool

**Files:** `src/main/java/com/refinex/dbflow/mcp/DbflowMcpSmokeTool.java`,
`src/main/java/com/refinex/dbflow/mcp/DbflowMcpToolConfiguration.java`
**Verification:** Targeted test can discover `dbflow_smoke`.

Status: ✅ Done
Evidence: `DbflowMcpSmokeTool` exposes `dbflow_smoke`; `DbflowMcpToolConfiguration` registers it through
`MethodToolCallbackProvider`.
Deviations:

### Step 3: Add MCP server tests

**Files:** `src/test/java/com/refinex/dbflow/mcp/DbflowMcpServerTests.java`
**Verification:** `./mvnw -Dtest=DbflowMcpServerTests test`

Status: ✅ Done
Evidence: `./mvnw -Dtest=DbflowMcpServerTests test` passed; tests assert MCP properties, WebMVC transport bean, and
callable `dbflow_smoke`.
Deviations:

### Step 4: Document commands and archive evidence

**Files:** `docs/OBSERVABILITY.md`, this plan, `docs/PLANS.md`
**Verification:** `./mvnw test` and `python3 scripts/check_harness.py`

Status: ✅ Done
Evidence: `docs/OBSERVABILITY.md` now records `/mcp`, `refinex-dbflow`, `0.1.0-SNAPSHOT`, and smoke discovery
expectations. Local startup on port `18080` returned initialize metadata and `tools/list` discovered `dbflow_smoke`.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                                     | Notes                                                                                   |
|------|--------|--------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| 1    | ✅      | `./mvnw -q -DskipTests compile` passed.                                                                      | Added `spring-ai-starter-mcp-server-webmvc` and `spring.ai.mcp.server.*` configuration. |
| 2    | ✅      | Targeted test discovered and invoked `dbflow_smoke`.                                                         | No database execution logic was added.                                                  |
| 3    | ✅      | `./mvnw -Dtest=DbflowMcpServerTests test` passed.                                                            | Test covers properties, transport provider, and tool callback.                          |
| 4    | ✅      | `./mvnw test` and `python3 scripts/check_harness.py` passed; local JSON-RPC smoke discovered `dbflow_smoke`. |                                                                                         |

## Decision Log

| Decision                                                          | Context                                                                        | Alternatives Considered                          | Rationale                                   |
|-------------------------------------------------------------------|--------------------------------------------------------------------------------|--------------------------------------------------|---------------------------------------------|
| 使用 Spring AI `@Tool` + `MethodToolCallbackProvider` 暴露 smoke tool | Context7 与本地 Spring AI docs 均说明 ToolCallbackProvider 会被 MCP server 自动转换为 tools | 直接使用低层 `McpServerFeatures.SyncToolSpecification` | `@Tool` 路径更贴近 Spring AI 应用开发模型，且不会引入业务执行逻辑。 |

## Completion Summary

Completed: 2026-04-29
Duration: 4 steps
All acceptance criteria: PASS

Summary:

- Added Spring AI MCP WebMVC Server starter under the existing Spring AI BOM 1.1.4.
- Configured `refinex-dbflow` MCP server version `0.1.0-SNAPSHOT` with `STREAMABLE` protocol and endpoint `/mcp`.
- Registered the no-database `dbflow_smoke` tool through Spring AI `@Tool` and `MethodToolCallbackProvider`.
- Verified targeted MCP tests, full Maven tests, Harness validator, and a local JSON-RPC Streamable HTTP smoke
  discovery.
