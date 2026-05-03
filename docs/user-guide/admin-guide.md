# DBFlow 管理员手册

本文档面向 Refinex-DBFlow 管理员，说明如何通过管理端完成用户、授权、Token、审计、策略和连接排查。MCP
客户端配置细节见 [mcp-clients.md](mcp-clients.md)，部署与外部配置见
[docs/deployment/README.md](../deployment/README.md)。

## 1. 管理端入口

| 场景             | 地址                                      | 说明                                  |
|----------------|-----------------------------------------|-------------------------------------|
| 本地后端管理端        | `http://127.0.0.1:8080/login`           | Spring Security 登录入口。               |
| 本地 React 管理端开发 | `http://127.0.0.1:5173/admin`           | 前端 dev server，代理 `/admin/api/**`。   |
| 打包后 React 管理端  | `http://127.0.0.1:8080/admin`           | 需要用 `react-admin` Maven profile 打包。 |
| 内网部署示例         | `https://dbflow.internal.example/login` | 反向代理后仍先走同一登录入口。                     |
| 旧 Thymeleaf 后台 | `http://127.0.0.1:8080/admin-legacy`    | 仅用于过渡期排障，仍要求管理员登录。                  |

登录后默认进入 `/admin` React 总览页。管理端使用 Spring Security form login，`/admin/**` 和
`/admin-legacy/**` 共享同一个管理员 session；`/admin/api/**` 是 React 管理端使用的 JSON API，写操作必须携带
Spring Security CSRF token。`/mcp` 不使用管理端 session，而是每次请求都要求
`Authorization: Bearer <DBFlow Token>`。

初始管理员账号应来自外部配置或密钥系统，例如 `dbflow.admin.initial-user.*`。不要把真实密码、BCrypt hash、Token
pepper 或数据库密码写入仓库。

## 2. 本地开发启动

后端：

```bash
./mvnw spring-boot:run
```

前端：

```bash
pnpm --dir dbflow-admin dev
```

开发态访问 `http://127.0.0.1:5173/admin`。Vite 会把 `/admin/api/**`、`/login`、`/logout`、`/actuator`
代理到 `http://localhost:8080`，因此后端必须先启动并完成登录 session 建立。

打包验证：

```bash
./mvnw -Preact-admin -DskipTests package
```

该命令会安装/构建 `dbflow-admin`，并把 `dbflow-admin/dist/**` 复制进 Spring Boot jar 的 `static/admin/`。

## 3. First-use Smoke Test

本地开发环境首次验证后端：

```bash
curl -s http://127.0.0.1:8080/actuator/health
```

预期：

- 返回有限的 Actuator health 状态，默认不展示敏感详情。
- 浏览器访问 `http://127.0.0.1:8080/login` 能看到 DBFlow 管理端登录页。
- 登录后访问 `/admin/health` 能看到 React 管理端的系统健康页。
- 登录后访问 `/admin-legacy/health` 能看到旧 Thymeleaf 健康页，用于过渡期排障。
- 访问 `/admin/audit` 能看到审计列表页；没有记录时应显示空状态，而不是报错。

## 4. 登录、CSRF 与 Session

- `/login` 仍是统一登录入口，支持普通 Thymeleaf form login，也支持 React admin 的 JSON/XHR 登录。
- 管理端 session 存在服务端，浏览器只保存标准 session cookie；React admin 不在 localStorage 保存登录状态。
- Spring Security 会下发浏览器可读的 `XSRF-TOKEN` cookie。React admin API client 会在 mutation 请求中把它复制到
  `X-XSRF-TOKEN` header。
- `/admin/api/**` 缺少 CSRF token 的写请求会被拒绝；Thymeleaf `/admin-legacy/**` 表单继续使用隐藏 `_csrf`
  字段。
- `/mcp` 与管理端 session 完全分离，必须使用 MCP Bearer Token。

## 5. `/admin` 与 `/admin-legacy` cutover 状态

当前 cutover 策略：

- `/admin` 是 React 管理端正式入口，登录成功默认跳转到这里。
- `/admin/api/**` 保持 JSON API，不被 SPA fallback 捕获。
- `/admin-legacy/**` 是短期保留的旧 Thymeleaf 后台，仅用于过渡期排障，仍要求 `ROLE_ADMIN`。
- 稳定一个版本后再评估删除旧模板和 `/admin-assets/**`。

## 6. 创建和禁用用户

页面：`/admin/users`

主任务：

- 创建可使用 DBFlow 的内部用户。
- 禁用离职、转岗或临时冻结的用户。
- 按 username、status 过滤用户列表。

关键字段：

| 字段          | 说明                                       |
|-------------|------------------------------------------|
| username    | 登录名或员工唯一标识。建议与企业账号一致。                    |
| displayName | 管理端显示名，便于审计阅读。                           |
| password    | 管理端登录密码；生产环境建议由外部身份源或密钥流程管理。             |
| status      | `ACTIVE` 或禁用状态。禁用后应同时评估 Token 和授权是否需要撤销。 |

操作步骤：

1. 打开 `/admin/users`。
2. 点击创建用户，填写 username、displayName 和临时密码。
3. 创建后确认用户出现在列表中。
4. 用户离岗或权限不再需要时，使用禁用操作。

注意：

- 禁用用户不会成为保留 Token 的理由。高风险场景下应同步吊销该用户 Token。
- 管理端列表不展示 password hash。

## 7. 授权项目环境

页面：`/admin/grants`

主任务：

- 给用户授予指定 `project/env` 的访问权限。
- 撤销不再需要的项目环境授权。
- 按 username、projectKey、environmentKey、status 过滤授权。

关键字段：

| 字段             | 说明                                                   |
|----------------|------------------------------------------------------|
| user           | 被授权用户。                                               |
| projectKey     | `dbflow.projects[*].key` 中配置的项目标识，例如 `billing-core`。 |
| environmentKey | 项目下的环境标识，例如 `dev`、`staging`、`prod`。                  |
| grantType      | 授权类型，MVP 以项目环境访问授权为主。                                |

操作步骤：

1. 打开 `/admin/grants`。
2. 选择用户、projectKey、environmentKey 和授权类型。
3. 保存后让员工通过 `dbflow_list_targets` 验证可见目标。
4. 权限过期、岗位变更或误授权时，使用撤销操作。

授权原则：

- 默认最小权限：只授权员工实际需要的 project/env。
- `prod` 环境授权应有业务负责人确认。
- 撤销授权后，员工对该 project/env 的 `dbflow_inspect_schema`、`dbflow_explain_sql`、`dbflow_execute_sql`
  都应被拒绝。

## 8. 颁发、吊销和重新颁发 Token

页面：`/admin/tokens`

主任务：

- 为已授权用户颁发 MCP Token。
- 吊销泄露、过期或不再需要的 Token。
- 重新颁发 Token，替代旧凭据。

关键字段：

| 字段            | 说明                              |
|---------------|---------------------------------|
| user          | Token 所属用户。                     |
| prefix        | 列表中用于识别 Token 的安全前缀，不是完整 Token。 |
| expiresInDays | 有效天数；生产建议设置明确过期时间。              |
| status        | 当前 Token 是否有效、吊销或过期。            |

安全规则：

- Token 明文只在颁发或重新颁发成功后通过一次性页面状态展示一次。
- React 新后台的明文 reveal dialog 关闭后会清空本地明文状态；刷新、返回列表或重新打开页面都不能再次取回明文。
- 管理端列表不展示 Token 明文、Token hash 或 pepper。
- 不通过 IM、邮件正文、截图或共享文档传播真实 Token。
- 如果员工反馈 Token 丢失，管理员应重新颁发并吊销旧 Token，而不是查询旧明文。

交付给员工时，只给必要信息：

```text
MCP URL: https://dbflow.internal.example/mcp
Token: <DBFLOW_TOKEN_ONLY_VISIBLE_ONCE>
Authorized targets: billing-core/staging
Client guide: docs/user-guide/mcp-clients.md
```

## 9. 查看审计

页面：

- 列表：`/admin/audit`
- 详情：`/admin/audit/{eventId}`
- API：`/admin/api/audit-events`

主任务：

- 按时间、用户、project、env、risk、decision、SQL hash、tool 过滤 AI 数据库操作。
- 查看单条审计详情、拒绝原因、确认状态和结果摘要。
- 用 requestId、traceId、sqlHash 与日志、指标、员工反馈关联。

常用过滤：

| 场景             | 建议过滤                                                                 |
|----------------|----------------------------------------------------------------------|
| 员工反馈“Token 无效” | user、tool、decision=`DENIED` 或错误时间范围                                  |
| 排查危险 SQL       | risk=`HIGH` / `CRITICAL`，decision=`DENIED` 或 `REQUIRES_CONFIRMATION` |
| 追踪某条 SQL       | sqlHash                                                              |
| 追踪 MCP 客户端问题   | tool、source IP、client metadata、时间范围                                  |

审计详情会展示 SQL text、SQL hash、operation、risk、decision、result summary 等信息。`result_summary` 是摘要，
不会保存完整结果集；详情也不会展示 Token 明文、Token hash、数据库密码或 JDBC 连接串。

## 10. 查看危险策略

页面：`/admin/policies/dangerous`

主任务：

- 查看当前有效的危险 SQL 策略。
- 理解 DROP 白名单、TRUNCATE confirmation 和 prod 强化规则。
- 向配置维护者提供策略问题的准确定位信息。

当前策略边界：

- `DROP DATABASE` 默认拒绝。
- `DROP TABLE` 默认拒绝，只有 YAML/Nacos 白名单命中时才可能放行。
- `TRUNCATE` 不依赖客户端口头确认；`dbflow_execute_sql` 创建服务端 confirmation，`dbflow_confirm_sql`
  校验同一用户、同一 Token、同一 project/env、同一 SQL hash 和未过期状态后才会执行。
- `prod` 危险 DDL 需要显式配置允许，否则保持拒绝。

策略页 MVP 只读展示，配置来源仍以外部 YAML 或 Nacos 为准。

## 11. 排查连接

页面：`/admin/health`

命令：

```bash
curl -s http://127.0.0.1:8080/actuator/health
curl -s http://127.0.0.1:8080/actuator/metrics
```

排查顺序：

1. 看 `/admin/health`：确认 metadata database、target datasource registry、Nacos、MCP endpoint readiness。
2. 看启动日志：建议本地使用 `./mvnw spring-boot:run | tee target/dbflow.log`。
3. 用 requestId 或 traceId 检索日志。
4. 如果是 MCP 客户端连不上，确认 URL 为 `/mcp`，Token 在 Authorization header 中，且没有使用 query string token。
5. 如果浏览器型客户端被拒绝，检查 `dbflow.security.mcp-endpoint.origin.trusted-origins`。
6. 如果 target DB 不可用，检查外部配置中的 `dbflow.projects[*].environments[*]`，不要在日志或工单里粘贴密码。

更多场景见 [troubleshooting.md](../runbooks/troubleshooting.md)。

## 12. 日常检查清单

- 每周检查即将过期或长期未使用的 Token。
- 每次员工转岗、离职或项目结束后，撤销不需要的 project/env 授权。
- 对 `HIGH` / `CRITICAL` risk 的审计记录做定期抽查。
- 对 `DENIED` 和 `FAILED` 激增场景，先查 `/admin/audit`，再查日志和 metrics。
- 配置变更后先看 `/admin/health`，再让员工执行只读 smoke。
