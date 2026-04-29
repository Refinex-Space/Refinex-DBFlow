# Refinex-DBFlow 0-1 Development Plans

本文档是 Refinex-DBFlow 从 0 到 1 落地的总控路线图。后续每次打开 Codex App 时，先读本文件，再按阶段顺序复制对应提示词推进实现。

设计依据：

- 架构总纲：[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- 已批准 spec：[docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md](docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md)
- 本地 Spring AI 官方源码：`/Users/refinex/develop/code/spring-ai`
- MCP 官方与 Spring AI 文档：涉及协议、starter、annotation、security、配置时必须用 Context7 或官方源码核验。

## 使用原则

1. 严格按阶段顺序推进；前一阶段没有通过验收，不进入后一阶段。
2. 每个阶段复制本文提供的 Prompt 给 Codex App；不要口头省略验收标准。
3. 每个实现阶段都必须先跑 `python3 scripts/check_harness.py`。
4. 每个阶段完成前必须使用 `harness-verify`，并把验证命令写入对应执行计划 evidence。
5. 每个阶段提交一个中文 conventional commit；不要把多个阶段揉成一个巨型提交。
6. Spring AI MCP、Spring Boot、Spring Cloud Alibaba、Nacos、MCP 协议相关实现必须先查 Context7，并优先核验 `/Users/refinex/develop/code/spring-ai`。
7. UI 原型阶段使用 `huashu-design`，产物放在 `docs/prototypes/admin/`，后续管理端实现必须一比一转化该原型的布局、信息架构和关键交互。

## Prompt 模版

后续复制任务时使用以下结构。本文每个子任务已经按此格式写好。

```text
$<skill-name>

任务：<一句话目标>

任务明细：
- <具体事项>
- <具体事项>
- <具体事项>

硬性约束：
- 先读 AGENTS.md、docs/ARCHITECTURE.md、docs/OBSERVABILITY.md、docs/PLANS.md。
- 涉及库/API/配置时先用 Context7，再核验 /Users/refinex/develop/code/spring-ai。
- 不删除用户已有改动。
- 完成前运行 python3 scripts/check_harness.py 和本阶段指定测试。

验收标准：
- <可验证结果>
- <可验证结果>
```

## Harness Powers Lifecycle Reminder

- Use `harness-using` to route repository work.
- Store approved design specs in `docs/exec-plans/specs/`.
- Store active execution plans in `docs/exec-plans/active/`.
- Archive completed execution plans in `docs/exec-plans/completed/`.
- Use `harness-verify` before completion claims.

## 0-1 Phase Map

| 阶段 | 目标 | 关键产物 | 进入下一阶段条件 |
| --- | --- | --- | --- |
| P00 | 规划基线确认 | 当前路线图、Harness 绿色 | `check_harness.py` 通过 |
| P01 | Spring Boot 工程骨架 | Maven wrapper、`pom.xml`、基础包结构 | `mvn test` 通过 |
| P02 | 元数据库与配置模型 | Flyway、JPA entity、YAML binding | 单元测试和迁移测试通过 |
| P03 | 身份、Token 与授权模型 | 用户、Token、授权关系、基础安全 | 认证授权测试通过 |
| P04 | MCP Server 骨架 | Streamable HTTP endpoint、6 个 tool stub、resources/prompts | MCP 工具发现 smoke 通过 |
| P05 | 多项目多环境数据源 | Hikari registry、Nacos Config 边界 | 配置加载与连接池测试通过 |
| P06 | SQL 策略与确认链路 | JSQLParser classifier、DROP 白名单、TRUNCATE confirmation | policy 单元测试通过 |
| P07 | SQL 执行与元数据能力 | `execute_sql`、`explain_sql`、`inspect_schema` | MySQL 8/5.7 Testcontainers 通过 |
| P08 | 审计闭环 | 审计事件、结果摘要、查询 API | 全链路审计测试通过 |
| P09 | 后台管理 HTML 原型 | `docs/prototypes/admin/` 高保真原型 | Playwright 截图与点击验证通过 |
| P10 | 后台管理实现 | Thymeleaf/静态资源转化原型 | 管理端端到端 smoke 通过 |
| P11 | Nacos 与运维能力 | Nacos config/discovery、Actuator、日志指标 | 配置刷新与健康检查验证通过 |
| P12 | 文档、部署与客户端手册 | 部署文档、Codex/Claude/OpenCode/Copilot 配置 | 文档 smoke 可复现 |
| P13 | 发布候选验收 | 总体验证、修复、版本化 | 0-1 验收清单全部通过 |

## P00 - 规划基线确认

阶段目标：确认当前仓库控制面、架构 spec、路线图和本地 Spring AI 源码参考路径都可用。

### P00.1 路线图自检

```text
$harness-verify

任务：验证 Refinex-DBFlow 当前规划基线可用。

任务明细：
- 读取 AGENTS.md、docs/ARCHITECTURE.md、docs/OBSERVABILITY.md、docs/PLANS.md。
- 确认 docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md 状态为 Approved。
- 确认 /Users/refinex/develop/code/spring-ai 目录存在。
- 运行 python3 scripts/check_harness.py。

硬性约束：
- 不修改业务代码；若发现文档链接或 Harness manifest 漂移，只做最小文档修复。
- 输出验证证据和下一阶段建议。

验收标准：
- Harness validator 通过。
- 明确说明 P01 可以开始或列出阻塞项。
```

## P01 - Spring Boot 工程骨架

阶段目标：建立 JDK 21 + Maven + Spring Boot 3.5.13 + Spring AI 1.1.4 + Spring Cloud 2025.0.2 + Spring Cloud Alibaba 2025.0.0.0 的可构建工程骨架。

### P01.1 Maven/Spring Boot scaffold

```text
$harness-feat

任务：创建 Refinex-DBFlow Spring Boot Maven 工程骨架。

任务明细：
- 基于当前 docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md 创建 Maven wrapper、pom.xml、src/main、src/test 基础目录。
- 使用 JDK 21；Spring Boot 3.5.13；Spring AI BOM 1.1.4；Spring Cloud BOM 2025.0.2；Spring Cloud Alibaba BOM 2025.0.0.0。
- 添加最小启动类，包名建议为 com.refinex.dbflow。
- 添加 Spring Boot test 基础测试，确保 application context 可启动。
- 更新 docs/OBSERVABILITY.md 中 build/test/run 命令。

硬性约束：
- 涉及依赖和 starter 坐标时先用 Context7 核验，并可参考 /Users/refinex/develop/code/spring-ai。
- 只建立骨架，不实现 MCP tools、数据库执行、安全业务逻辑。
- 保持单体优先，不拆 Maven multi-module。

验收标准：
- ./mvnw test 通过。
- python3 scripts/check_harness.py 通过。
- docs/OBSERVABILITY.md 包含真实可运行命令。
```

### P01.2 基础工程质量与目录约定

```text
$harness-feat

任务：建立 Java 工程基础质量约定和包结构。

任务明细：
- 创建基础包结构：common、config、security、access、mcp、sqlpolicy、executor、audit、admin、observability。
- 配置 Maven 编译参数、测试参数、编码、Java release 21。
- 增加基础异常模型、结果模型、request id/filter 或预留 observability 基础组件。
- 在 docs/ARCHITECTURE.md 中补充真实存在的源码包路径和依赖方向。

硬性约束：
- 不提前实现业务大逻辑；本阶段只建立包边界和可测试基础设施。
- 所有新增包路径必须真实存在。

验收标准：
- ./mvnw test 通过。
- docs/ARCHITECTURE.md 不再只描述目标架构，而是包含实际源码路径。
```

## P02 - 元数据库与配置模型

阶段目标：落地 DBFlow 自身元数据库、Flyway migration、JPA entity/repository，以及 YAML 配置绑定模型。

### P02.1 Flyway 与元数据库 schema

```text
$harness-feat

任务：实现 Refinex-DBFlow 元数据库 Flyway schema。

任务明细：
- 添加 Flyway、Spring Data JPA、H2 或 Testcontainers MySQL 测试依赖。
- 创建 V1 migration，包含 dbf_users、dbf_api_tokens、dbf_projects、dbf_environments、dbf_user_env_grants、dbf_confirmation_challenges、dbf_audit_events。
- 为 token 唯一 active 约束、grant 唯一约束、audit 常用查询字段建立索引。
- 添加 migration 测试，验证 schema 能在测试数据库上干净迁移。

硬性约束：
- 不存 Token 明文，只为 hash/prefix/status/last_used 等字段建模。
- 审计表默认不存完整结果集，只存摘要、行数、错误、SQL 原文和 SQL hash。

验收标准：
- ./mvnw test 通过。
- migration 测试覆盖所有核心表存在性和关键索引/约束。
```

### P02.2 JPA entity/repository 与领域服务

```text
$harness-feat

任务：实现元数据 JPA entity、repository 与基础领域服务。

任务明细：
- 为 users、api_tokens、projects、environments、user_env_grants、confirmation_challenges、audit_events 创建 entity 和 repository。
- 创建 access/audit/confirmation 基础 service，先覆盖 CRUD、状态转换和查询边界。
- 增加单元测试或 slice test，覆盖唯一 active token、授权关系查询、confirmation 状态变更、audit 插入。

硬性约束：
- entity 字段命名与 Flyway schema 保持一致。
- 不在 controller/MCP 层直接访问 repository。

验收标准：
- ./mvnw test 通过。
- 覆盖 token、grant、confirmation、audit 的核心 repository/service 测试。
```

### P02.3 YAML 配置绑定与校验

```text
$harness-feat

任务：实现 dbflow.projects、datasource-defaults、policies 的 YAML 配置绑定与校验。

任务明细：
- 创建 @ConfigurationProperties 模型，覆盖 datasource-defaults、projects/environments、dangerous-ddl policy。
- 支持 project/environment/schema/table/operation 粒度白名单配置。
- 对重复 project key、重复 environment key、缺失 jdbc-url、缺失 driver、非法 policy 配置进行启动时校验。
- 增加配置绑定测试和失败配置测试。

硬性约束：
- 数据库密码允许使用环境变量占位；不得把真实密码写入测试或文档。
- DROP TABLE、DROP DATABASE 默认 deny；TRUNCATE 默认 require-confirmation。

验收标准：
- ./mvnw test 通过。
- docs/ARCHITECTURE.md 或 docs/OBSERVABILITY.md 记录配置来源和敏感配置边界。
```

## P03 - 身份、Token 与授权模型

阶段目标：实现管理端登录基础、用户 Token 生命周期、项目环境授权，以及 MCP Bearer Token 认证基础。

### P03.1 管理员与用户安全基础

```text
$harness-feat

任务：实现 Spring Security 管理端登录与用户密码模型。

任务明细：
- 添加 Spring Security 配置，支持管理端 form login、logout、CSRF。
- 使用 BCrypt 存储管理端用户密码。
- 提供初始化管理员账号机制，优先通过环境变量或本地开发 profile。
- 增加安全测试，覆盖未登录访问管理端跳转、登录成功、CSRF 保护。

硬性约束：
- 不把管理员默认密码写死到源码或文档。
- MCP endpoint 的 Bearer Token 安全链路与管理端 session 分开设计。

验收标准：
- ./mvnw test 通过。
- 管理端安全测试覆盖成功与失败路径。
```

### P03.2 用户 Token 颁发、吊销与唯一约束

```text
$harness-feat

任务：实现用户唯一 MCP Token 生命周期。

任务明细：
- 实现 Token 生成、hash、prefix、明文只展示一次、吊销、重新颁发。
- 保证一个用户只能有 0 到 1 个 active token。
- 增加 token pepper 配置，pepper 只能从环境变量或安全配置读取。
- 增加 service 测试覆盖颁发、重复颁发、吊销、校验、last_used 更新。

硬性约束：
- Token 明文不得落库、不得写日志、不得写审计。
- Token 校验必须使用 hash 比较。

验收标准：
- ./mvnw test 通过。
- token 生命周期测试覆盖成功、失败、吊销后不可用。
```

### P03.3 项目环境授权服务

```text
$harness-feat

任务：实现用户对项目环境的授权模型与访问判断。

任务明细：
- 实现用户对 projectKey/environmentKey 的 grant 创建、删除、查询。
- 实现 AccessDecisionService，输入 user/token/project/env，输出 allow/deny 和原因。
- 将 YAML 项目环境配置与元数据库 project/environment 展示模型打通。
- 增加测试覆盖未授权、已授权、环境不存在、用户禁用、token 禁用。

硬性约束：
- 所有 MCP SQL 执行前都必须调用授权判断。
- 管理员 UI 可以展示配置项目环境，但数据库连接密码不可展示。

验收标准：
- ./mvnw test 通过。
- AccessDecisionService 测试覆盖主要拒绝原因。
```

## P04 - MCP Server 骨架

阶段目标：基于 Spring AI MCP WebMVC starter 暴露 Streamable HTTP MCP endpoint、tool/resource/prompt skeleton，并通过 MCP 客户端 smoke。

### P04.1 Spring AI MCP Streamable HTTP 配置

```text
$harness-feat

任务：接入 Spring AI MCP WebMVC server starter 并启用 Streamable HTTP。

任务明细：
- 用 Context7 核验 Spring AI 1.1.4 MCP server WebMVC starter、STREAMABLE 配置、capabilities 配置。
- 参考 /Users/refinex/develop/code/spring-ai 核验 auto-configuration 和 annotation 支持。
- 添加 spring-ai-starter-mcp-server-webmvc 依赖。
- 配置 spring.ai.mcp.server.name、version、protocol=STREAMABLE、capabilities.tool/resource/prompt。
- 建立最小 MCP health/smoke tool，证明 server 可启动。

硬性约束：
- 不把数据库执行逻辑放进本阶段。
- endpoint 路径、server name、version 写入配置和 docs/OBSERVABILITY.md。

验收标准：
- ./mvnw test 通过。
- 本地启动后可用 MCP Inspector 或测试客户端发现最小 tool。
```

### P04.2 MVP MCP tools/resources/prompts skeleton

```text
$harness-feat

任务：实现 DBFlow MCP 暴露面的 skeleton。

任务明细：
- 创建 dbflow_list_targets、dbflow_inspect_schema、dbflow_get_effective_policy、dbflow_explain_sql、dbflow_execute_sql、dbflow_confirm_sql 六个 tool 的 skeleton。
- 创建 dbflow://targets、dbflow://projects/{project}/envs/{env}/schema、dbflow://projects/{project}/envs/{env}/policy resources skeleton。
- 创建 dbflow_safe_mysql_change、dbflow_explain_plan_review prompts skeleton。
- 每个 tool 的 input schema、description、返回结构应对 AI 客户端友好。

硬性约束：
- skeleton 可以返回 mock/empty 结构，但必须通过认证上下文和授权服务接口预留边界。
- tool 名称稳定，后续不得随意改名。

验收标准：
- ./mvnw test 通过。
- tool/resource/prompt discovery 测试通过。
```

### P04.3 MCP Bearer Token 认证接入

```text
$harness-feat

任务：让 MCP endpoint 使用 DBFlow Bearer Token 认证。

任务明细：
- 为 MCP HTTP endpoint 添加 Bearer Token filter/security chain。
- 拒绝 query string token，只接受 Authorization: Bearer。
- 将 token 校验结果写入 request security context 或 DBFlow request context。
- 记录 clientInfo/User-Agent/source IP/request id 的提取策略。
- 增加认证测试：无 token、非法 token、吊销 token、合法 token。

硬性约束：
- 不依赖 MCP session 复用认证状态；每次请求都必须认证。
- Token 不写日志。

验收标准：
- ./mvnw test 通过。
- MCP endpoint 未认证返回 401，权限不足路径后续返回 403。
```

## P05 - 多项目多环境数据源与 Nacos

阶段目标：实现 YAML/Nacos 配置驱动的多项目多环境 HikariDataSource registry。

### P05.1 Hikari 数据源 registry

```text
$harness-feat

任务：实现项目环境级 HikariDataSourceRegistry。

任务明细：
- 根据 dbflow.projects[*].environments[*] 创建每个 project/env 独立 HikariDataSource。
- 共享 datasource-defaults.hikari 配置。
- 提供按 projectKey/environmentKey 获取 DataSource 的服务接口。
- 支持启动时连接校验可配置开关。
- 增加测试覆盖多项目多环境、缺失环境、配置错误、连接池关闭。

硬性约束：
- 数据源之间严格隔离，不允许 fallback 到默认数据库。
- 密码不可出现在日志和异常消息中。

验收标准：
- ./mvnw test 通过。
- Testcontainers 或 mock DataSource 测试覆盖 registry 生命周期。
```

### P05.2 Nacos Config 与服务发现边界

```text
$harness-feat

任务：接入 Spring Cloud Alibaba Nacos Config 与 Discovery 的基础配置。

任务明细：
- 用 Context7 核验 Spring Cloud Alibaba Nacos config/discovery 依赖与 spring.config.import 配置方式。
- 添加 nacos config/discovery 依赖，但默认本地开发 profile 可不依赖真实 Nacos 启动。
- 配置 Nacos data id/group/namespace 示例和 refreshEnabled 策略。
- 在 docs/OBSERVABILITY.md 添加本地 YAML 与 Nacos profile 的启动方式。
- 增加配置加载测试；真实 Nacos 集成测试可放独立 profile。

硬性约束：
- Nacos credentials 不写入仓库。
- 配置刷新不能直接摧毁当前可用连接池；后续热替换要先校验候选配置。

验收标准：
- ./mvnw test 通过。
- 本地无 Nacos 时默认测试仍可运行。
```

### P05.3 数据源热替换策略

```text
$harness-feat

任务：实现配置变更后的数据源候选校验与原子替换策略。

任务明细：
- 设计并实现 DataSourceConfigReloader。
- 新配置先绑定、校验、预热候选连接池，成功后替换 registry。
- 替换失败保留旧配置并记录告警。
- 增加测试覆盖成功替换、失败保留旧池、旧池关闭时机。

硬性约束：
- 不允许因为一条错误配置导致所有既有环境不可用。
- 替换过程要有审计或运维日志。

验收标准：
- ./mvnw test 通过。
- docs/ARCHITECTURE.md 更新配置刷新流程。
```

## P06 - SQL 策略与二次确认

阶段目标：实现 SQL 解析、风险分类、危险操作拦截、DROP 白名单与 TRUNCATE confirmation。

### P06.1 SQL parser 与风险分类

```text
$harness-feat

任务：实现 SQL 解析与风险分类服务。

任务明细：
- 引入 JSQLParser。
- 实现 SqlClassifier，输出 statementType、operation、riskLevel、targetSchema、targetTable、isDdl、isDml、parseStatus。
- 默认拒绝多语句执行。
- 解析失败的 DDL/DML 默认拒绝，SELECT/SHOW/DESCRIBE/EXPLAIN 走明确分类。
- 增加 MySQL 8 与 MySQL 5.7 常见语法测试。

硬性约束：
- 不因解析失败而放行危险 SQL。
- 分类结果必须可审计。

验收标准：
- ./mvnw test 通过。
- 分类测试覆盖 SELECT、INSERT、UPDATE、DELETE、CREATE、ALTER、DROP、TRUNCATE、GRANT、LOAD DATA。
```

### P06.2 DROP 白名单策略

```text
$harness-feat

任务：实现 DROP DATABASE 和 DROP TABLE 的 YAML 白名单策略。

任务明细：
- 实现 DangerousDdlPolicyEngine。
- DROP DATABASE、DROP TABLE 默认 deny。
- 支持 project、environment、schema、table 通配匹配。
- prod 环境即使命中白名单，也必须显式 allow-prod-dangerous-ddl=true 才允许。
- 增加测试覆盖未命中、命中、prod 缺少显式开关、通配匹配。

硬性约束：
- 拒绝结果必须包含机器可读 reason code 和人类可读 reason。
- 所有 policy decision 后续必须写审计。

验收标准：
- ./mvnw test 通过。
- DROP 相关策略测试覆盖所有核心分支。
```

### P06.3 TRUNCATE confirmation 链路

```text
$harness-feat

任务：实现 TRUNCATE 二次确认挑战。

任务明细：
- dbflow_execute_sql 遇到 TRUNCATE 时不执行，创建 confirmation challenge。
- challenge 绑定 user、token、project、environment、sql_hash、risk_level、expires_at。
- dbflow_confirm_sql 校验同一 user、同一 token、同一 project/env、同一 SQL hash 和有效期。
- 使用后立即失效，过期不可用。
- 增加测试覆盖创建、确认成功、token 不同、SQL 不同、过期、重复使用。

硬性约束：
- 不依赖 AI 客户端口头确认；确认必须由服务端 challenge 校验。
- confirmation 状态变化必须可审计。

验收标准：
- ./mvnw test 通过。
- confirmation lifecycle 测试完整。
```

## P07 - SQL 执行、EXPLAIN 与 schema inspect

阶段目标：实现核心数据库操作能力，并用 MySQL 8 与 MySQL 5.7 集成测试保护。

### P07.1 SQL execution engine

```text
$harness-feat

任务：实现受控 SQL 执行引擎。

任务明细：
- 实现 SqlExecutionService，串联 token context、授权、classifier、policy、confirmation、DataSourceRegistry、audit。
- 支持 SELECT/SHOW/DESCRIBE/EXPLAIN 查询类返回限流结果。
- 支持 DML/DDL 返回 affected rows、warning、duration、statement summary。
- 设置 query timeout、max rows、fetch size。
- 增加 MySQL 8 Testcontainers 集成测试。

硬性约束：
- 所有执行路径都必须先授权、再策略、再执行、最后审计。
- 查询结果默认截断，不能无限返回。

验收标准：
- ./mvnw test 通过。
- MySQL 8 测试覆盖 SELECT、INSERT、UPDATE、DELETE、CREATE TABLE、ALTER TABLE。
```

### P07.2 EXPLAIN 与索引建议基础

```text
$harness-feat

任务：实现 dbflow_explain_sql 和执行计划结果结构化。

任务明细：
- 对 SELECT 和可 explain 的 DML 支持 EXPLAIN。
- MySQL 8 优先使用 EXPLAIN FORMAT=JSON；MySQL 5.7 提供兼容输出。
- 返回表、type、key、rows、filtered、Extra、JSON plan 摘要。
- 提供基础索引建议提示，不做 LLM 判断。
- 增加 MySQL 8/5.7 explain 集成测试。

硬性约束：
- explain_sql 不实际执行目标 DML。
- 返回结果对 MCP 客户端友好，字段稳定。

验收标准：
- ./mvnw test 通过。
- explain 测试覆盖有索引、无索引、语法错误、未授权环境。
```

### P07.3 Schema inspect 能力

```text
$harness-feat

任务：实现 dbflow_inspect_schema 与 schema resource。

任务明细：
- 基于 information_schema 查询库、表、字段、索引、视图、存储过程、函数元数据。
- 支持按 schema/table 过滤。
- 返回字段类型、nullable、default、comment、index 信息。
- 对大 schema 做分页或上限控制。
- 增加 MySQL 8/5.7 集成测试。

硬性约束：
- inspect_schema 必须先检查 project/env 授权。
- 不返回数据库密码、连接串等敏感配置。

验收标准：
- ./mvnw test 通过。
- schema inspect 测试覆盖表、字段、索引、存储过程/函数基础查询。
```

## P08 - 审计闭环

阶段目标：把所有 MCP 操作、策略拒绝、确认、执行成功/失败都落库，并提供后端查询能力。

### P08.1 Audit event writer

```text
$harness-feat

任务：实现统一审计事件写入器。

任务明细：
- 实现 AuditEventWriter，覆盖 request received、policy denied、requires confirmation、executed、failed、confirmation expired。
- 捕获 user_id、token_id、client_name、client_version、user_agent、source_ip、project/env、tool、operation、risk、decision、sql_text、sql_hash、result_summary。
- 确保异常路径也写入审计。
- 增加测试覆盖成功、拒绝、异常、确认场景。

硬性约束：
- 不记录 Token 明文。
- result_summary 不保存完整结果集。

验收标准：
- ./mvnw test 通过。
- 审计字段测试验证核心字段不为空。
```

### P08.2 Audit query API

```text
$harness-feat

任务：实现管理端审计查询后端 API/service。

任务明细：
- 提供按时间、用户、项目、环境、risk、decision、SQL hash、tool 过滤的查询服务。
- 支持分页和排序。
- 支持查看单条审计详情。
- 增加 repository/service/controller 测试。

硬性约束：
- 只有管理员能查询全量审计；普通用户后续仅可查自己的记录。
- 审计详情不展示敏感 token 或数据库密码。

验收标准：
- ./mvnw test 通过。
- audit query 测试覆盖过滤、分页、详情和权限。
```

## P09 - 后台管理 HTML 原型设计

阶段目标：用 `huashu-design` 在 `docs/prototypes/admin/` 设计完整后台管理高保真 HTML 原型，为后续 Thymeleaf/静态资源实现提供一比一依据。

### P09.1 后台信息架构与页面清单

```text
$huashu-design

任务：为 Refinex-DBFlow 后台管理端设计信息架构和页面清单。

任务明细：
- 先读 docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md、docs/ARCHITECTURE.md。
- 面向企业内网管理后台，风格参考 Nacos 这类安静、实用、高密度运维工具，不做营销页。
- 输出页面清单：登录、总览、用户管理、Token 管理、项目环境授权、配置查看、危险策略、审计列表、审计详情、系统健康。
- 明确每个页面的主任务、关键字段、主要交互和空/错/加载状态。
- 将信息架构写入 docs/prototypes/admin/README.md。

硬性约束：
- 不做生产代码，只做设计资产。
- 不使用装饰性 landing page，不做大 hero。
- 管理后台优先密度、可扫描、可重复操作。

验收标准：
- docs/prototypes/admin/README.md 存在且页面清单完整。
- 页面清单覆盖用户、授权、Token、审计、配置、健康状态。
```

### P09.2 高保真 HTML 原型

```text
$huashu-design

任务：设计 Refinex-DBFlow 后台管理高保真 HTML 原型。

任务明细：
- 基于 docs/prototypes/admin/README.md 设计可打开的 HTML 原型。
- 产物放在 docs/prototypes/admin/index.html，必要资源放同目录 assets/。
- 做 overview 平铺或可切换页面导航，至少覆盖登录、总览、用户管理、项目授权、Token、危险策略、审计列表、审计详情、健康状态。
- 使用真实业务字段和合理示例数据，不编造无意义 stats。
- 为关键交互提供点击状态：筛选、查看详情、颁发 Token、吊销 Token、授权环境、查看拒绝原因。

硬性约束：
- 使用 HTML 原型，不写生产后端。
- 避免 AI slop：不使用紫色渐变、emoji 图标、装饰性卡片堆叠。
- 固定管理后台为高密度、克制、专业风格。

验收标准：
- docs/prototypes/admin/index.html 可直接打开。
- Playwright 截图无明显布局错位。
- 关键点击路径至少通过一次浏览器验证。
```

### P09.3 原型专家评审与修订

```text
$huashu-design

任务：评审并修订 Refinex-DBFlow 后台管理原型。

任务明细：
- 按 huashu-design 的专家评审方式，从信息架构、视觉层级、操作效率、状态完整性、企业可信度五个维度打分。
- 修复评分中影响落地的严重问题。
- 在 docs/prototypes/admin/README.md 追加原型使用说明、页面路由映射、后续转 Thymeleaf 的组件清单。

硬性约束：
- 评审设计，不评审实现。
- 修订后再次截图验证。

验收标准：
- 原型评分和修复记录写入 README。
- 管理端实现阶段可以直接按该原型拆模板。
```

## P10 - 后台管理实现

阶段目标：把 P09 HTML 原型一比一转化为 Spring MVC + Thymeleaf + 静态 CSS/JS 的管理后台。

### P10.1 Thymeleaf 管理端基础布局

```text
$harness-feat

任务：将后台原型转化为 Thymeleaf 管理端基础布局。

任务明细：
- 读取 docs/prototypes/admin/index.html 和 docs/prototypes/admin/README.md。
- 采用 Spring MVC + Thymeleaf + 静态 CSS/JS，不引入独立前端 SPA。
- 实现登录页、基础布局、导航、顶部状态、统一表格/筛选/详情布局。
- 保持原型的信息密度、布局比例、文案层级和关键状态。

硬性约束：
- 不引入 Node/Vite 构建链，除非后续单独批准。
- 后台必须与 Spring Security form login 集成。

验收标准：
- ./mvnw test 通过。
- 本地启动后管理端基础页面可访问，未登录会跳转登录页。
```

### P10.2 用户、Token、授权页面

```text
$harness-feat

任务：实现管理端用户、Token、项目环境授权页面。

任务明细：
- 实现用户列表、创建/禁用用户。
- 实现 Token 颁发、明文只展示一次、吊销、重新颁发。
- 实现项目环境授权和撤销。
- 增加 controller/service/security 测试。

硬性约束：
- Token 明文只在颁发成功页面展示一次。
- 页面不能展示数据库密码或 token hash。

验收标准：
- ./mvnw test 通过。
- 管理端 smoke 覆盖创建用户、授权环境、颁发和吊销 Token。
```

### P10.3 审计、策略、健康页面

```text
$harness-feat

任务：实现管理端审计、危险策略、系统健康页面。

任务明细：
- 实现审计列表和审计详情页，支持过滤、分页、查看拒绝原因。
- 实现危险策略查看页，展示 DROP 白名单、TRUNCATE confirmation 策略和 prod 强化规则。
- 实现系统健康页，展示元数据库、项目环境连接池、Nacos、MCP endpoint 状态。
- 增加测试覆盖权限、分页、过滤和页面渲染。

硬性约束：
- 策略页 MVP 只读展示；配置来源仍以 YAML/Nacos 为准。
- 审计详情脱敏敏感信息。

验收标准：
- ./mvnw test 通过。
- 管理端端到端 smoke 覆盖审计查询和健康状态。
```

## P11 - 运维、观测与安全加固

阶段目标：补齐 Actuator、结构化日志、指标、rate limit、Origin 校验、错误处理和安全基线。

### P11.1 Actuator 与健康检查

```text
$harness-feat

任务：实现 DBFlow 运维健康检查和指标。

任务明细：
- 接入 Spring Boot Actuator。
- 自定义 health indicator：metadata database、target datasource registry、Nacos config/discovery、MCP endpoint readiness。
- 添加 Micrometer 指标：MCP 调用次数、SQL 风险分布、拒绝次数、执行耗时、确认挑战数量。
- 更新 docs/OBSERVABILITY.md。

硬性约束：
- Actuator 暴露面最小化，敏感详情默认不公开。
- 管理端健康页复用 health service，不重复实现。

验收标准：
- ./mvnw test 通过。
- 本地启动后 health 和 metrics 可按配置访问。
```

### P11.2 MCP HTTP 安全加固

```text
$harness-feat

任务：加固 MCP Streamable HTTP endpoint 安全。

任务明细：
- 实现 Origin 校验，局域网部署允许配置可信来源。
- 实现 request size limit、rate limit 或基础限流策略。
- 统一错误返回：401、403、策略拒绝、SQL 执行失败、确认过期、结果截断。
- 增加安全测试覆盖 Origin、Token、权限、限流。

硬性约束：
- 不接受 query string token。
- 不向客户端返回内部堆栈、数据库密码、连接串。

验收标准：
- ./mvnw test 通过。
- 安全测试覆盖主要攻击/误用路径。
```

### P11.3 日志与故障排查手册

```text
$harness-feat

任务：建立结构化日志和故障排查文档。

任务明细：
- 为 MCP 请求、SQL 执行、策略拒绝、配置刷新、数据源替换加入 requestId/traceId。
- 编写 docs/runbooks/troubleshooting.md，覆盖启动失败、Nacos 配置失败、数据库连接失败、Token 无效、MCP 客户端连不上。
- 更新 docs/OBSERVABILITY.md 的日志和排查入口。

硬性约束：
- 日志不输出 Token 明文、数据库密码。
- 故障排查步骤必须可执行。

验收标准：
- ./mvnw test 通过。
- runbook 包含至少 8 个常见故障场景。
```

## P12 - 部署文档与用户手册

阶段目标：让企业用户和员工能部署服务、配置客户端、理解权限和审计。

### P12.1 部署文档

```text
$harness-feat

任务：编写并验证 Refinex-DBFlow 部署文档。

任务明细：
- 编写 docs/deployment/README.md。
- 覆盖 JDK 21、构建 jar、配置元数据库、配置 MySQL 项目环境、配置 Nacos、本地 YAML、启动参数、反向代理/TLS、内网访问限制。
- 提供 application-dbflow-example.yml，不包含真实密码。
- 提供 Dockerfile 或说明为什么 MVP 暂不提供容器化。

硬性约束：
- 所有命令必须实际可运行或明确前置条件。
- 不把真实密钥写入仓库。

验收标准：
- ./mvnw test 通过。
- 按文档能从空环境启动本地开发实例。
```

### P12.2 MCP 客户端配置手册

```text
$harness-feat

任务：编写 Codex、Claude、OpenCode、Copilot 等 MCP 客户端配置与使用手册。

任务明细：
- 编写 docs/user-guide/mcp-clients.md。
- 覆盖 Streamable HTTP URL、Authorization Bearer Token、局域网访问、常见错误。
- 分别提供 Codex、Claude、OpenCode、Copilot 的配置样例；如果某客户端配置方式版本敏感，先查官方文档或标注需按客户端版本核验。
- 提供首次 smoke prompt：list targets、inspect schema、explain sql、execute select、TRUNCATE confirmation、DROP 被拒绝。

硬性约束：
- 不虚构客户端不支持的配置字段；不确定时必须查官方资料或明确写“按客户端版本核验”。
- Token 示例必须是假值。

验收标准：
- 文档包含每个客户端的配置片段和首用验证 prompt。
- 用户能按文档完成一次只读查询 smoke。
```

### P12.3 管理员与员工使用手册

```text
$harness-feat

任务：编写管理员手册和员工使用手册。

任务明细：
- 编写 docs/user-guide/admin-guide.md，覆盖登录、创建用户、授权项目环境、颁发/吊销 Token、查看审计、查看策略、排查连接。
- 编写 docs/user-guide/operator-guide.md，覆盖员工如何申请 Token、配置 MCP、选择 project/env、执行查询、处理确认、理解拒绝原因。
- 编写 docs/user-guide/security-and-audit.md，解释 AI 操作数据库不黑盒的审计价值。

硬性约束：
- 文档语言清晰专业，中文正文，英文技术名词保留。
- 不只写安装步骤，必须有 first-use smoke test。

验收标准：
- 三份用户文档都存在。
- 文档中的命令和路径与实际项目一致。
```

## P13 - 发布候选与总体验收

阶段目标：完成从空仓库到可部署、可使用、可审计、可演示的 0-1 闭环。

### P13.1 全链路验收测试

```text
$harness-verify

任务：执行 Refinex-DBFlow 0-1 全链路验收。

任务明细：
- 运行 python3 scripts/check_harness.py。
- 运行 ./mvnw test。
- 运行 MySQL 8 与 MySQL 5.7 集成测试。
- 本地启动服务，使用 MCP 客户端或 MCP Inspector 完成 list targets、inspect schema、explain、select、TRUNCATE confirmation、DROP reject。
- 管理端完成登录、创建用户、授权、颁发 token、查看审计。

硬性约束：
- 验收失败不得宣称 ready。
- 每个失败项记录到 docs/exec-plans/tech-debt-tracker.md 或创建 harness-fix 计划。

验收标准：
- 全部自动化测试通过。
- 手工 smoke 全部通过。
- 审计表能看到允许、拒绝、确认、失败样例。
```

### P13.2 安全与代码审查

```text
$harness-review

任务：对 Refinex-DBFlow 0-1 版本做安全优先的代码审查。

任务明细：
- 按 security > correctness > performance > readability 顺序审查当前 diff 和核心实现。
- 重点检查 Token 明文泄漏、数据库密码泄漏、未授权 SQL 执行、策略绕过、TRUNCATE confirmation 绕过、DROP 白名单误放行、审计漏记。
- 输出 findings，逐条决定修复或记录风险。

硬性约束：
- 不只做风格审查。
- 发现安全问题必须先修复再发布。

验收标准：
- 高危与中危安全 finding 清零或有明确书面风险接受。
- 修复后重新运行 harness-verify。
```

### P13.3 版本化发布准备

```text
$harness-finish

任务：完成 Refinex-DBFlow 0-1 发布候选收尾。

任务明细：
- 确认所有 active execution plans 已完成或归档。
- 更新 README.md，说明项目定位、快速启动、文档入口、MCP 客户端入口。
- 更新 docs/PLANS.md，将 0-1 阶段状态标记清楚。
- 准备中文 commit/PR 描述或 release notes。

硬性约束：
- 先通过 harness-verify，再进入 finish。
- 不做破坏性清理。

验收标准：
- 工作区干净。
- release notes 能说明功能、部署方式、客户端配置、安全审计能力和已知限制。
```

## 最终 0-1 Done Definition

Refinex-DBFlow 0-1 完成必须同时满足：

- Spring Boot 服务可构建、可测试、可本地启动。
- MCP Streamable HTTP endpoint 可被至少一个真实 MCP 客户端或 MCP Inspector 调用。
- Token 认证、用户授权、项目环境隔离生效。
- `DROP DATABASE`、`DROP TABLE` 默认拒绝，仅 YAML 白名单放行。
- `TRUNCATE` 必须二次确认后执行。
- MySQL 8 作为主路径测试通过，MySQL 5.7 核心兼容测试通过。
- 审计记录覆盖允许、拒绝、确认、失败。
- 后台管理端覆盖用户、Token、授权、策略、审计、健康状态。
- 部署文档和用户手册覆盖 Codex、Claude、OpenCode、Copilot 等客户端配置。
- `python3 scripts/check_harness.py` 与 `./mvnw test` 均通过。
