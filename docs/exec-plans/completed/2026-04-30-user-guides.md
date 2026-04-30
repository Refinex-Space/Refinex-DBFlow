# 2026-04-30 User Guides

## Objective

为 Refinex-DBFlow 补齐面向管理员、员工操作者和安全审计读者的用户手册，使管理端上线前具备可执行的日常使用、MCP 首用验证和审计解释材料。

## Scope

- 编写 `docs/user-guide/admin-guide.md`。
- 编写 `docs/user-guide/operator-guide.md`。
- 编写 `docs/user-guide/security-and-audit.md`。
- 更新文档索引，使新手册能从现有架构和可观测性文档中被发现。

## Non-Scope

- 不修改生产 Java 代码、Spring Security 配置或 Thymeleaf 页面。
- 不新增 MCP 客户端配置能力；客户端细节继续以 `docs/user-guide/mcp-clients.md` 为准。
- 不提交真实 Token、数据库密码、Nacos 密码或内网真实地址。

## Constraints

- 中文正文，保留 `MCP`、`Token`、`project/env`、`Streamable HTTP`、`Actuator` 等英文技术名词。
- 不只写安装步骤；每份面向使用的手册必须包含 first-use smoke test。
- 文档里的命令、路径、路由和工具名必须与当前项目一致。
- 敏感信息边界必须清楚：不指导记录 Token 明文、Token hash、数据库密码或完整 SQL 结果集。

## Assumptions

- 本地默认服务地址为 `http://127.0.0.1:8080`，内网示例使用 `https://dbflow.internal.example`。
- 管理端已按当前实现暴露 `/login` 和 `/admin/**` 页面。
- MCP endpoint 为 `/mcp`，由 `Authorization: Bearer <DBFlow Token>` 认证。
- 当前 DBFlow tool 名称以 `DbflowMcpNames` 中的稳定常量为准。

## Acceptance Criteria

- [x] `docs/user-guide/admin-guide.md` 存在，覆盖登录、创建用户、授权项目环境、颁发/吊销 Token、查看审计、查看策略、排查连接，并包含管理端
  smoke test。
- [x] `docs/user-guide/operator-guide.md` 存在，覆盖申请 Token、配置 MCP、选择 project/env、执行查询、处理确认、理解拒绝原因，并包含员工
  first-use smoke test。
- [x] `docs/user-guide/security-and-audit.md` 存在，解释 AI 操作数据库不黑盒的审计价值，并包含审计 smoke test。
- [x] 新文档不包含真实密钥，且路径、命令、路由、MCP tool 名称与当前项目一致。
- [x] `./mvnw test` 和 `python3 scripts/check_harness.py` 通过。

## Plan

1. [x] 创建三份用户手册，优先引用当前实现中的路由、工具名和安全边界。
2. [x] 更新 `docs/ARCHITECTURE.md`、`docs/OBSERVABILITY.md` 和 `docs/PLANS.md` 的文档索引。
3. [x] 运行文档自检、diff whitespace 检查、Harness 校验和 Maven 测试。
4. [x] 记录验证证据，归档本计划。

## Evidence Log

- 2026-04-30: Added administrator, operator, and security/audit guides under `docs/user-guide/`.
- 2026-04-30: Updated architecture and observability documentation indexes with the new user guide entry points.
- 2026-04-30: Verified required files exist with
  `test -f docs/user-guide/admin-guide.md && test -f docs/user-guide/operator-guide.md && test -f docs/user-guide/security-and-audit.md`.
- 2026-04-30: Verified documented routes, MCP tool names, first-use smoke sections, and sensitive-data boundary terms
  with `rg` over `docs/user-guide/*.md`.
- 2026-04-30: `git diff --check` passed.
- 2026-04-30: `python3 scripts/check_harness.py` passed.
- 2026-04-30: `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
