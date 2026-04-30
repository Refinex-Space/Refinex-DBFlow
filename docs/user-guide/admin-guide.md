# DBFlow 管理员手册

本文档面向 Refinex-DBFlow 管理员，说明如何通过管理端完成用户、授权、Token、审计、策略和连接排查。MCP
客户端配置细节见 [mcp-clients.md](mcp-clients.md)，部署与外部配置见
[docs/deployment/README.md](../deployment/README.md)。

## 1. 管理端入口

| 场景     | 地址                                      |
|--------|-----------------------------------------|
| 本地开发   | `http://127.0.0.1:8080/login`           |
| 内网部署示例 | `https://dbflow.internal.example/login` |

登录后进入 `/admin` 总览页。管理端使用 Spring Security form login，`/admin/**` 需要管理员会话；
`/mcp` 不使用管理端 session，而是每次请求都要求 `Authorization: Bearer <DBFlow Token>`。

初始管理员账号应来自外部配置或密钥系统，例如 `dbflow.admin.initial-user.*`。不要把真实密码、BCrypt hash、Token
pepper 或数据库密码写入仓库。

## 2. First-use Smoke Test

本地开发环境首次验证：

```bash
./mvnw spring-boot:run
```

另开终端检查最小健康面：

```bash
curl -s http://127.0.0.1:8080/actuator/health
```

预期：

- 返回有限的 Actuator health 状态，默认不展示敏感详情。
- 浏览器访问 `http://127.0.0.1:8080/login` 能看到 DBFlow 管理端登录页。
- 登录后访问 `/admin/health` 能看到 metadata database、target datasource registry、Nacos、MCP endpoint 状态。
- 访问 `/admin/audit` 能看到审计列表页；没有记录时应显示空状态，而不是报错。

## 3. 创建和禁用用户

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

## 4. 授权项目环境

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

## 5. 颁发、吊销和重新颁发 Token

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
- 管理端列表不展示 Token 明文、Token hash 或 pepper。
- 不通过 IM、邮件正文、截图或共享文档传播真实 Token。
- 如果员工反馈 Token 丢失，管理员应重新颁发并吊销旧 Token，而不是查询旧明文。

交付给员工时，只给必要信息：

```text
MCP URL: https://dbflow.internal.example/mcp
Token: dbf_xxx_only_visible_once
Authorized targets: billing-core/staging
Client guide: docs/user-guide/mcp-clients.md
```

## 6. 查看审计

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

## 7. 查看危险策略

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

## 8. 排查连接

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

## 9. 日常检查清单

- 每周检查即将过期或长期未使用的 Token。
- 每次员工转岗、离职或项目结束后，撤销不需要的 project/env 授权。
- 对 `HIGH` / `CRITICAL` risk 的审计记录做定期抽查。
- 对 `DENIED` 和 `FAILED` 激增场景，先查 `/admin/audit`，再查日志和 metrics。
- 配置变更后先看 `/admin/health`，再让员工执行只读 smoke。
