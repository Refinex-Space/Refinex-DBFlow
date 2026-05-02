# Refinex-DBFlow Troubleshooting Runbook

本 runbook 面向本地开发、内网测试和首次部署排障。所有命令默认在仓库根目录执行：

```bash
cd /Users/refinex/develop/code/Refinex-DBFlow
```

## Correlation First

每次排障先拿到 `requestId` 和 `traceId`：

```bash
REQ_ID="debug-$(date +%Y%m%d%H%M%S)"
TRACE_ID="$REQ_ID"
```

调用 HTTP endpoint 时带上：

```bash
-H "X-Request-Id: $REQ_ID" -H "X-Trace-Id: $TRACE_ID"
```

需要可搜索日志文件时，用下面方式启动：

```bash
mkdir -p target
./mvnw spring-boot:run | tee target/dbflow.log
```

日志中搜索：

```bash
grep "requestId=$REQ_ID" target/dbflow.log
grep "traceId=$TRACE_ID" target/dbflow.log
```

本地控制台没有文件日志时，直接在启动终端搜索相同字段。

## Scenario 1: Application Fails To Start

Symptoms:

- `./mvnw spring-boot:run` 退出。
- 日志出现 `APPLICATION FAILED TO START`、端口占用或 Bean 创建失败。

Steps:

```bash
./mvnw -q -DskipTests compile
./mvnw spring-boot:run
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

Checks:

- 如果 8080 被占用，换端口启动：

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

- 如果是 Bean 创建失败，按日志里的 class name 搜代码：

```bash
rg "ClassNameFromStack" src/main/java src/test/java
```

## Scenario 2: Metadata Database Or Flyway Migration Fails

Symptoms:

- 启动时出现 `Flyway`、`dbflow_metadata`、`migration` 错误。
- `/actuator/health` 返回 `DOWN`。

Steps:

```bash
./mvnw -Dtest=MetadataSchemaMigrationTests test
./mvnw spring-boot:run
curl -s http://localhost:8080/actuator/health
```

Checks:

- 确认本地 profile 没有指向不可访问的外部 metadata database。
- 只看 health 状态，默认不会展示敏感详情；需要细节时在安全环境临时启用 Actuator health detail。

## Scenario 3: Nacos Configuration Fails

Symptoms:

- 使用 `nacos` profile 启动失败。
- 日志出现 `nacos`、`ConfigData`、`serverAddr` 或认证失败。

Steps:

```bash
DBFLOW_NACOS_SERVER_ADDR=127.0.0.1:8848 ./mvnw spring-boot:run
curl -s http://127.0.0.1:8848/nacos/v1/ns/operator/metrics
rg -n "nacos|spring.config.import|DBFLOW_NACOS" src/main/resources docs
```

Checks:

- 默认启动方式需要 Nacos；如果 Nacos 不可用，先修复 Nacos 或使用测试环境覆盖配置。
- 确认 `DBFLOW_NACOS_NAMESPACE`、`DBFLOW_NACOS_USERNAME`、`DBFLOW_NACOS_PASSWORD` 来自环境变量或密钥系统。
- 日志和文档不得出现 Nacos 密码明文。

## Scenario 4: Target Database Connection Fails

Symptoms:

- 日志出现 `目标数据源启动校验失败`、`datasource.target.create.failed`。
- SQL tool 返回目标库连接失败或执行失败。
- MCP tool 返回 `Connection is not available, request timed out after 30001ms` 时，通常表示运行配置仍在使用旧的
  `connection-timeout=30s` 或配置刷新/重启尚未生效。

Steps:

```bash
rg -n "dbflow.projects|jdbc-url|validate-on-startup" src/main/resources docs
./mvnw -Dtest=HikariDataSourceRegistryTests test
curl -s http://localhost:8080/actuator/health
```

Checks:

- JDBC URL 不要携带 password 参数；密码必须在 password 字段或外部密钥源。
- 本地离线开发保持 `dbflow.datasource-defaults.validate-on-startup=false`。
- 关闭启动校验时，DBFlow 会把目标 Hikari pool 的实际 `minimumIdle` 降为 `0`；如果仍出现
  `connection-adder` 建连告警，优先确认运行包是否已包含该修复。
- dev 模板默认 `dbflow.datasource-defaults.hikari.connection-timeout=5s`；如果 Codex/MCP 仍等待约 30 秒才失败，
  先检查 Nacos `application-dbflow.yml` 中是否仍是 `30s`，更新后重启应用或执行受控配置刷新。
- 生产要 fail fast 时再启用启动校验。

## Scenario 5: Runtime Config Refresh Or Datasource Replacement Fails

Symptoms:

- 日志出现 `config.reload.completed status=failure`。
- 旧连接池仍然可用，但新配置未生效。

Steps:

```bash
./mvnw -Dtest=DataSourceConfigReloaderTests test
grep "config.reload" target/dbflow.log
grep "datasource.registry.replace" target/dbflow.log
```

Checks:

- 失败时 DBFlow 会保留旧连接池；不要手动杀进程作为第一步。
- 检查候选配置是否缺少 project/env key、driver、JDBC URL 或 Hikari pool 参数。
- 日志只允许出现 project/env、targetCount、reason，不应出现 JDBC URL 或数据库密码。

## Scenario 6: MCP Token Invalid Or Rejected

Symptoms:

- `/mcp` 返回 `401`。
- 响应包含 `code":"UNAUTHORIZED"`。
- 日志出现 `mcp.auth.rejected`。

Steps:

```bash
REQ_ID="token-debug-$(date +%s)"
curl -i http://localhost:8080/mcp \
  -H "X-Request-Id: $REQ_ID" \
  -H "X-Trace-Id: $REQ_ID" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"0.0.1"}}}'
```

Checks:

- 必须使用 `Authorization: Bearer <DBFlow Token>`。
- 不支持 `?token=` 或 `?access_token=`；query string token 会被拒绝。
- Token 明文只在管理端颁发成功时展示一次；丢失后只能重新颁发。

## Scenario 7: MCP Client Cannot Connect

Symptoms:

- MCP Inspector、Codex、Claude 或其他客户端连不上。
- 客户端报 connection refused、404、401、403、SSE/Streamable HTTP 初始化失败。

Steps:

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/mcp \
  -H "Authorization: Bearer $DBFLOW_MCP_TOKEN" \
  -H "X-Request-Id: mcp-connect-debug" \
  -H "X-Trace-Id: mcp-connect-debug" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"0.0.1"}}}'
```

Checks:

- endpoint 是 `/mcp`，不是 `/sse` 或 `/api/mcp`。
- `Mcp-Session-Id` 不携带认证状态，每个请求都要带 Bearer Token。
- 客户端若带 `Origin`，必须匹配 `dbflow.security.mcp-endpoint.origin.trusted-origins`。

## Scenario 8: Origin Denied

Symptoms:

- `/mcp` 返回 `403`。
- 响应包含 `code":"ORIGIN_DENIED"`。
- 日志出现 `mcp.request.rejected reason=origin-denied`。

Steps:

```bash
curl -i http://localhost:8080/mcp \
  -H "Origin: http://untrusted.example" \
  -H "Authorization: Bearer $DBFLOW_MCP_TOKEN" \
  -H "X-Request-Id: origin-debug" \
  -H "X-Trace-Id: origin-debug" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"0.0.1"}}}'
```

Checks:

- CLI/agent 客户端通常不带 `Origin`，可以正常通过。
- 浏览器或代理带 `Origin` 时，加入可信来源：

```yaml
dbflow:
  security:
    mcp-endpoint:
      origin:
        trusted-origins:
          - http://your-client-host:port
```

## Scenario 9: Request Too Large Or Rate Limited

Symptoms:

- `/mcp` 返回 `413`，`code":"REQUEST_TOO_LARGE"`。
- `/mcp` 返回 `429`，`code":"RATE_LIMITED"`。

Steps:

```bash
grep "mcp.request.rejected reason=request-too-large" target/dbflow.log
grep "mcp.request.rejected reason=rate-limited" target/dbflow.log
```

Checks:

- 调整请求体大小：

```yaml
dbflow:
  security:
    mcp-endpoint:
      request-size:
        max-bytes: 1048576
```

- 调整基础限流：

```yaml
dbflow:
  security:
    mcp-endpoint:
      rate-limit:
        max-requests: 120
        window: 1m
```

## Scenario 10: SQL Policy Denied

Symptoms:

- Tool response `data.status=DENIED`。
- Tool response `data.error.code=POLICY_DENIED`。
- 日志出现 `sql.policy.denied`。

Steps:

```bash
grep "sql.policy.denied" target/dbflow.log
rg -n "dangerous-ddl|drop|truncate|allow-prod-dangerous-ddl" src/main/resources docs
```

Checks:

- `DROP DATABASE` 和 `DROP TABLE` 默认拒绝，必须 YAML/Nacos whitelist 放行。
- `TRUNCATE` 不会直接执行，会创建服务端 confirmation challenge。
- 日志只记录 `project`、`env`、`operation`、`risk`、`sqlHash`，不要把 SQL 原文复制到工单。

## Scenario 11: SQL Execution Fails

Symptoms:

- Tool response `data.status=FAILED`。
- Tool response `data.error.code=SQL_EXECUTION_FAILED`。
- 日志出现 `sql.execution.failed`。

Steps:

```bash
grep "sql.execution.failed" target/dbflow.log
grep "sqlHash=<hash-from-tool-response>" target/dbflow.log
curl -s http://localhost:8080/actuator/metrics/dbflow.sql.execution.duration
```

Checks:

- 先按 `sqlHash` 串联 audit、日志和 MCP tool response。
- 不要在日志、工单或聊天中粘贴数据库密码、JDBC URL 或完整结果集。
- 如果是目标库权限、表不存在或语法错误，优先在目标库侧用只读账号复现。

## Scenario 12: Admin Login Or Management Page Access Fails

Symptoms:

- `/admin/**` 跳转 `/login`。
- 登录失败或普通用户访问后台返回拒绝。

Steps:

```bash
curl -i http://localhost:8080/admin
curl -i http://localhost:8080/login
./mvnw -Dtest=AdminSecurityTests,AdminUiControllerTests test
```

Checks:

- 管理端使用 Spring Security form login，不接受 MCP Bearer Token。
- 初始管理员密码应来自环境变量、本地未提交 profile 或 secret-managed BCrypt hash。

## Safe Logging Rules

- Do log: `requestId`, `traceId`, `project`, `env`, `operation`, `risk`, `decision`, `sqlHash`, `durationMillis`,
  `affectedRows`, `targetCount`, `sourceIp`.
- Do not log: Bearer Token plaintext, Token hash, database password, JDBC URL with credentials, Nacos password, full SQL
  result set, Java stack trace in client-facing responses.
- When sharing evidence, prefer:

```bash
grep "requestId=$REQ_ID" target/dbflow.log
grep "sqlHash=<hash>" target/dbflow.log
curl -s http://localhost:8080/actuator/health
```
