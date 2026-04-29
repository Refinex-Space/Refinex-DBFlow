# Execution Plan: MCP Bearer Token Authentication

## Objective

让 Spring AI MCP Streamable HTTP endpoint `/mcp` 使用 DBFlow 自有 MCP Token 进行每请求 Bearer
认证，并把认证后的用户/Token 上下文交给现有 MCP skeleton 授权边界。

## Scope

- 为 `/mcp` 增加独立于管理端 form login 的 Spring Security filter chain。
- 只接受 `Authorization: Bearer <token>`，拒绝 query string token。
- 使用现有 `McpTokenService` 校验 Token hash、状态、过期时间和吊销状态。
- 将校验结果写入 Spring SecurityContext，并由 MCP 层解析为 `McpAuthenticationContext`。
- 记录 clientInfo、User-Agent、source IP、request id 的提取策略。
- 增加 MCP endpoint 认证测试。

## Non-Scope

- 不实现 SQL 执行、SQL policy、audit 持久化或 MCP tool 业务逻辑。
- 不引入 OAuth2/JWT MCP security 模块；本阶段沿用 DBFlow opaque Token 生命周期服务。
- 不把项目环境授权拒绝改造成完整 HTTP 403 业务路径；只预置 Spring Security access denied 处理。

## Constraints

- 每个 MCP HTTP 请求都必须重新校验 Bearer Token，不能依赖 `Mcp-Session-Id`。
- Token 明文不得写日志、审计或响应。
- 管理端 session 安全链和 MCP Bearer 安全链必须分离。

## Assumptions

- 当前 MCP endpoint 配置为 `/mcp`，本阶段安全链按该路径精确匹配。
- clientInfo 的协议内结构化提取需要读取 JSON-RPC body；当前 filter 不消费 body，仅记录 header/后续审计层提取策略。
- 合法 Token 的项目/环境授权仍由现有 `AccessDecisionService` 在 MCP tool 边界判断。

## Acceptance Criteria

- [x] AC-1: `/mcp` 无 Token、非法 Token、吊销 Token 返回 HTTP 401。
- [x] AC-2: `/mcp` query string token 被拒绝，即使同时提供 header token。
- [x] AC-3: `/mcp` 合法 Token 可完成 MCP initialize/discovery。
- [x] AC-4: 同一 MCP session 后续请求缺少 Bearer Token 仍返回 401。
- [x] AC-5: MCP tool skeleton 能从 SecurityContext 解析 authenticated DBFlow context。
- [x] AC-6: `./mvnw test` 和 `python3 scripts/check_harness.py` 通过。

## Plan

| Step                                                                              | Status | Evidence                                                                                                                            |
|-----------------------------------------------------------------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------|
| 1. 建立 active plan 并登记到 `docs/PLANS.md`。                                           | Done   | Baseline `./mvnw test` and `python3 scripts/check_harness.py` passed before edits.                                                  |
| 2. 增加 MCP Bearer authentication token、metadata extractor、filter 和 security chain。 | Done   | Added `McpSecurityConfiguration`, `McpBearerTokenAuthenticationFilter`, `McpAuthenticationToken`, and request metadata types.       |
| 3. 将 MCP authentication context resolver 接到 Spring SecurityContext。               | Done   | Replaced anonymous-only resolver with `SecurityContextMcpAuthenticationContextResolver`; direct calls still fall back to anonymous. |
| 4. 增加 MCP HTTP 认证测试并调整 discovery 测试携带 Bearer Token。                               | Done   | `./mvnw -Dtest=McpSecurityTests,DbflowMcpDiscoveryTests,DbflowMcpServerTests test` passed after async-dispatch fix.                 |
| 5. 更新 architecture/observability 文档和 plan evidence。                               | Done   | Updated MCP security/runtime/test coverage notes in `docs/ARCHITECTURE.md` and `docs/OBSERVABILITY.md`.                             |
| 6. 运行 targeted/full verification 并归档计划。                                           | Done   | `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` passed.                                                   |

## Evidence Log

- Preflight `python3 scripts/check_harness.py`: passed, 14 Harness artifacts found.
- Preflight `./mvnw test`: passed, 46 tests passed.
- Context7 verified Spring Security multiple filter chains/stateless session configuration and Spring AI MCP security
  guidance for token authentication on every request.
- Local Spring AI checkout verified MCP Streamable HTTP endpoint and security docs under
  `/Users/refinex/develop/code/spring-ai/spring-ai-docs/src/main/antora/modules/ROOT/pages/api/mcp/`.
- Targeted test first failed because a `OncePerRequestFilter` bean was registered as a global Servlet filter; fixed by
  constructing the filter only inside the MCP SecurityFilterChain.
- Targeted test then failed on Streamable HTTP async dispatch because SecurityContext was missing in the async thread;
  fixed by letting the Bearer filter run during async dispatch and re-authenticate from the request header.
- Targeted `./mvnw -Dtest=McpSecurityTests,DbflowMcpDiscoveryTests,DbflowMcpServerTests test`: passed, 9 tests passed.
- Full `./mvnw test`: passed, 51 tests passed.
- Final `python3 scripts/check_harness.py`: passed, 14 Harness artifacts found.
- Final `git diff --check`: passed.
