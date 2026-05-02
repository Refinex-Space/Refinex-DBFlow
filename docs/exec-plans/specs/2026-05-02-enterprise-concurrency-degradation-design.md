# Enterprise Concurrency And Degradation Design

## 背景

DBFlow 的正式企业部署目标是单实例内网 MCP Server。预期使用场景是约 100 名开发在工作高峰期同时通过
Codex、Claude、OpenCode、Copilot 等 AI 客户端调用同一个 `/mcp` Streamable HTTP endpoint，执行目标库查询、schema
inspect、EXPLAIN、受控 SQL 执行、TRUNCATE 确认、目标与策略查看等操作。

当前实现已经具备基础入口保护和 SQL 执行边界，但这些能力主要是单请求、单来源 IP、单目标 Hikari pool 的局部保护。它们不能单独证明系统能在
100 人高峰、流量突增、目标库变慢、代理出口 IP 聚合、多个 AI 客户端重试叠加时稳定运行。

本设计定义单实例内网部署下的企业级并发治理与降级方案，使 DBFlow 在高峰期优先保护服务进程、metadata
database、目标业务库和审计链路，而不是追求无限排队或无限并发。

## 现状证据

- `/mcp` 已有 `McpEndpointGuardFilter`，执行 Origin 校验、请求体大小限制、按 source IP 的固定窗口限流，并返回 413/429 等稳定
  HTTP 错误。
- MCP endpoint rate limit 默认是单来源 `120 requests / 1 minute`，配置位于 `dbflow.security.mcp-endpoint.rate-limit`。
- 目标库使用项目环境级 Hikari pool，配置位于 `dbflow.datasource-defaults.hikari`，支持 `maximum-pool-size`、`minimum-idle`、
  `connection-timeout`、`idle-timeout`、`max-lifetime`。
- 目标 Hikari `connection-timeout` 代码默认值和 dev Nacos 模板默认值已经收敛到 `5s`，避免不可达目标库让交互等待 Hikari 默认
  30 秒。
- `validate-on-startup=false` 时，DBFlow 会把目标 Hikari pool 的实际 `minimumIdle` 降为 `0`，避免启动后后台连接不可达目标库。
- `SqlJdbcExecutor` 对 SQL statement 设置 `queryTimeoutSeconds=10`、`maxRows=100`、`fetchSize=100`，并对查询结果做截断。
- `SchemaInspectService` 默认每类 metadata 返回 100 条，硬上限 500 条。
- `DbflowHealthService` 能展示每个目标 Hikari pool 的 `active`、`idle`、`total`、`waiting`。
- `DbflowMetricsService` 已记录 MCP tool 调用次数、SQL 风险分布、拒绝次数、SQL 执行耗时、待确认 challenge 数。
- 当前没有独立的业务级并发舱壁、按 Token/用户/工具/目标库/SQL 风险的分层限流、流量突增时的只读优先降级、写操作快速失败策略、metadata
  审计写入背压策略、压测验收基线或虚拟线程运行模式说明。

## 目标

- 支撑单实例内网部署下约 100 名开发高峰并发使用。
- 明确容量边界，让 DBFlow 在目标库慢、目标 pool 耗尽、AI 客户端集中重试时可控失败。
- 按工具类型、用户/Token、目标 project/env、SQL 风险建立服务端分层限流和并发舱壁。
- 保留当前安全优先策略：鉴权、授权、SQL 分类、危险策略、TRUNCATE 确认和审计不可被降级绕过。
- 对流量突增给出明确降级顺序：元数据与轻量只读能力优先，重型 inspect/EXPLAIN 降级，真实写执行快速失败。
- 将 JDK 21 虚拟线程纳入可选运行模式，但不让虚拟线程替代容量限制、连接池限制或 bulkhead。
- 新增可观测指标、健康状态和排障说明，使管理员能判断是入口限流、bulkhead 满、目标 pool 满、目标库慢还是执行超时。
- 给出可验证的压测验收标准，避免只从配置推断“应该能撑住”。

## 非目标

- 不引入 Redis、分布式 rate limiter、消息队列、外部 WAF 或网关级治理。
- 不支持多实例限流一致性；多实例部署需要后续独立设计。
- 不改变现有 MCP tool/resource/prompt 名称和主要响应结构。
- 不改变 SQL 风险策略本身，不放宽 DROP/TRUNCATE 等高风险操作限制。
- 不把 SQL 执行改成异步任务系统，不提供后台长任务队列。
- 不为了压测通过而增加自动重试；高风险或写操作失败后必须由客户端/用户显式重试。
- 不主动执行目标库健康探测 SQL 作为降级依据，避免监控本身制造目标库压力。

## 方案比较

### 方案 A：只调现有参数

调大或调小现有 source IP rate limit、Hikari maximum pool size、JDBC timeout、Tomcat thread 参数。

结论：拒绝。该方案成本低，但无法解决同一代理 IP 聚合、单目标库被打满、写操作挤占读操作、metadata 审计写入被拖慢、不同工具共用同一并发池等问题。

### 方案 B：进程内分层治理与可解释降级

在单实例进程内新增业务级 capacity guard：保留现有入口防刷，增加 Token/用户/工具/目标/风险维度的限流，按工具类别设置
bulkhead，按目标 project/env 限制并发，按系统压力态执行只读优先降级，并暴露专门指标。

结论：接受。该方案最符合当前单实例内网目标，不增加外部部署依赖，能覆盖企业首版正式部署需要的主要高峰和突增风险。

### 方案 C：外置网关或 Redis 分布式治理

通过 API Gateway、Redis、服务网格或 WAF 统一限流、熔断和排队。

结论：拒绝用于本阶段。它适合后续多实例部署，但对当前单实例内网目标过重，并会引入额外运维依赖和一致性问题。

## 接受的设计

采用方案 B：进程内分层治理与可解释降级。

### 容量模型

将 DBFlow 容量拆成 5 层，每一层都必须有独立保护：

1. HTTP 入口层：保护 `/mcp` endpoint 不被超大请求体、非可信 Origin、明显刷流量请求打穿。
2. MCP 工具层：保护不同工具类别互不挤占，避免真实 SQL 执行拖垮轻量只读能力。
3. 用户/Token 层：限制单个用户或 Token 的并发与速率，避免一个 AI 客户端或一个自动循环占满实例。
4. 目标 project/env 层：限制同一目标业务库的并发，避免 DBFlow 把压力集中打到一个业务库。
5. JDBC/metadata 层：用 Hikari pool、statement timeout、结果截断、审计写入边界保护数据库资源。

### 工具分级

MCP tool/resource 按压力和业务风险分为 4 类：

| 类别           | 工具/资源                                                                      | 默认策略                        |
|--------------|----------------------------------------------------------------------------|-----------------------------|
| `LIGHT_READ` | `dbflow_list_targets`、`dbflow_get_effective_policy`、目标/策略 resource、prompts | 高优先级，允许短等待，压力态继续服务          |
| `HEAVY_READ` | `dbflow_inspect_schema`、schema resource                                    | 中优先级，压力态降低 `maxItems` 或快速拒绝 |
| `EXPLAIN`    | `dbflow_explain_sql`                                                       | 中优先级，受目标库并发限制，压力态快速拒绝       |
| `EXECUTE`    | `dbflow_execute_sql`、`dbflow_confirm_sql`                                  | 低优先级但安全关键，超过并发阈值直接返回忙碌，不排长队 |

`dbflow_confirm_sql` 虽然不直接执行 SQL，但它服务高风险 TRUNCATE 流程，必须和 `EXECUTE` 共享更保守的并发与速率策略，避免确认流量绕开写操作保护。

### 配置模型

新增 `dbflow.capacity.*` 配置树。命名保持 Spring Boot relaxed binding 风格。

```yaml
dbflow:
  capacity:
    enabled: true
    pressure:
      enabled: true
      target-pool-waiting-threshold: 1
      target-pool-active-ratio-threshold: 0.85
      jvm-memory-used-ratio-threshold: 0.85
    rate-limit:
      per-token:
        max-requests: 60
        window: 1m
      per-user:
        max-requests: 120
        window: 1m
      per-tool:
        LIGHT_READ:
          max-requests: 180
          window: 1m
        HEAVY_READ:
          max-requests: 60
          window: 1m
        EXPLAIN:
          max-requests: 60
          window: 1m
        EXECUTE:
          max-requests: 30
          window: 1m
    bulkhead:
      global-max-concurrent: 80
      acquire-timeout: 100ms
      classes:
        LIGHT_READ:
          max-concurrent: 40
          acquire-timeout: 200ms
        HEAVY_READ:
          max-concurrent: 20
          acquire-timeout: 100ms
        EXPLAIN:
          max-concurrent: 16
          acquire-timeout: 100ms
        EXECUTE:
          max-concurrent: 10
          acquire-timeout: 0ms
      per-token-max-concurrent: 4
      per-user-max-concurrent: 8
      per-target-max-concurrent: 6
    degradation:
      enabled: true
      heavy-read-max-items-under-pressure: 50
      reject-explain-under-pressure: true
      reject-execute-under-pressure: true
```

默认值用于单实例首版内网部署，不代表所有企业环境的最终容量。部署前必须通过压测根据 CPU、内存、metadata DB、目标库规格和
Hikari pool 配置校准。

### Capacity Guard

新增 `capacity` 领域包，承载运行时容量治理：

```text
capacity/
  configuration/
  properties/
  service/
  model/
  support/
```

职责划分：

- `CapacityProperties`：绑定并校验 `dbflow.capacity.*`。
- `McpToolClass`：定义 `LIGHT_READ`、`HEAVY_READ`、`EXPLAIN`、`EXECUTE`。
- `CapacityDecision`：表达 `allowed`、`degraded`、`rejected`、`reasonCode`、`retryAfter`、`notices`。
- `CapacityGuardService`：统一执行 rate limit、bulkhead、目标并发、压力态判断。
- `InMemoryWindowRateLimiter`：进程内固定窗口或滑动窗口限流，key 支持 token/user/tool/target 组合。
- `SemaphoreBulkheadRegistry`：为全局、工具类别、Token、用户、目标 project/env 管理并发 permit。
- `SystemPressureService`：基于 Hikari MXBean、JVM memory、bulkhead 拒绝率判断压力态，不主动执行目标库 SQL。
- `CapacityMetricsService`：记录容量类指标。

MCP tool 入口必须在鉴权上下文解析之后、访问目标库或执行 SQL 之前调用 `CapacityGuardService`。对于需要目标 project/env
的工具，容量判断必须包含目标维度；对于轻量元数据工具，容量判断只使用 token/user/tool/global 维度。

### 执行顺序

`dbflow_execute_sql` 的服务端顺序调整为：

1. HTTP guard：Origin、request size、source IP rate limit。
2. Bearer Token authentication。
3. MCP authentication context resolve。
4. Capacity guard：global/tool/token/user/target permit 与业务限流。
5. Access decision：用户对 project/env 的授权。
6. SQL classification。
7. Dangerous DDL policy。
8. TRUNCATE confirmation 或 JDBC execution。
9. Audit event。
10. Metrics and response。

容量拒绝必须发生在目标库连接获取之前。授权仍然保留在容量之后，原因是容量层只使用已认证主体和请求参数做资源保护，不做安全授权判断；真正的
allow/deny 仍由 `AccessDecisionService` 和 SQL policy 决定。

`dbflow_inspect_schema`、schema resource、`dbflow_explain_sql` 同样必须在目标 datasource lookup 和 JDBC connection
获取之前完成容量判断。

### 降级策略

压力态分为两类：

- `LOCAL_PRESSURE`：全局或工具类别 bulkhead 接近耗尽、JVM memory 超阈值、容量拒绝率持续升高。
- `TARGET_PRESSURE`：某个目标 Hikari pool active ratio 超阈值或 threads awaiting connection 大于阈值。

压力态下按以下规则处理：

- `LIGHT_READ`：继续服务，但响应 `notices` 中标明系统处于压力态。
- `HEAVY_READ`：若请求 `maxItems` 大于降级上限，服务端自动压到 `heavy-read-max-items-under-pressure`，响应标明降级；如果
  bulkhead 已满则返回 `CAPACITY_BUSY`。
- `EXPLAIN`：若 `reject-explain-under-pressure=true` 且目标处于压力态，返回 `CAPACITY_BUSY`，不获取目标连接。
- `EXECUTE`：若 `reject-execute-under-pressure=true` 且全局或目标处于压力态，返回 `CAPACITY_BUSY`，不执行 SQL，不创建
  TRUNCATE challenge。
- `CONFIRM_SQL`：压力态下允许确认请求通过容量检查的前提是仍有 `EXECUTE` permit；不允许通过确认请求绕开 execute 保护。

容量拒绝响应使用稳定错误结构：

```json
{
  "error": {
    "code": "CAPACITY_BUSY",
    "message": "DBFlow 当前繁忙，请稍后重试",
    "retryAfterMillis": 1000,
    "reasonCode": "TARGET_BULKHEAD_FULL"
  }
}
```

客户端响应不得包含 Java stack trace、JDBC URL、数据库密码、Token 明文或 SQL 全量结果。

### JDK 21 虚拟线程

JDK 21 虚拟线程可以用于 DBFlow，但定位是降低阻塞式 JDBC/Servlet 请求对平台线程的占用，不是容量控制手段。

接受的决策：

- 允许提供 `spring.threads.virtual.enabled=true` 的部署选项。
- 在单实例部署中，虚拟线程必须与 `dbflow.capacity.*` 同时使用；不得因为启用虚拟线程而放宽 bulkhead、rate limit 或 Hikari
  pool 上限。
- 虚拟线程默认不强制开启。生产启用前必须通过并发压测和 pinned virtual thread 观察。
- 文档必须明确：Java 21 `Executors.newVirtualThreadPerTaskExecutor()` 是每任务创建虚拟线程且线程数无界，因此不能用它替代业务级并发阈值。
- Spring Boot 3.5 在 Java 21+ 可通过 `spring.threads.virtual.enabled=true` 启用虚拟线程；Tomcat 会将请求执行切换到虚拟线程
  executor。该模式仍要受 Tomcat 连接数、DBFlow bulkhead、Hikari pool 和目标库容量约束。

拒绝的做法：

- 不在业务代码里为每个 SQL 请求手工创建 `newVirtualThreadPerTaskExecutor()`。
- 不为 SQL 执行引入无界虚拟线程队列。
- 不用虚拟线程包裹长时间排队等待 Hikari 连接；目标库连接不足时应按 bulkhead/connection-timeout 快速失败。

验证要求：

- 在虚拟线程关闭和开启两种模式下分别运行并发压测。
- 使用日志、JFR 或 `jcmd` 观察 pinned virtual thread 风险；如果出现明显 pinned 导致吞吐下降或延迟恶化，生产默认保持关闭。
- 对比 `dbflow.capacity.rejections`、Hikari `waiting`、SQL duration p95/p99、JVM memory，确认虚拟线程没有掩盖目标库过载。

### Tomcat 与 HTTP 层

单实例部署仍应配置 Tomcat/HTTP 边界：

- `server.tomcat.threads.max`：平台线程模式下应显式配置；虚拟线程模式下仍需关注连接数与请求体读取。
- `server.tomcat.max-connections`：限制并发连接数，避免 keep-alive 或客户端重试堆积。
- `server.tomcat.accept-count`：限制连接队列，避免过量排队。
- 反向代理保持 `client_max_body_size 1m`，与 DBFlow request-size limit 对齐。
- `/mcp` 请求必须继续要求每次请求携带 Bearer Token，`Mcp-Session-Id` 不承载认证状态。

这些配置属于部署建议和 Nacos/dev 模板文档更新范围。应用内部容量治理仍由 `dbflow.capacity.*` 决定。

### 指标与健康

新增指标：

- `dbflow.capacity.requests{toolClass,decision,reason}`：容量判断次数。
- `dbflow.capacity.rejections{toolClass,reason}`：容量拒绝次数。
- `dbflow.capacity.degradations{toolClass,reason}`：降级次数。
- `dbflow.capacity.bulkhead.active{scope,name}`：当前占用 permit。
- `dbflow.capacity.bulkhead.available{scope,name}`：剩余 permit。
- `dbflow.capacity.rate_limit.exhausted{scope}`：限流耗尽次数。
- `dbflow.capacity.acquire.duration{toolClass,decision}`：permit 获取耗时。
- `dbflow.target.pool.pressure{project,env,status}`：目标 pool 压力状态，标签必须做低基数裁剪。

健康页和 Actuator health 增加容量摘要：

- 全局 capacity 状态：`HEALTHY`、`DEGRADED`、`BUSY`。
- 每个目标 pool 保留现有 `active/idle/total/waiting`。
- 当 `waiting > 0` 或 active ratio 超阈值时，健康详情展示目标处于压力态，但不主动访问目标库。

排障 runbook 增加场景：

- `CAPACITY_BUSY`。
- `TOKEN_RATE_LIMITED`。
- `TOOL_BULKHEAD_FULL`。
- `TARGET_BULKHEAD_FULL`。
- `TARGET_POOL_PRESSURE`。
- 虚拟线程启用后的 pinned thread 检查。

### 测试与验证

单元测试：

- `CapacityPropertiesTests`：默认值、非法阈值、工具类别配置校验。
- `InMemoryWindowRateLimiterTests`：按 token/user/tool/target 维度限流，窗口推进后恢复。
- `SemaphoreBulkheadRegistryTests`：permit 获取、超时、释放、异常释放、不同 scope 隔离。
- `SystemPressureServiceTests`：Hikari active ratio、waiting、JVM memory、拒绝率触发压力态。

集成测试：

- `McpCapacityGuardSecurityTests`：容量拒绝发生在目标库连接获取之前，响应无密钥泄露。
- `DbflowMcpToolsCapacityTests`：四类 tool 映射到正确 tool class。
- `SqlExecutionCapacityTests`：execute 超并发返回 `CAPACITY_BUSY`，不会写 executed audit，不会执行目标 SQL。
- `SchemaInspectDegradationTests`：压力态下 `maxItems` 被降到配置上限并返回 notice。
- `VirtualThreadConfigurationTests`：开启 `spring.threads.virtual.enabled=true` 时应用上下文可启动，容量 guard 仍生效。

压测验收：

- 模拟 100 个开发用户、至少 4 种客户端标识、混合调用比例：
    - 35% `listTargets/getEffectivePolicy`
    - 25% `inspectSchema`
    - 20% `explainSql`
    - 15% `executeSql` read-only SELECT
    - 5% DML/DDL dry-run 或 policy-denied 场景
- 单实例持续 10 分钟，额外执行 2 分钟突增流量。
- 验收指标：
    - `/actuator/health` 不返回 `DOWN`。
    - metadata DB 无连接耗尽。
    - 任一目标 Hikari pool `waiting` 不持续大于阈值。
    - `LIGHT_READ` p95 延迟保持在可交互范围内。
    - `EXECUTE` 在压力态快速返回 `CAPACITY_BUSY`，不出现长时间挂起。
    - 客户端响应无 stack trace、JDBC URL、数据库密码、Token 明文。
    - 启用虚拟线程模式时，吞吐或 p95/p99 延迟不得显著劣化；若劣化则生产默认关闭虚拟线程。

## 文档更新

实施时必须同步更新：

- `docs/ARCHITECTURE.md`：新增 `capacity` 包和容量治理链路。
- `docs/OBSERVABILITY.md`：新增容量指标、健康状态、压测命令与虚拟线程说明。
- `docs/deployment/README.md`：新增单实例内网容量配置建议、Tomcat 边界、虚拟线程开关说明。
- `docs/deployment/nacos/dev/application-dbflow.yml`：新增 `dbflow.capacity.*` 示例与注释。
- `docs/runbooks/troubleshooting.md`：新增容量拒绝、目标压力、虚拟线程排障场景。
- `docs/user-guide/operator-guide.md`：说明 `CAPACITY_BUSY`、`retryAfterMillis` 和安全重试建议。

## 风险与缓解

| 风险               | 缓解                                                      |
|------------------|---------------------------------------------------------|
| 限流过紧导致用户误以为服务不可用 | 默认保守但可配置，响应携带 `retryAfterMillis` 和 reason code，文档给出调参路径 |
| 限流过松导致目标库被打满     | 目标 project/env bulkhead 独立于全局并发，且必须小于 Hikari pool 的有效容量 |
| source IP 聚合误伤   | 入口 source IP 限流只做粗防护，业务级限流按 Token/用户/工具/目标执行            |
| 虚拟线程掩盖真实过载       | 虚拟线程只作为执行模式，不改变容量阈值；上线前压测并观察 pinned thread              |
| 容量拒绝绕过审计         | 容量拒绝至少记录安全日志和容量指标；如已具备审计上下文，可写入轻量 denied audit          |
| 复杂配置增加运维负担       | dev Nacos 模板给出可复制默认值，runbook 给出指标解释和调参顺序                |

## 验收标准

- 新增 `capacity` 包和配置模型，所有 public Java 类型具备中文 JavaDoc 与参数完整注释。
- 所有 MCP tool/resource 进入目标库访问前都经过容量判断。
- 单个 Token、单个用户、单个目标 project/env、单个工具类别都可被独立限流或并发限制。
- 压力态下 `LIGHT_READ` 优先服务，`HEAVY_READ` 可降级，`EXPLAIN` 和 `EXECUTE` 可快速拒绝。
- `CAPACITY_BUSY`、`TOKEN_RATE_LIMITED`、`TARGET_BULKHEAD_FULL` 等错误结构稳定、脱敏、可排障。
- 虚拟线程作为可选配置记录在部署文档；不开启时系统行为不变，开启时容量 guard 仍生效。
- 新增容量指标和健康摘要，不暴露敏感信息。
- 并发压测报告记录平台线程模式和虚拟线程模式结果，并给出生产默认开关结论。
- `./mvnw test`、`python3 scripts/check_harness.py`、`git diff --check` 全部通过。

## 后续扩展

如果未来从单实例演进到多实例部署，需要重新设计：

- 分布式 rate limiter。
- 跨实例目标库并发配额。
- 统一 gateway/WAF 策略。
- 共享容量状态与熔断状态。
- 多实例压测和故障注入。

这些扩展不属于本设计实施范围。
