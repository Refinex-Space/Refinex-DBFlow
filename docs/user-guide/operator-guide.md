# DBFlow 员工使用手册

本文档面向使用 Codex、Claude、OpenCode、Copilot 等 MCP 客户端访问 Refinex-DBFlow 的员工。目标是完成一次安全的只读
smoke，并理解查询、确认和拒绝原因。客户端配置细节见 [mcp-clients.md](mcp-clients.md)。

## 1. 申请 Token 和授权

向 DBFlow 管理员提交以下信息：

| 项目                | 示例                            | 说明                       |
|-------------------|-------------------------------|--------------------------|
| 用户标识              | `zhang.wei`                   | 应与企业账号一致。                |
| 需要访问的 project/env | `billing-core/staging`        | 只申请实际需要的环境。              |
| 使用目的              | 排查订单状态、验证数据修复结果               | 会进入审计上下文或工单记录。           |
| 客户端               | Codex、Claude、OpenCode、Copilot | 方便管理员排查 client metadata。 |

管理员完成三件事后，员工才能使用：

1. 创建或启用用户。
2. 授权 project/env。
3. 颁发 MCP Token，并只展示一次明文。

收到 Token 后立即保存到本机安全位置，例如 shell 环境变量或系统密钥管理器。不要把真实 Token 写入仓库、
`.mcp.json`、截图、聊天记录或工单。

## 2. 配置 MCP 客户端

连接参数：

| 项目        | 本地开发                                   | 内网部署示例                                 |
|-----------|----------------------------------------|----------------------------------------|
| MCP URL   | `http://127.0.0.1:8080/mcp`            | `https://dbflow.internal.example/mcp`  |
| Transport | Streamable HTTP                        | Streamable HTTP                        |
| Auth      | `Authorization: Bearer <DBFlow Token>` | `Authorization: Bearer <DBFlow Token>` |

假 Token 示例：

```bash
export DBFLOW_MCP_TOKEN="dbf_fake_token_for_docs_only_do_not_use"
```

规则：

- 每个 `/mcp` 请求都必须带 Bearer Token。
- 不支持 `?token=` 或 `?access_token=`；DBFlow 会拒绝 query string token。
- CLI/Agent 客户端通常不带 `Origin`；浏览器型客户端或代理带 `Origin` 时，需要管理员配置可信来源。
- Codex、Claude、OpenCode、Copilot 的配置片段见 [mcp-clients.md](mcp-clients.md)。

## 3. 选择 project/env

先让客户端调用：

```text
dbflow_list_targets
```

只使用返回列表中的 project/env。常见字段包括 project key、environment key、可见状态和数据源摘要。不要猜测环境名；
如果列表里没有目标环境，说明授权尚未生效或已被撤销。

建议约定：

- `dev`：开发验证，仍然要遵守 SQL policy。
- `staging`：预发排查，尽量先 `dbflow_explain_sql`。
- `prod`：生产环境，默认只做必要的只读查询；任何变更都应有明确工单或负责人确认。

## 4. First-use Smoke Test

首次使用请在 MCP 客户端中输入类似 prompt：

```text
Use Refinex-DBFlow only. First call dbflow_list_targets.
Choose project "billing-core" and env "staging" only if it is visible.
Then inspect schema "billing" with maxItems 20.
Explain this SQL without executing it:
SELECT id, status, amount FROM payment_order WHERE status = 'PENDING' ORDER BY created_at DESC LIMIT 20;
Finally execute the same SELECT with LIMIT 20 and summarize whether the result was truncated.
Do not run UPDATE, DELETE, TRUNCATE, DROP, or ALTER.
```

预期：

- `dbflow_list_targets` 返回你有权限的 project/env。
- `dbflow_inspect_schema` 返回 bounded schema/table/column/index 元数据，不包含密码或连接串。
- `dbflow_explain_sql` 返回 plan rows、type、key、rows、filtered、Extra 和基础索引建议。
- `dbflow_execute_sql` 对 SELECT 返回有限行数，并在达到上限时标记 truncated。
- 管理员能在 `/admin/audit` 查到对应 tool、user、project/env、operation、risk、decision 和 sqlHash。

## 5. 执行查询的安全习惯

优先顺序：

1. 先 `dbflow_list_targets`，确认 project/env。
2. 再 `dbflow_inspect_schema`，确认表、字段和索引。
3. 对复杂 SELECT 或 DML，先 `dbflow_explain_sql`。
4. 最后才 `dbflow_execute_sql`。

查询建议：

- SELECT 必须加业务条件和合理 `LIMIT`。
- 不要让 AI “随便查全表看看”；结果默认会截断，但大查询仍会占用目标数据库资源。
- 对 UPDATE/DELETE/ALTER 这类变更 SQL，要在 prompt 中写清操作原因，例如 `reason: 工单 INC-1234 回滚测试数据`。
- 遇到不理解的结果，先让客户端解释 `statementSummary`、`warnings`、`durationMillis` 和 `notices`，不要重复执行。

## 6. 处理确认挑战

`TRUNCATE` 属于高风险操作。DBFlow 不接受自然语言确认，也不会因为 AI 说“用户已确认”就执行。

典型流程：

1. 你通过 `dbflow_execute_sql` 提交 `TRUNCATE TABLE ...`。
2. 服务端返回 `REQUIRES_CONFIRMATION`、`confirmationId`、过期时间和 SQL hash，不执行目标 SQL。
3. 你确认 project/env、SQL text、工单和业务影响。
4. 使用 `dbflow_confirm_sql` 提交同一个 `confirmationId` 和同一条 SQL。
5. 服务端校验同一用户、同一 Token、同一 project/env、同一 SQL hash、未过期、未消费后才执行。

如果确认过期，重新从 `dbflow_execute_sql` 开始，不要复用旧 confirmationId。

## 7. 理解拒绝原因

常见拒绝和处理方式：

| 现象                        | 常见原因                        | 处理                                         |
|---------------------------|-----------------------------|--------------------------------------------|
| `401` / `UNAUTHENTICATED` | Token 缺失、格式错误、未知、过期或已吊销     | 检查 Authorization header；需要时请管理员重新颁发 Token。 |
| `403` / `FORBIDDEN`       | 没有目标 project/env 授权         | 请管理员在 `/admin/grants` 授权。                  |
| `ORIGIN_DENIED`           | 浏览器型客户端 Origin 未配置为可信来源     | 提供客户端 Origin 给管理员。                         |
| `RATE_LIMITED`            | 请求过快或共享出口触发限流               | 降低频率，等待窗口恢复；批量任务不要走交互式 MCP。                |
| `REQUEST_TOO_LARGE`       | SQL 或请求体超过限制                | 缩小请求，不要上传大结果或长脚本。                          |
| `POLICY_DENIED`           | DROP、prod 危险 DDL、未白名单表等策略拒绝 | 查看返回 reason；需要变更配置时走审批。                    |
| `SQL_PARSE_FAILED`        | SQL 语法错误、多语句或不支持的语句形态       | 改成单条明确 SQL，先 explain。                      |
| `CONFIRMATION_EXPIRED`    | confirmationId 过期或已消费       | 重新发起确认流程。                                  |
| `TRUNCATED` notice        | 结果超过服务端 max rows            | 加更精确条件或分页查询。                               |

拒绝不是“系统坏了”。它通常说明身份、授权、策略或确认链路正在按预期保护数据库。

## 8. 出问题时给管理员的信息

提交排查请求时，提供这些信息即可：

- 时间范围和客户端名称。
- project/env。
- tool 名称，例如 `dbflow_execute_sql`。
- requestId、traceId 或 sqlHash，如果客户端响应里有。
- 你希望完成的业务动作和工单号。

不要提供：

- 真实 Token 明文。
- 数据库密码、JDBC URL 中的敏感参数。
- 大量完整查询结果。
