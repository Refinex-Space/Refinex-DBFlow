# DBFlow 安全与审计说明

Refinex-DBFlow 的目标不是让 AI “绕过数据库权限”，而是把 AI 数据库操作变成可授权、可限制、可确认、可审计的受控流程。
本文解释为什么 AI 操作数据库不是黑盒，以及管理员如何用审计记录复盘风险。

## 1. 核心原则

| 原则      | DBFlow 做法                                                                                   |
|---------|---------------------------------------------------------------------------------------------|
| 身份明确    | `/mcp` 每次请求都要求 `Authorization: Bearer <DBFlow Token>`，Token 绑定用户。                           |
| 权限先行    | `dbflow_inspect_schema`、`dbflow_explain_sql`、`dbflow_execute_sql` 在访问目标库前检查 project/env 授权。 |
| 策略服务端执行 | SQL classification、DROP 白名单、TRUNCATE confirmation、prod 强化规则由服务端执行，不信任客户端自述。                 |
| 结果有边界   | SELECT、schema inspect、EXPLAIN 和 result summary 都有上限，不无限返回完整结果集。                             |
| 审计可复盘   | request、policy denied、requires confirmation、executed、failed、confirmation expired 等路径都会写审计。  |
| 敏感信息不落库 | 不保存 Token 明文、数据库密码、连接串密钥或完整查询结果集。                                                           |

## 2. 审计记录包含什么

审计事件用于回答五个问题：谁、从哪里、用什么工具、对哪个环境、做了什么结果。

常见字段：

| 字段                           | 用途                                                                                         |
|------------------------------|--------------------------------------------------------------------------------------------|
| user_id                      | 关联内部用户。                                                                                    |
| token_id / token_prefix      | 识别使用的 MCP Token，但不暴露明文。                                                                    |
| client_name / client_version | 区分 Codex、Claude、OpenCode、Copilot 等客户端。                                                     |
| user_agent / source_ip       | 辅助定位来源。                                                                                    |
| requestId / traceId          | 关联 MCP 响应、日志和排查工单。                                                                         |
| project / env                | 明确目标项目环境。                                                                                  |
| tool                         | `dbflow_list_targets`、`dbflow_inspect_schema`、`dbflow_explain_sql`、`dbflow_execute_sql` 等。 |
| operation / risk             | SQL 操作类型和风险等级。                                                                             |
| decision                     | `EXECUTED`、`DENIED`、`REQUIRES_CONFIRMATION`、`FAILED` 等。                                    |
| sql_text / sql_hash          | 支持查看原 SQL 和按 hash 聚合追踪。                                                                    |
| result_summary               | bounded 摘要，例如 affected rows、warning、duration、truncated。                                    |

审计详情不会展示 Token 明文、Token hash、数据库密码、JDBC 连接串密钥或完整结果集。

## 3. First-use Audit Smoke Test

完成一次只读 MCP smoke 后，管理员在管理端验证审计价值：

1. 员工按 [operator-guide.md](operator-guide.md) 执行 first-use smoke。
2. 管理员打开 `http://127.0.0.1:8080/admin/audit` 或内网 `/admin/audit`。
3. 按用户、project、env 和时间范围过滤。
4. 打开最新的 `dbflow_execute_sql` 详情。
5. 核对：
    - user、Token metadata、client metadata 不为空。
    - project/env 与员工请求一致。
    - operation 为 `SELECT`，risk 为低风险。
    - decision 为 `EXECUTED`。
    - result summary 只记录行数、截断状态、duration 等摘要。
    - 页面不出现 Token 明文、数据库密码或连接串。

这个 smoke 证明 AI 查询不是只存在于客户端聊天记录里，而是进入了 DBFlow 的服务端审计链路。

## 4. 三条典型审计链路

### 4.1 只读查询

路径：

```text
MCP request received -> authorization allowed -> SQL classified as SELECT -> policy allowed -> bounded execute -> audit executed
```

可复盘内容：

- 谁查了哪个 project/env。
- SQL hash 和 SQL text。
- 返回行数、是否 truncated、duration。
- 客户端和来源 IP。

### 4.2 TRUNCATE 二次确认

路径：

```text
dbflow_execute_sql -> requires confirmation -> dbflow_confirm_sql -> validation passed -> executed
```

可复盘内容：

- 第一次请求没有执行目标 DML/DDL，只创建 confirmation。
- confirmationId、SQL hash、过期时间和状态变化。
- 最终确认是否来自同一用户、同一 Token、同一 project/env、同一 SQL。

### 4.3 DROP 被拒绝

路径：

```text
MCP request received -> authorization allowed -> SQL classified as DROP -> dangerous policy denied -> audit policy denied
```

可复盘内容：

- 被拒绝的 DROP 类型：`DROP TABLE` 或 `DROP DATABASE`。
- 策略原因，例如未命中 YAML/Nacos 白名单或 prod 强化规则。
- 没有访问目标数据库执行 DROP。

## 5. 审计如何降低 AI 风险

AI 客户端擅长生成 SQL，但它不应该成为数据库权限边界。DBFlow 把边界放在服务端：

- AI 不能绕过 Token 认证。
- AI 不能看到未授权 project/env。
- AI 不能用提示词绕过 DROP/TRUNCATE 策略。
- AI 不能让服务端无限返回结果集。
- AI 不能抹掉服务端审计事件。

因此，组织可以允许员工在受控范围内提升查询和排障效率，同时保留管理端复盘、问责、告警和风险治理能力。

## 6. 事件排查建议

当出现误操作、拒绝激增或连接异常时，按这个顺序排查：

1. 在 `/admin/audit` 用时间、user、project/env、tool、decision 过滤。
2. 打开详情，记录 requestId、traceId、sqlHash 和 reason。
3. 用 requestId/traceId 搜索应用日志。
4. 在 `/admin/health` 查看 metadata database、target datasource registry、Nacos、MCP endpoint 状态。
5. 查看 `/actuator/metrics` 中 DBFlow 相关 meters，例如 MCP 调用、SQL risk、rejection、execution duration。
6. 必要时吊销 Token、禁用用户或撤销 project/env 授权。

可执行排查步骤见 [troubleshooting.md](../runbooks/troubleshooting.md)。

## 7. 留存和最小化建议

- 审计保留周期应由组织的安全、合规和数据治理要求决定。
- `sql_text` 有助于复盘，但不要把业务导出的完整数据集复制到审计摘要。
- `result_summary` 应保持 bounded；大结果应通过业务系统或正式数据流程处理。
- Token 轮换、用户禁用、授权撤销都应在审计中可见。
