<!-- Harness brainstorm spec. Safe to edit before approval. -->

# Refinex-DBFlow MCP 架构设计

Date: 2026-04-29

Status: Approved

## 背景

Refinex-DBFlow 是面向企业内网的 MySQL MCP 数据库操作网关。它让 Codex、Claude、OpenCode、Copilot 等 AI 工具通过 MCP 操作企业授权范围内的 MySQL 数据库，同时由服务端统一承担认证、授权、危险 SQL 拦截、二次确认、执行审计和结果留痕。

当前仓库是空仓库形态，仅存在 `README.md` 与 `LICENSE`。本设计用于统领后续项目初始化、Harness 控制面、Spring Boot 工程脚手架、MCP server、管理端、审计与数据库执行核心实现。

## 目标

- 基于 MCP 官方协议与 Spring AI MCP 1.1.4 构建远程 MCP Server。
- 支持 JDK 21、Spring Boot 3.5.13、Spring AI 1.1.4、Spring Cloud 2025.0.2、Spring Cloud Alibaba Nacos。
- 支持 MySQL 8 作为主要目标，MySQL 5.7 作为次要兼容目标。
- 支持 YAML / Nacos 配置多项目、多环境、多数据源连接信息。
- 支持局域网管理后台：管理员登录、创建用户、授权项目环境、颁发或吊销用户 Token。
- 支持用户通过 Token 在 MCP 客户端配置后访问授权项目环境。
- 支持全面 MySQL 操作：DDL、DML、查询、索引优化、执行计划分析、存储过程/函数管理。
- 支持服务端强制拦截危险操作，并持久化审计“谁、何时、用什么 AI 工具、对哪个项目环境、执行什么 SQL、结果如何、通过还是拒绝”。

## 非目标

- MVP 不做公网上的多租户 SaaS。
- MVP 不内置 LLM、Text-to-SQL 模型或 Agent 编排框架。
- MVP 不支持 MySQL 以外的数据库。
- MVP 不做数据库迁移平台替代品，不负责 schema migration 的发布流程编排。
- MVP 不把客户端确认视为安全边界；所有危险判断必须在服务端完成。
- MVP 不把完整查询结果默认写入审计库，只记录摘要、行数、错误、SQL 原文和 SQL hash，避免审计库变成敏感数据副本。

## 官方依据

- MCP 是 AI 应用连接外部系统的开放标准，服务端主要暴露 tools、resources、prompts。
- MCP 架构是 host/client/server；远程 MCP Server 更适合使用 Streamable HTTP。
- Streamable HTTP 使用单一 HTTP endpoint，支持 POST/GET、可选 SSE；服务端必须重视认证、Origin 校验和会话安全。
- MCP HTTP 认证应使用 `Authorization: Bearer <access-token>`，Token 必须每个请求携带，不应进入 query string。
- Spring AI MCP server starter 支持 WebMVC/WebFlux 与 `SSE`、`STREAMABLE`、`STATELESS` 等协议配置。

参考链接：

- https://modelcontextprotocol.io/docs/getting-started/intro
- https://modelcontextprotocol.io/docs/learn/architecture
- https://modelcontextprotocol.io/docs/develop/build-server#java
- https://modelcontextprotocol.io/specification/2025-03-26/basic/transports
- https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization
- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-streamable-http-server-boot-starter-docs.html
- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-stateless-server-boot-starter-docs.html
- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-security.html

## 版本决策

| 组件 | 版本 / 坐标 | 决策 |
| --- | --- | --- |
| JDK | 21 | 项目基线。 |
| Spring Boot | 3.5.13 | 已在 Maven Central 验证存在。 |
| Spring AI | 1.1.4 | 使用 `org.springframework.ai:spring-ai-bom:1.1.4`。 |
| Spring Cloud | 2025.0.2 | 已在 Maven Central 验证存在。 |
| Spring Cloud Alibaba | 2025.0.0.0 | Maven Central 的 BOM 版本是 `2025.0.0.0`，不是 `2025.0.0`。 |
| MySQL | 8.x / 5.7 | MySQL 8 优先；5.7 兼容核心 DDL/DML/EXPLAIN。 |
| Build | Maven | Spring 生态默认路径，后续脚手架使用 Maven wrapper。 |

## 方案比较

### 方案 A：远程 Streamable HTTP MCP 网关

使用 Spring Boot WebMVC + Spring AI MCP WebMVC starter，以 `STREAMABLE` 协议提供远程 MCP endpoint。管理端、认证、授权、SQL 策略、审计和连接池都在同一个中心服务内完成。

优点：

- 符合企业内网“统一入口、统一审计、统一授权”的诉求。
- 支持多个 MCP 客户端复用同一个服务。
- 便于管理员管理 Token、项目环境授权和审计查询。
- 更适合后续接入 Nacos、网关、Prometheus、企业 SSO。

风险：

- 远程 MCP 需要更严格的 HTTP 安全、Origin 校验、Token 管理和审计。
- Streamable HTTP 的客户端兼容度比 stdio 更依赖各 AI 工具版本。

结论：采用。

### 方案 B：本地 stdio MCP 工具

每个用户本机运行一个 stdio MCP server，本地工具连接企业数据库。

优点：

- Claude Desktop / 部分 CLI 工具接入简单。
- 网络协议和远程认证复杂度较低。

风险：

- 企业审计、集中授权、Token 管理和配置下发弱。
- 数据库凭据容易分散到员工本机。
- 无法形成企业推广时的“AI 操作数据库不黑盒”亮点。

结论：拒绝作为主架构。未来可考虑提供一个轻量 stdio bridge，转发到远程 Streamable HTTP 服务。

### 方案 C：细粒度 MCP tools 严格拆分

将查询、建表、改表、删表、索引、存储过程等全部拆成独立 MCP tool。

优点：

- 单个工具输入 schema 更清晰。
- 更容易给 LLM 暴露强约束语义。

风险：

- 员工使用会被工具边界频繁打断。
- MySQL 语法面太大，过早拆分会导致工具爆炸。
- AI 客户端体验不如通用 SQL 工作流自然。

结论：MVP 不采用。MVP 采用“宽入口、强拦截”；对 schema、explain、policy 等高频只读能力保留独立工具。

## 总体架构决策

采用“单体优先、边界清晰”的 Spring Boot 应用，不在 MVP 阶段拆多 Maven module。代码按 package 边界组织，后续当复杂度上升再拆模块。

架构分层：

```text
MCP Clients
  Codex / Claude / OpenCode / Copilot / MCP Inspector
        |
        | Streamable HTTP + Bearer Token
        v
Refinex-DBFlow Spring Boot
  web-admin      管理端登录、用户、授权、Token、审计查询
  mcp-adapter    MCP tools/resources/prompts 暴露层
  security       管理端 session 与 MCP token 认证
  access         用户、Token、项目环境授权
  config         YAML/Nacos 项目环境配置加载与校验
  sql-policy     SQL 解析、风险分类、白名单、二次确认
  executor       Hikari 数据源注册、JDBC 执行、EXPLAIN
  audit          审计事件、确认挑战、结果摘要持久化
  observability  Actuator、日志、指标、trace/request id
        |
        +--> Metadata DB: users / tokens / grants / audit / confirmations
        |
        +--> Target MySQL: project-env configured databases
```

## MCP 暴露面

MVP 暴露少量稳定工具，避免“工具爆炸”，同时不把员工卡得太死。

| MCP tool | 用途 | 执行 SQL |
| --- | --- | --- |
| `dbflow_list_targets` | 返回当前 Token 可访问的项目和环境。 | 否 |
| `dbflow_inspect_schema` | 查看库、表、字段、索引、存储过程/函数元数据。 | 只读 |
| `dbflow_get_effective_policy` | 返回当前项目环境下的危险操作策略摘要，帮助 AI 生成合规 SQL。 | 否 |
| `dbflow_explain_sql` | 对 SELECT / DML 执行 `EXPLAIN` 或 `EXPLAIN FORMAT=JSON`。 | 只读 explain |
| `dbflow_execute_sql` | 通用单条 SQL 执行入口，支持 DDL/DML/查询。 | 是 |
| `dbflow_confirm_sql` | 对需要二次确认的 SQL 使用 `confirmationId` 完成执行。 | 是 |

MCP resources：

| Resource URI | 内容 |
| --- | --- |
| `dbflow://targets` | 当前用户可访问的项目环境摘要。 |
| `dbflow://projects/{project}/envs/{env}/schema` | 指定项目环境 schema 快照。 |
| `dbflow://projects/{project}/envs/{env}/policy` | 指定项目环境有效策略摘要。 |

MCP prompts：

| Prompt | 用途 |
| --- | --- |
| `dbflow_safe_mysql_change` | 引导 AI 先 inspect/explain，再执行变更。 |
| `dbflow_explain_plan_review` | 引导 AI 分析执行计划和索引建议。 |

## SQL 执行流程

`dbflow_execute_sql` 的服务端流程：

1. 读取 Bearer Token，校验 token hash、状态、过期时间。
2. 解析 MCP client/server 上下文，提取 client name/version、User-Agent、source IP、request id、session id。
3. 校验用户是否有目标 `projectKey` + `environmentKey` 权限。
4. 校验 SQL 是单条语句；MVP 默认拒绝多语句执行。
5. 使用 JSQLParser 解析 SQL，提取 statement type、schema、table、operation、是否 DDL/DML、是否危险操作。
6. 对解析失败的 DDL/DML 默认拒绝；对 explain/metadata 类能力走专用工具。
7. 读取项目环境有效 policy，进行风险分类和拦截判断。
8. 若 SQL 需要二次确认，创建 confirmation challenge，审计状态为 `REQUIRES_CONFIRMATION`，返回 `confirmationId` 和确认说明。
9. 若 SQL 允许执行，从对应项目环境的 HikariDataSource 获取连接。
10. 设置 query timeout、max rows、fetch size、只读标记或事务边界。
11. 执行 SQL。
12. 对查询类返回受限结果集；对变更类返回 affected rows、generated keys 摘要、warning 摘要。
13. 审计记录最终状态：`ALLOWED_EXECUTED`、`DENIED`、`FAILED`、`CONFIRMATION_EXPIRED` 等。

## SQL 策略决策

MVP 采用“宽入口、强拦截”：

- 不把 DDL/DML 能力拆成大量强约束工具。
- 员工可通过通用 SQL 工具自然操作数据库。
- 服务端只拦截明确高危操作，并提供可配置策略。

风险级别：

| 风险 | 操作 | MVP 行为 |
| --- | --- | --- |
| LOW | SELECT、SHOW、DESC/DESCRIBE、EXPLAIN | 按授权执行，结果限流。 |
| MEDIUM | INSERT、UPDATE、DELETE、CREATE TABLE、ALTER TABLE、CREATE/ALTER/DROP INDEX、CREATE/ALTER/DROP PROCEDURE、CREATE/ALTER/DROP FUNCTION | 按授权执行并审计；可通过配置增强限制。 |
| HIGH | TRUNCATE | 默认允许进入二次确认，确认后执行。 |
| CRITICAL | DROP TABLE、DROP DATABASE | 默认全局拒绝，仅命中 YAML 白名单才允许。 |
| FORBIDDEN | GRANT、REVOKE、CREATE USER、ALTER USER、DROP USER、SET GLOBAL、SHUTDOWN、INSTALL PLUGIN、LOAD DATA、SELECT ... INTO OUTFILE | MVP 默认拒绝，后续单独评审。 |

危险 DDL 规则：

- `DROP DATABASE`：默认拒绝；仅 YAML 白名单放行。
- `DROP TABLE`：默认拒绝；仅 YAML 白名单放行。
- `TRUNCATE`：默认允许但必须服务端二次确认。

二次确认规则：

- `dbflow_execute_sql` 遇到 `TRUNCATE` 时不执行，返回 `confirmationId`、SQL hash、目标库表、过期时间和风险说明。
- `dbflow_confirm_sql` 必须由同一 user、同一 token、同一 project/env、同一 SQL hash 在有效期内调用。
- 确认挑战默认 5 分钟过期，使用后立即失效。
- 确认、过期、取消、失败都进入审计。

生产环境增强建议：

- `prod` 环境默认禁止 CRITICAL 操作，即使配置白名单，也必须显式配置 `allow-prod-dangerous-ddl: true`。
- `prod` 环境可配置 UPDATE/DELETE 无 WHERE 时要求二次确认；MVP 默认作为可配置项，不强制阻断 dev/test。

## YAML 配置模型

配置分为两类：

- 静态/动态配置：项目环境、连接池、SQL policy、白名单，可由本地 YAML 或 Nacos Config 提供。
- 敏感配置：数据库密码、token pepper 等不进入仓库；通过环境变量、Nacos 加密配置或密钥管理系统注入。

示例结构：

```yaml
dbflow:
  datasource-defaults:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 1
      connection-timeout: 30000
      validation-timeout: 5000
      max-lifetime: 1800000
  security:
    mcp:
      allowed-origins:
        - "http://localhost:*"
        - "http://192.168.*"
      token-header: Authorization
  projects:
    - key: crm
      name: CRM
      environments:
        - key: dev
          mysql-version: "8"
          jdbc-url: "${DBFLOW_CRM_DEV_URL}"
          username: "${DBFLOW_CRM_DEV_USERNAME}"
          password: "${DBFLOW_CRM_DEV_PASSWORD}"
          driver-class-name: com.mysql.cj.jdbc.Driver
        - key: prod
          mysql-version: "8"
          jdbc-url: "${DBFLOW_CRM_PROD_URL}"
          username: "${DBFLOW_CRM_PROD_USERNAME}"
          password: "${DBFLOW_CRM_PROD_PASSWORD}"
  policies:
    dangerous-ddl:
      truncate:
        require-confirmation: true
        confirmation-ttl: 5m
      drop-table:
        default-action: deny
        whitelist:
          - project: crm
            environment: dev
            schema: crm_dev
            table: temp_*
      drop-database:
        default-action: deny
        whitelist:
          - project: sandbox
            environment: dev
            schema: sandbox_*
```

## 数据库操作框架决策

目标数据库执行采用 Spring JDBC / `JdbcTemplate` / `NamedParameterJdbcTemplate`，不采用 MyBatis 或 jOOQ 作为主执行层。

理由：

- DBFlow 的核心是执行用户/AI 提交的动态 SQL，不是维护业务系统的静态 mapper。
- MyBatis 更适合固定业务 SQL，无法天然覆盖任意 DDL/DML。
- jOOQ 更适合类型安全 DSL 和 schema-aware 开发，不适合作为任意 SQL 网关的第一层。
- Spring JDBC 更贴近 SQL 网关场景，便于控制 connection、timeout、result limit、metadata、warnings 和审计。

辅助框架：

- HikariCP：每个项目环境独立连接池，共享默认连接池配置。
- JSQLParser：SQL 解析、statement 分类、危险目标提取。
- Flyway：元数据库 schema 迁移。
- Spring Data JPA：元数据 CRUD，包括 users、tokens、grants、audit、confirmation。
- Micrometer + Actuator：指标与健康检查。
- Testcontainers：MySQL 8 / MySQL 5.7 集成测试。

## 数据模型

元数据库表：

| 表 | 作用 |
| --- | --- |
| `dbf_users` | 管理端用户与 MCP 用户。 |
| `dbf_api_tokens` | 用户唯一 Token，存 hash、prefix、状态、过期时间；明文只展示一次。 |
| `dbf_projects` | 项目元数据，可从 YAML 同步或在管理端只读展示。 |
| `dbf_environments` | 项目环境元数据，可从 YAML 同步或在管理端只读展示。 |
| `dbf_user_env_grants` | 用户对项目环境的授权关系。 |
| `dbf_confirmation_challenges` | 二次确认挑战记录。 |
| `dbf_audit_events` | 所有 MCP 操作与拒绝记录。 |

Token 约束：

- 一个人只能持有 0 到 1 个 active token。
- Token 明文只在颁发时展示一次。
- 元数据库仅保存 token hash、token prefix、last used、status。
- 管理员可吊销并重新颁发。

审计核心字段：

- `request_id`
- `trace_id`
- `mcp_session_id`
- `user_id`
- `token_id`
- `client_name`
- `client_version`
- `user_agent`
- `source_ip`
- `project_key`
- `environment_key`
- `mcp_tool_name`
- `operation_type`
- `risk_level`
- `policy_decision`
- `sql_text`
- `sql_hash`
- `normalized_sql`
- `target_schema`
- `target_table`
- `confirmation_id`
- `status`
- `rows_affected`
- `result_summary`
- `error_code`
- `error_message`
- `duration_ms`
- `created_at`

## 安全设计

管理端：

- Spring Security 表单登录。
- 密码使用强 hash，MVP 可用 BCrypt。
- 管理端启用 CSRF。
- 管理员操作 Token、授权、白名单查看、审计查询。

MCP 端：

- 使用 Bearer Token。
- Token 不允许出现在 URL query string。
- Token 不写日志，不写审计原文。
- 服务端校验 HTTP Origin，局域网部署默认只允许可信来源。
- 对所有 MCP 请求执行认证与授权，不依赖 MCP session 复用认证状态。
- 对 Streamable HTTP endpoint 设置 rate limit、request size limit、timeout。

目标数据库：

- 每个环境使用独立数据库账号。
- 生产账号按最小权限原则配置；服务端策略不是唯一防线。
- 数据库密码不进入 git。
- 连接池按项目环境隔离，避免跨环境连接串用。

## Nacos 使用边界

Nacos 用于：

- 项目环境 YAML 配置下发。
- 服务注册与发现。
- 多节点部署时统一配置来源。

MVP 策略：

- 支持本地 YAML 与 Nacos Config 两种来源。
- 配置变更先校验再生效。
- 数据源重载采用“候选连接池预热成功后原子替换”的模式。
- 如果新配置校验失败，保留旧配置并记录告警。

## MySQL 兼容策略

MySQL 8：

- 作为主要验证目标。
- 支持 `EXPLAIN FORMAT=JSON`、information_schema 元数据、索引分析、存储过程/函数元数据。

MySQL 5.7：

- 作为兼容目标。
- 不依赖 MySQL 8 专属能力实现核心流程。
- 集成测试覆盖连接、schema inspect、SELECT、DML、DDL、EXPLAIN、TRUNCATE 确认、DROP 白名单。

SQL parser 兼容：

- 若 JSQLParser 无法解析 MySQL 特定 DDL，服务端不得直接放行危险 SQL。
- 解析失败的 DDL/DML 默认拒绝并审计。
- 后续可为特定 MySQL 语法补充专门 classifier。

## 错误处理

错误返回应对 AI 工具友好：

- 认证失败：HTTP 401。
- 授权不足：HTTP 403。
- SQL 被策略拒绝：MCP tool 返回结构化拒绝结果，并记录审计。
- 需要二次确认：MCP tool 返回 `requiresConfirmation=true`、`confirmationId`、过期时间、原因。
- SQL 执行失败：返回数据库错误摘要，不泄露连接密码、内部堆栈或 Token。
- 查询结果过大：返回截断说明、实际返回行数、建议增加 WHERE/LIMIT。

## 测试策略

单元测试：

- Token hash 与唯一 active token 约束。
- YAML 配置绑定与校验。
- SQL 风险分类。
- DROP DATABASE / DROP TABLE 白名单匹配。
- TRUNCATE confirmation 生命周期。
- 审计事件状态转换。

集成测试：

- Spring Security MCP Bearer Token 认证。
- Spring AI MCP tools 能被列出和调用。
- MySQL 8 Testcontainers 执行核心 DDL/DML/EXPLAIN。
- MySQL 5.7 Testcontainers 执行兼容性用例。
- Nacos 配置加载可用性测试可后置到独立 profile。

端到端 smoke：

- 管理员创建用户。
- 管理员授权项目环境。
- 管理员颁发 Token。
- MCP Inspector 或兼容客户端配置 Bearer Token。
- 调用 `dbflow_list_targets`。
- 调用 `dbflow_inspect_schema`。
- 执行普通 SELECT。
- 执行 TRUNCATE 返回 confirmation。
- 使用 confirmation 执行成功。
- 执行未白名单 DROP TABLE 被拒绝并审计。

## 验收标准

- 仓库完成 Harness bootstrap 后，架构文档能链接到本 spec。
- Spring Boot 工程能在 JDK 21 下构建。
- MCP endpoint 能通过 Spring AI MCP server starter 暴露至少 6 个 MVP tools。
- Bearer Token 认证对 MCP endpoint 生效。
- 用户只能访问被授权的项目环境。
- YAML 能配置至少 2 个 project、每个 project 至少 2 个 environment，并复用 Hikari 默认配置。
- SQL policy 能拒绝未白名单 `DROP DATABASE` 与 `DROP TABLE`。
- `TRUNCATE` 必须先返回 confirmation，再由同一用户同一 token 确认执行。
- 所有允许、拒绝、确认、失败操作均写入审计表。
- MySQL 8 集成测试通过。
- MySQL 5.7 核心兼容测试通过。

## 后续执行顺序

1. 使用 `harness-bootstrap` 初始化空仓库控制面。
2. 将本 spec 纳入 `docs/PLANS.md` 与后续 `docs/ARCHITECTURE.md`。
3. 使用 `harness-plan` 将本 spec 拆成可执行计划。
4. 第一阶段实现 Maven/Spring Boot skeleton、元数据库、配置绑定、MCP endpoint 与基础 security。
5. 第二阶段实现 SQL parser/policy/executor/audit。
6. 第三阶段实现管理端与 Nacos。
7. 第四阶段补足 MySQL 5.7、审计查询、文档与客户端配置示例。
