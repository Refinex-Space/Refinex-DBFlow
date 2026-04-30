# DBFlow MCP Client Guide

本文档说明如何把 Codex、Claude、OpenCode、GitHub Copilot / VS Code 连接到 Refinex-DBFlow MCP
Streamable HTTP endpoint，并完成首次只读查询 smoke。所有 Token 示例均为假值，不能作为真实凭据使用。

## 1. 连接参数

| 项目        | 本地开发                                   | 内网部署                                   |
|-----------|----------------------------------------|----------------------------------------|
| MCP URL   | `http://127.0.0.1:8080/mcp`            | `https://dbflow.internal.example/mcp`  |
| Transport | Streamable HTTP                        | Streamable HTTP                        |
| Auth      | `Authorization: Bearer <DBFlow Token>` | `Authorization: Bearer <DBFlow Token>` |
| Token 来源  | 管理端 `/admin/tokens` 颁发，一次性展示明文         | 管理端 `/admin/tokens` 颁发，一次性展示明文         |
| Origin    | CLI/Agent 客户端通常不带 `Origin`             | 浏览器型客户端或代理带 `Origin` 时必须在服务端配置可信来源     |

假 Token 示例：

```bash
export DBFLOW_MCP_TOKEN="dbf_fake_token_for_docs_only_do_not_use"
```

规则：

- 不要把真实 Token 写进仓库、团队共享配置或截图。
- 不支持 `?token=`、`?access_token=` 等 query string Token；DBFlow 会拒绝。
- 每个请求都要带 Bearer Token，`Mcp-Session-Id` 不承载认证状态。
- 只读 smoke 前，管理员需要先给该 Token 所属用户授予 project/env 权限。

官方资料核验：

- MCP 规范支持 HTTP 请求携带 `Authorization: Bearer <access_token>`。
- Codex 配置参考记录了 `mcp_servers.<id>.url`、`bearer_token_env_var`、`env_http_headers` 和
  `http_headers`。
- Claude Code 官方文档推荐 remote HTTP server，并支持 `--header`、`headers` 和 `headersHelper`。
- OpenCode 官方文档使用 `opencode.json` 的 remote MCP server、`headers` 和 `{env:...}`。
- VS Code / Copilot MCP 配置参考使用 `mcp.json` 的 `servers`、`inputs`、`type: "http"`、`url` 和
  `headers`。

## 2. Codex

适用范围：Codex CLI / Codex App 读取同一套 `~/.codex/config.toml` 配置时。具体加载范围以当前 Codex 版本为准。

`~/.codex/config.toml`：

```toml
[mcp_servers.refinex_dbflow]
enabled = true
url = "https://dbflow.internal.example/mcp"
bearer_token_env_var = "DBFLOW_MCP_TOKEN"
startup_timeout_sec = 10
tool_timeout_sec = 60
```

本地开发可把 URL 改成：

```toml
url = "http://127.0.0.1:8080/mcp"
```

启动 Codex 前设置环境变量：

```bash
export DBFLOW_MCP_TOKEN="dbf_fake_token_for_docs_only_do_not_use"
codex
```

可选：只暴露本手册 smoke 所需工具，降低误用面。

```toml
[mcp_servers.refinex_dbflow]
enabled = true
url = "https://dbflow.internal.example/mcp"
bearer_token_env_var = "DBFLOW_MCP_TOKEN"
enabled_tools = [
    "dbflow_list_targets",
    "dbflow_inspect_schema",
    "dbflow_explain_sql",
    "dbflow_execute_sql",
    "dbflow_confirm_sql"
]
```

版本核验：

```bash
codex --version
```

如果当前版本不识别 `bearer_token_env_var`，按官方配置参考核验 `env_http_headers` 或 `http_headers`。不要在
`http_headers` 里写真实 Token。

## 3. Claude

推荐入口：Claude Code。Claude Desktop、Claude.ai managed connector、Claude API MCP connector 的配置形态随产品版本
和套餐能力变化，接入前应按对应官方文档核验；不要把 Claude Code 的 JSON 字段无条件复制到其它 Claude 产品。

### 3.1 Claude Code CLI

临时本机配置：

```bash
export DBFLOW_MCP_TOKEN="dbf_fake_token_for_docs_only_do_not_use"
claude mcp add --transport http --header "Authorization: Bearer ${DBFLOW_MCP_TOKEN}" \
  refinex-dbflow https://dbflow.internal.example/mcp
claude mcp get refinex-dbflow
```

本地开发：

```bash
claude mcp add --transport http --header "Authorization: Bearer ${DBFLOW_MCP_TOKEN}" \
  refinex-dbflow http://127.0.0.1:8080/mcp
```

项目 `.mcp.json` 模板，适合提交到项目时保留假值并让个人在本地替换：

```json
{
  "mcpServers": {
    "refinex-dbflow": {
      "type": "http",
      "url": "https://dbflow.internal.example/mcp",
      "headers": {
        "Authorization": "Bearer ${DBFLOW_MCP_TOKEN}"
      }
    }
  }
}
```

更安全的个人配置方式是使用 `headersHelper`，让 helper 从本机环境变量或密钥系统读取 Token：

```json
{
  "mcpServers": {
    "refinex-dbflow": {
      "type": "http",
      "url": "https://dbflow.internal.example/mcp",
      "headersHelper": "printf '{\"Authorization\":\"Bearer %s\"}' \"$DBFLOW_MCP_TOKEN\""
    }
  }
}
```

`headersHelper` 会执行 shell 命令；只在可信 workspace 使用。

## 4. OpenCode

适用范围：OpenCode `opencode.json` remote MCP server。OpenCode 官方示例支持 `headers`，也展示了 `{env:...}`
形式的环境变量占位。

`opencode.json`：

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "refinex-dbflow": {
      "type": "remote",
      "url": "https://dbflow.internal.example/mcp",
      "enabled": true,
      "oauth": false,
      "headers": {
        "Authorization": "Bearer {env:DBFLOW_MCP_TOKEN}"
      }
    }
  }
}
```

本地开发 URL：

```json
"url": "http://127.0.0.1:8080/mcp"
```

启动前：

```bash
export DBFLOW_MCP_TOKEN="dbf_fake_token_for_docs_only_do_not_use"
opencode
```

如果当前 OpenCode 版本未展开 `headers` 中的 `{env:DBFLOW_MCP_TOKEN}`，改用个人本地配置文件或启动脚本生成
`opencode.json`，不要提交真实 Token。

## 5. GitHub Copilot / VS Code

适用范围：VS Code GitHub Copilot Chat/Agent mode 的 MCP server 配置。配置文件可以放在 workspace
`.vscode/mcp.json` 或用户 profile；团队仓库里不要提交真实 Token。

`.vscode/mcp.json`：

```json
{
  "inputs": [
    {
      "type": "promptString",
      "id": "dbflow-token",
      "description": "DBFlow MCP Token",
      "password": true
    }
  ],
  "servers": {
    "refinexDbflow": {
      "type": "http",
      "url": "https://dbflow.internal.example/mcp",
      "headers": {
        "Authorization": "Bearer ${input:dbflow-token}"
      }
    }
  }
}
```

本地开发 URL：

```json
"url": "http://127.0.0.1:8080/mcp"
```

VS Code 命令面板验证：

- `MCP: List Servers`
- `MCP: Reset Cached Tools`
- `MCP: Show Installed Servers`

如果企业策略禁用了 MCP 或限制了服务器来源，联系 GitHub Copilot / VS Code 管理员调整 `chat.mcp.*` 相关策略。

## 6. 首次只读 smoke prompt

在任意已连接 DBFlow 的客户端里粘贴下面 prompt。把 `demo`、`dev`、`app_db` 和表名替换成你实际被授权的
project/env/schema/table。

```text
请使用 Refinex-DBFlow MCP 完成一次只读 smoke，不要执行 DML/DDL：

1. 调用 dbflow_list_targets，列出当前 Token 可访问的 project/env。
2. 选择 project=demo, env=dev。
3. 调用 dbflow_inspect_schema，参数：project=demo, env=dev, schema=app_db, table=users, maxItems=20。
4. 调用 dbflow_explain_sql，参数：project=demo, env=dev, schema=app_db, sql="SELECT id, username FROM users ORDER BY id LIMIT 5"。
5. 调用 dbflow_execute_sql，只执行这条 SELECT：
   project=demo
   env=dev
   schema=app_db
   sql="SELECT id, username FROM users ORDER BY id LIMIT 5"
   dryRun=false
   reason="MCP client read-only smoke"

请汇总：可访问目标、schema/table 是否存在、EXPLAIN 关键字段、SELECT 返回列名、是否发生结果截断。
```

预期：

- `dbflow_list_targets` 能返回目标或至少明确授权状态。
- `dbflow_inspect_schema` 不返回 JDBC URL、数据库密码或连接串。
- `dbflow_explain_sql` 不实际执行 DML，SELECT 只返回执行计划。
- `dbflow_execute_sql` 的 SELECT 返回有限行数；结果默认受服务端上限约束。

## 7. 高风险策略 smoke prompt

只在专用 scratch schema 执行，并提前准备一个允许被清空的临时表。不要在生产库或真实业务表上运行。

```text
请使用 Refinex-DBFlow MCP 验证高风险 SQL 策略。仅使用 project=demo, env=dev, schema=scratch。

1. 调用 dbflow_execute_sql：
   sql="TRUNCATE TABLE tmp_mcp_smoke"
   dryRun=false
   reason="Verify DBFlow TRUNCATE confirmation flow"
   预期：不要立即执行，返回 confirmationRequired=true 和 confirmationId。

2. 如果第 1 步返回 confirmationId，再调用 dbflow_confirm_sql：
   confirmationId=<上一条返回的 confirmationId>
   sql="TRUNCATE TABLE tmp_mcp_smoke"
   reason="Confirm DBFlow TRUNCATE smoke on disposable table"
   预期：只有同一 user/token/project/env/sqlHash 且未过期时确认成功。

3. 调用 dbflow_execute_sql：
   sql="DROP TABLE non_whitelisted_table"
   dryRun=false
   reason="Verify DROP denial smoke"
   预期：策略拒绝，不执行 DROP。

请只汇总每一步 status、confirmationId、error.code、error.message，不要输出 Token 明文。
```

## 8. 常见错误

| 症状                                    | 常见原因                                    | 处理                                                                                |
|---------------------------------------|-----------------------------------------|-----------------------------------------------------------------------------------|
| `401 UNAUTHORIZED`                    | 未带 Bearer Token、Token 拼写错误、Token 已吊销或过期 | 重新从管理端颁发 Token；确认 header 是 `Authorization: Bearer ...`                            |
| `401` 且 URL 含 `?token=`               | DBFlow 拒绝 query string Token            | 移除 query 参数，改用 header                                                             |
| `403 ORIGIN_DENIED`                   | 浏览器型客户端或代理携带了未信任 Origin                 | 在服务端 `dbflow.security.mcp-endpoint.origin.trusted-origins` 加入客户端 Origin           |
| `403 FORBIDDEN` 或工具返回 `allowed=false` | 用户没有 project/env 授权                     | 管理端 `/admin/grants` 授权对应环境                                                        |
| `404`                                 | URL 不是 `/mcp`，或反向代理 path 改写错误           | 使用 `http://host:port/mcp` 或 `https://host/mcp`                                    |
| `413 REQUEST_TOO_LARGE`               | 请求体超过服务端上限                              | 缩短 SQL / prompt；必要时调整 `dbflow.security.mcp-endpoint.request-size.max-bytes`       |
| `429 RATE_LIMITED`                    | 单个 source IP 超过限流窗口                     | 等待窗口恢复，或按部署负载调整 rate limit                                                        |
| 客户端看不到工具                              | 客户端缓存、配置未重载或 server 未初始化成功              | 重启客户端；Codex/Claude/OpenCode 查看 MCP server 状态；VS Code 执行 `MCP: Reset Cached Tools` |
| SELECT 没有返回全部行                        | 服务端默认截断和最大行数保护                          | 在 SQL 中明确 `LIMIT`；不要依赖无限结果集                                                       |
| TRUNCATE 未执行                          | 这是预期行为，必须先返回确认挑战                        | 使用 `dbflow_confirm_sql` 且保持原 SQL 完全一致                                             |
| DROP 被拒绝                              | 默认策略拒绝 DROP，未命中白名单                      | 仅在 YAML/Nacos 白名单明确配置且非生产强化规则拒绝时才可能放行                                             |

## 9. 参考链接

- [Model Context Protocol Authorization](https://modelcontextprotocol.io/specification/draft/basic/authorization)
- [Codex Configuration Reference](https://developers.openai.com/codex/config-reference)
- [Claude Code MCP](https://docs.claude.com/en/docs/claude-code/mcp)
- [OpenCode MCP servers](https://opencode.ai/docs/mcp-servers/)
- [VS Code MCP configuration reference](https://code.visualstudio.com/docs/copilot/reference/mcp-configuration)
