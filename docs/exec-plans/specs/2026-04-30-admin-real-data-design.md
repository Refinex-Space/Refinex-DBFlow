<!-- Harness brainstorm spec. Safe to edit before approval. -->

# Admin 页面真实数据接入设计

Date: 2026-04-30

Status: Approved

## 背景

`/admin` 管理端已经完成 Spring MVC + Thymeleaf 基础布局，并且用户、MCP Token、项目环境授权、审计列表、审计详情、危险策略、系统健康等页面已经陆续接入真实服务。

当前仍存在明显原型残留：

- `/admin` 总览页的指标卡、最近审计事件、需要关注事项来自 `AdminHomeController` 中的硬编码样例。
- `/admin/config` 配置查看页来自 `AdminHomeController` 中的硬编码样例。
- 管理端共享顶栏中的管理员名、配置源、MCP 状态是固定文本。
- 共享布局中的“状态样例”抽屉是原型演示内容，不对应真实运行状态。
- 总览页环境下拉包含 `billing-core / prod`、`risk-lab / staging` 等演示环境。

这些残留会让管理端看起来像模拟数据页面，削弱 DBFlow “可审计、可运维、可落地”的产品可信度。

## 目标

- 将 `/admin` 第一屏可见数据全部改为真实运行时数据。
- 将 `/admin/config` 改为展示脱敏后的真实项目环境配置。
- 将共享顶栏改为展示当前登录管理员、真实 MCP endpoint 状态、真实配置来源状态。
- 移除或替换“状态样例”抽屉，不再向用户展示原型状态说明。
- 保持现有 Spring MVC + Thymeleaf + Spring Security form login 架构，不引入独立 SPA 或 Node/Vite 构建链。
- 保持管理端页面不泄露 Token hash、完整 Token 明文、password hash、数据库密码或完整 JDBC URL。

## 非目标

- 不新增管理端角色模型或权限 schema。
- 不新增前端 JSON API 作为主要数据通道。
- 不实现 WebSocket、SSE 或自动轮询刷新。
- 不修改 MCP tool 协议、SQL 执行策略、审计写入语义。
- 不新增配置写入能力；`/admin/config` 仍为只读展示。
- 不把目标数据库业务探活变成页面加载的强依赖。

## 现状依据

已接入真实服务的页面能力：

- `/admin/users`、`/admin/grants`、`/admin/tokens` 已通过 `AdminAccessManagementService` 读取和写入元数据库。
- `/admin/audit`、`/admin/audit/{eventId}` 已通过 `AdminOperationsViewService` 和 `AuditQueryService` 读取真实审计数据。
- `/admin/policies/dangerous` 已通过 `DbflowProperties` 展示生效高危 DDL 策略。
- `/admin/health` 已通过 `DbflowHealthService` 展示元数据库、目标连接池、Nacos、MCP endpoint 状态。

仍需改造的主要位置：

- `src/main/java/com/refinex/dbflow/admin/AdminHomeController.java`
- `src/main/resources/templates/admin/overview.html`
- `src/main/resources/templates/admin/config.html`
- `src/main/resources/templates/admin/fragments/layout.html`
- `src/test/java/com/refinex/dbflow/admin/*`

## 方案比较

### 方案 A：服务端聚合视图模型

新增或扩展管理端 view service，由服务端聚合审计、Token、授权、配置、健康和当前认证上下文，再将结构化视图传给 Thymeleaf 模板。

优点：

- 最符合当前 Spring MVC + Thymeleaf 架构。
- 权限、脱敏、统计口径和空状态都留在服务端，安全边界清晰。
- 可以复用已有 repository、`AuditQueryService`、`DbflowHealthService`、`ProjectEnvironmentCatalogService`。
- 页面测试可继续用 MockMvc 覆盖。

风险：

- 需要为统计查询补少量 repository 方法或 specification，避免在内存中过滤大量数据。
- 首页聚合多个服务，必须控制查询上限和避免慢健康探测。

结论：采用。

### 方案 B：新增管理端 JSON API，由前端 JS 拉取

为总览、配置和顶栏新增 JSON API，页面加载后由 `admin.js` 请求并渲染。

优点：

- 后续局部刷新更方便。
- 可以减少首屏模板模型复杂度。

风险：

- 引入额外 API 权限、CSRF/CORS、错误处理和前端渲染分支。
- 当前管理端不是 SPA，此方案会让简单页面复杂化。
- MockMvc 模板测试之外还需要更多 JS 行为测试。

结论：不作为本阶段主方案。后续确实需要实时刷新时再设计 API。

### 方案 C：直接在 Controller 中拼真实数据

在 `AdminHomeController` 中直接注入更多 repository 和 service，替换硬编码方法。

优点：

- 改动文件少。
- 能快速消除模拟数据。

风险：

- Controller 会承担统计、脱敏、配置解析和健康聚合职责，后续难维护。
- 与现有 `AdminAccessManagementService` / `AdminOperationsViewService` 分层风格不一致。

结论：拒绝。Controller 只负责路由和模型接线。

## 设计决策

采用方案 A：服务端聚合视图模型。

### 视图服务边界

新增 `AdminOverviewViewService`，负责 `/admin` 总览页：

- 读取最近 24 小时审计统计。
- 读取最近审计事件摘要。
- 读取 pending confirmation 数量。
- 读取 active Token、7 天内过期 Token 数量。
- 读取 active 授权数量。
- 读取配置项目环境数量和生产环境数量。
- 读取 `DbflowHealthService` 健康快照，形成异常数据源和系统状态摘要。
- 生成真实关注事项列表。
- 生成真实环境筛选下拉选项。

扩展 `AdminOperationsViewService` 的配置页能力，负责 `/admin/config`：

- 从 `ProjectEnvironmentCatalogService.listConfiguredEnvironments()` 读取配置项目环境。
- 展示项目、环境、驱动、数据库类型、脱敏 host、脱敏端口、脱敏 schema、用户名、连接池限制和元数据库同步状态。
- 不展示完整 JDBC URL、数据库密码、连接串 query 参数或 secret。
- 当没有配置项目环境时，展示真实空状态：当前未配置 `dbflow.projects`。

新增 `AdminShellViewService`，负责管理端共享顶栏模型：

- 管理员名来自 `Authentication.getName()`，而不是固定 `admin.refinex`。
- MCP 状态来自 `DbflowHealthService.mcpEndpointReadiness()`。
- 配置来源来自 Spring Environment 中的 Nacos Config/Discovery 开关和 namespace；Nacos 关闭时显示本地配置。
- 不展示“状态样例”按钮和抽屉。

### 总览页数据口径

总览页默认展示最近 24 小时数据，除非指标天然是当前状态。

指标卡：

| 指标       | 口径                                                                                               | 来源                                                            |
|----------|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| SQL 请求   | 最近 24 小时审计事件数量，优先统计 tool 为 `dbflow_execute_sql`、`dbflow_explain_sql`、`dbflow_inspect_schema` 的事件 | `DbfAuditEventRepository` 统计方法                                |
| 策略拒绝     | 最近 24 小时 decision/status 为 `POLICY_DENIED`、`DENIED`、`FAILED` 中属于策略拒绝的数量                          | 审计数据                                                          |
| 待确认      | 当前 `PENDING` confirmation challenge 数量                                                           | `DbfConfirmationChallengeRepository.countByStatus("PENDING")` |
| 有效 Token | 当前 `ACTIVE` Token 数量，并提示 7 天内过期数量                                                                | `DbfApiTokenRepository`                                       |
| 已授权环境    | 当前 active grant 数量，并提示配置环境/生产环境数量                                                                | `DbfUserEnvGrantRepository` + 配置目录                            |
| 异常数据源    | 当前健康快照中 `target-pool` 且状态为 `DEGRADED` 或 `DOWN` 的数量                                               | `DbflowHealthService`                                         |

最近审计事件：

- 默认取最近 5 条审计摘要。
- 行字段复用审计列表页已脱敏视图，不直接渲染审计实体。
- 详情链接使用真实审计事件主键。
- 没有审计事件时展示真实空状态，不展示样例行。

需要关注：

- 由真实条件生成，最多展示 5 条。
- 有策略拒绝时链接到 `/admin/audit?decision=POLICY_DENIED`。
- 有待确认 challenge 时链接到 `/admin/audit?decision=REQUIRES_CONFIRMATION`。
- 有 `DEGRADED` / `DOWN` 健康项时链接到 `/admin/health`。
- 有 7 天内过期 active Token 时链接到 `/admin/tokens?status=ACTIVE`。
- 无关注事项时展示“当前无需要关注的运行时事项”，不展示原型样例。

环境下拉：

- 选项来自配置项目环境。
- 默认保留“全部环境”。
- 无配置环境时只展示“全部环境”，并在页面空状态中提示配置来源为空。
- 本阶段下拉只展示真实选项，不承担筛选动作；后续需要筛选时另行设计 query 参数和统计口径。

### 配置页数据口径

配置页只展示安全字段：

| 字段    | 规则                                                           |
|-------|--------------------------------------------------------------|
| 项目    | `projectKey` / `projectName`                                 |
| 环境    | `environmentKey` / `environmentName`                         |
| 数据源   | 使用 `projectKey/environmentKey` 或 Hikari pool name 摘要，不展示 URL |
| 类型    | 从 driver class 或 JDBC scheme 推导，如 `mysql`、`h2`、`unknown`     |
| 主机    | JDBC host；解析失败显示 `-`                                         |
| 端口    | JDBC port；解析失败显示 `-`                                         |
| 库名    | JDBC database/schema；解析失败显示 `-`                              |
| 用户名   | 生效 username；为空显示 `-`                                         |
| 连接池限制 | maximumPoolSize、minimumIdle、connectionTimeout 等共享配置摘要        |
| 同步状态  | 元数据库 project/environment 是否存在                                |

脱敏规则：

- 不展示完整 JDBC URL。
- 不展示 password、query string、credentials。
- 解析异常时展示 `解析失败` 或 `-`，并避免把原始 URL 写入页面。
- 配置来源显示为 `Nacos enabled namespace=<namespace>` 或 `Local application config`，不展示 Nacos 用户名、密码或 server
  address 中的凭据。

### 模板改造

`overview.html`：

- 将 `${metrics}`、`${auditRows}`、`${attentionItems}` 替换为单一 `${overview}` 视图。
- 空状态由 `${overview.recentAuditRows}`、`${overview.attentionItems}` 是否为空决定。
- 删除硬编码环境 option。

`config.html`：

- 将 `${configs}` 替换为 `${configPage}`。
- 删除固定 `source=nacos:dbflow-targets-prod.yml`。
- 使用真实配置来源和空状态。

`fragments/layout.html`：

- 删除“状态样例”按钮和 `stateDrawer`。
- 顶栏字段来自 `${shell}` 或共享模型。
- 顶栏搜索若保留，必须只提交审计页已支持的字段；当前 `q` 参数没有后端语义，改为 `sqlHash` 或移除全局搜索，避免假功能。

### 安全与权限

- 所有页面继续受 `ROLE_ADMIN` 保护。
- 所有数据仍只在管理端 Thymeleaf 页面中渲染，不新增公开 API。
- 不展示 `tokenHash`、`passwordHash`、完整 Token、数据库密码、完整 JDBC URL。
- 最近审计事件和详情仍复用已脱敏 DTO。
- 页面统计不从 request 参数拼接 SQL，不访问目标业务库执行额外探测。

### 性能边界

- 总览页统计查询必须有时间范围和数量上限。
- 最近审计只取 5 条。
- 关注事项最多 5 条。
- 健康项复用 `DbflowHealthService.snapshot()`；不新增目标库业务 SQL 探测。
- Token、grant、environment 当前规模较小时可直接 repository 统计；如实现中发现已有 repository 不支持计数，应优先新增 count
  方法，而不是拉全量再内存过滤。

## 验收标准

- [ ] `/admin` 页面不再包含 `billing-core`、`risk-lab`、`admin.refinex`、`状态样例` 等原型演示文本，除非它们来自当前真实配置或当前登录用户。
- [ ] `/admin` 指标卡、最近审计、关注事项、环境下拉均来自真实服务或真实空状态。
- [ ] `/admin/config` 展示真实脱敏配置，且不包含完整 JDBC URL、数据库密码、Token hash、password hash。
- [ ] 共享顶栏展示当前登录管理员和真实 MCP/config 状态。
- [ ] 无审计事件、无配置环境、无关注事项时页面展示真实空状态，不展示样例行。
- [ ] 非管理员访问 `/admin`、`/admin/config` 仍被拒绝。
- [ ] 测试覆盖真实数据渲染、空状态、脱敏、权限和原型文本消失。
- [ ] `./mvnw test`、`python3 scripts/check_harness.py`、`git diff --check` 通过。

## 测试策略

- 增加 `AdminOverviewRealDataControllerTests` 或扩展 `AdminUiControllerTests`：
    - 写入真实审计事件后访问 `/admin`，断言最近审计和统计来自写入数据。
    - 不写入审计事件时访问 `/admin`，断言空状态出现且样例审计文本不存在。
    - 创建即将过期 active Token 后访问 `/admin`，断言 Token 过期关注项来自真实数据。
    - 使用普通用户访问 `/admin`，断言 403。
- 增加或扩展配置页测试：
    - 配置 `dbflow.projects` 后访问 `/admin/config`，断言项目/环境/host/schema/username 出现。
    - 断言完整 JDBC URL、password、query string 不出现在响应中。
    - 空配置启动时访问 `/admin/config`，断言真实空状态。
- 增加共享布局测试：
    - 使用 `user("actual-admin").roles("ADMIN")` 访问页面，断言顶栏展示 `actual-admin`。
    - 断言响应不包含 `状态样例`、`admin.refinex`、无语义 `q` 搜索参数。

## 实施边界

推荐后续 `harness-plan` 将实现拆为 4 个步骤：

1. 增加总览和配置视图服务，补 repository 统计方法。
2. 接线 `AdminHomeController` 和共享 shell 模型。
3. 改造 `overview.html`、`config.html`、`fragments/layout.html`，移除原型状态抽屉和假搜索。
4. 补充 MockMvc/service 测试，执行 Maven、Harness 和 diff 验证。

## 风险与缓解

| 风险             | 影响         | 缓解                                           |
|----------------|------------|----------------------------------------------|
| 首页聚合查询过多导致变慢   | 管理端首屏体验下降  | 对审计查询使用最近 24 小时和 limit；统计优先 repository count |
| 配置页误泄露连接串或密码   | 安全事故       | 页面模型只暴露脱敏字段；测试断言完整 URL/password 不出现          |
| 原型文本来自真实配置时被误判 | 测试脆弱       | 测试使用随机项目 key，并只禁止固定样例文本在默认场景出现               |
| 健康快照触发目标库连接等待  | 页面打开变慢     | 复用现有健康服务，不新增目标业务 SQL 探测                      |
| 顶栏搜索当前没有后端语义   | 用户误以为可全文搜索 | 改为已支持的审计字段或移除全局搜索                            |

## 采纳与拒绝

采纳：

- 服务端聚合 view service。
- Thymeleaf 服务端渲染真实数据。
- 真实空状态替代样例数据。
- 删除原型状态抽屉和无语义假搜索。

拒绝：

- 为本阶段新增 SPA 或 JSON API。
- 在 Controller 中继续堆业务聚合逻辑。
- 为了健康展示主动执行目标业务 SQL。
- 在页面展示完整 JDBC URL 或任何 secret-like 字段。
