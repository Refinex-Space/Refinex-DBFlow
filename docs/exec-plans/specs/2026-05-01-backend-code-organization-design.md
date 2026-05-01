# Backend Code Organization Design

## 背景

当前后端已经完成 MCP、SQL 执行、审计、安全、Admin
UI、部署与运行时硬化等主要能力，但源码组织仍保留早期快速推进时的平铺形态。一级包边界已经存在，二级语义边界不足，导致配置模型、Filter、DTO/record、页面
view model、工具方法和业务服务混在同一层级。

本设计定义一次后端代码规范化专项：保持外部行为不变，按职责清洗包结构、抽离内部 DTO/record、收敛重复工具能力，并顺手拆分少数超大类的协作组件。

## 现状证据

- `src/main/java/com/refinex/dbflow/executor` 当前 24 个 Java 文件全部平铺在一级包下。
- `src/main/java/com/refinex/dbflow/security` 当前 18 个 Java 文件全部平铺在一级包下。
- `src/main/java/com/refinex/dbflow/mcp` 当前 14 个 Java 文件全部平铺在一级包下。
- `src/main/java/com/refinex/dbflow/sqlpolicy` 当前 14 个 Java 文件全部平铺在一级包下。
- `DbflowProperties` 约 988 行，集中承载配置根、嵌套配置类、校验逻辑和工具方法。
- `AdminOperationsViewService` 约 957 行，集中承载审计页、配置页、危险策略页、健康页 view model 与转换逻辑。
- `DbflowMcpTools` 约 861 行，集中承载多个 MCP tool、响应 Map 装配、错误脱敏与指标记录。
- `SqlExecutionService` 约 759 行，集中承载授权、分类、策略、确认、JDBC 执行、结果转换、审计和指标记录。
- `AdminAccessManagementService` 约 748 行，内部定义用户、Token、授权相关 command/filter/row/view record。
- `isBlank`、`displayText`、`safeSqlText`、`truncate`、`sanitize`、JDBC URL 脱敏、password/token 脱敏等能力在多个包中重复出现。
- `DbflowMcpTools`、`DbflowMcpResources`、`SqlExecutionService` 存在包级通配导入，说明领域内类型过度拥挤。

## 目标

- 建立稳定的后端二级包规则，让类名、包名和职责一眼可判定。
- 将服务类内部的公开 command/filter/view/row/result record 移为顶层类型。
- 将重复通用能力抽到明确的 `common.util` 或领域内 `support` 类。
- 拆分少数超大类的协作职责，使服务类保留编排逻辑，细节转换和格式化下沉到专门组件。
- 保持所有外部契约不变，包括 MCP tool/resource/prompt 名称、HTTP 路由、Thymeleaf 模板路径、配置键、数据库 schema、审计事件语义和安全策略。
- 更新 `package-info.java`、架构文档和测试导入，使文档与真实源码结构一致。

## 非目标

- 不改 Maven 单模块结构。
- 不新增业务功能。
- 不改变 MCP 协议行为、Admin UI 页面行为或数据库执行策略。
- 不调整数据库 migration。
- 不引入新的格式化器、Checkstyle、PMD、SpotBugs 或 SonarQube 配置。
- 不把当前领域包改成全局横向的 `controller/service/dto/repository` 顶层结构。

## 方案比较

### 方案 A：只移动 DTO/record，不拆大类

只把内部公开 record 移到顶层类型，并补少量 `dto`、`view`、`command` 包。该方案风险最低，但 `DbflowMcpTools`、
`AdminOperationsViewService`、`SqlExecutionService` 仍然偏大，重复工具逻辑仍会继续扩散。

结论：拒绝。它能解决“看起来平铺”的问题，但不能解决变更热点和重复逻辑。

### 方案 B：领域优先二级包 + 适度拆大类

保留现有一级领域边界，在每个领域包内部建立二级语义包；把公开 DTO/record
移为顶层类型；将高复用文本处理、脱敏、hash、展示格式化能力集中；把大类拆成编排类和协作组件。外部契约不变，测试按移动后的类型更新。

结论：接受。该方案和当前单模块、领域边界、Harness 文档最匹配，风险可控，收益明确。

### 方案 C：全局技术分层重排

把全仓库改成顶层 `controller/service/dto/config/filter/util` 等技术层包。该方案表面整齐，但会打散 DBFlow 当前按安全、MCP、SQL
policy、executor、audit 切分的业务边界，增加跨领域认知成本。

结论：拒绝。DBFlow 的核心风险在服务端安全策略和执行链路，领域边界比横向技术层更重要。

## 接受的设计

采用方案 B：领域优先二级包 + 适度拆大类。

### 全局包规则

- 一级包继续表示业务领域：`access`、`admin`、`audit`、`common`、`config`、`executor`、`mcp`、`observability`、`security`、
  `sqlpolicy`。
- 二级包表示类型职责；领域内只创建本次迁移实际承载类型的包：
    - `controller`：Spring MVC controller。
    - `service`：业务编排或领域服务。
    - `entity`：JPA entity。
    - `repository`：Spring Data repository。
    - `dto`：跨组件传递的数据 record 或 response。
    - `command`：写操作输入命令。
    - `view`：Thymeleaf 页面 view model 或页面行模型。
    - `properties`：`@ConfigurationProperties` 配置绑定类。
    - `configuration`：Spring `@Configuration` 类。
    - `filter`：Servlet / Spring Security filter。
    - `support`：领域内辅助组件、转换器、formatter、builder、mapper。
    - `util`：无状态、跨领域复用工具类；仅允许放在 `common.util`。
- 顶层 `common` 仅保留真正全局的异常、错误码、API 结果与通用工具；不承载领域 DTO。
- 不再新增 `import xxx.*` 通配导入。

### record 与内部类规则

- `public record` 不得嵌套在 `Service`、`Controller`、`Component` 或 `Configuration` 类内部。
- 页面 view model 移到对应领域的 `view` 包。
- 写操作命令移到对应领域的 `command` 包。
- 查询 filter / criteria 若只服务管理端页面，放到 `admin.view`；属于审计领域查询契约的类型统一放到 `audit.dto`。
- 私有内部 record 只允许作为单类实现细节存在，例如固定窗口计数器、短生命周期 key、一次性局部转换对象。若被测试断言、跨方法大量传递或字段超过
  3 个，移为顶层 `support` 或 `dto` 类型。
- 私有内部 class 只允许作为协议/框架适配细节存在，例如可重复读取 request body wrapper；若后续被第二个类复用，必须移入
  `support` 或 `filter` 包。

### 通用工具规则

新增或调整 `common.util`，集中承载跨领域重复能力：

- `TextUtils`：空白判断、trim-to-empty、trim-to-null、默认展示文本。
- `SensitiveTextSanitizer`：JDBC URL、password/pwd、identified by、token/access_token/authorization 脱敏。
- `HashUtils`：SHA-256 hash 等非业务专属 hash 能力。
- `TruncationUtils`：安全截断与展示摘要裁剪。

领域专属规则不得硬塞进全局 util：

- SQL 风险分类仍归 `sqlpolicy`。
- 审计事件组装仍归 `audit`。
- MCP 响应结构仍归 `mcp`。
- Admin 页面色调、状态映射、导航高亮仍归 `admin.support` 或 `admin.view`。

### Admin 包设计

目标结构：

```text
admin/
  controller/
  service/
  command/
  view/
  support/
```

调整要求：

- `AdminHomeController` 移入 `admin.controller`。
- `AdminAuditEventController` 移入 `admin.controller`。
- `AdminAccessManagementService`、`AdminOverviewViewService`、`AdminOperationsViewService`、`AdminShellViewService` 保留在
  `admin.service` 或拆成更细服务。
- `UserFilter`、`TokenFilter`、`GrantFilter` 移为顶层 filter/view 类型。
- `CreateUserCommand`、`IssueTokenCommand`、`GrantEnvironmentCommand`、`UpdateProjectGrantsCommand` 移入 `admin.command`。
- `UserRow`、`TokenRow`、`GrantRow`、`GrantGroupRow`、`GrantEnvEntry`、`UserOption`、`EnvironmentOption`、`IssuedTokenView` 移入
  `admin.view`。
- `OverviewPageView`、`MetricCard`、`RecentAuditRow`、`AttentionItem` 等总览模型移入 `admin.view`。
- `AuditPageView`、`AuditSummaryRow`、`AuditFilterView`、`AuditDetailPageView`、`ConfigPageView`、`ConfigRow`、
  `DangerousPolicyPageView`、`PolicyDefaultRow`、`PolicyWhitelistRow`、`PolicyRuleRow`、`HealthPageView`、`HealthItem` 移入
  `admin.view`。
- `JdbcParts`、JDBC URL 安全展示解析、页面展示文本格式化等移入 `admin.support`。

### MCP 包设计

目标结构：

```text
mcp/
  tool/
  resource/
  prompt/
  auth/
  dto/
  support/
  configuration/
```

调整要求：

- `DbflowMcpTools` 移入 `mcp.tool`，并拆出响应装配/错误元数据/结果映射组件。
- `DbflowMcpResources` 移入 `mcp.resource`，并复用 MCP 响应装配组件，避免 tool/resource 各写一套
  schema/table/column/index/view/routine Map 转换。
- `DbflowMcpPrompts` 移入 `mcp.prompt`。
- `McpAuthenticationContext`、`McpAuthenticationContextResolver`、`SecurityContextMcpAuthenticationContextResolver`、
  `McpAuthorizationBoundary`、`McpAccessBoundaryService`、`DefaultMcpAccessBoundaryService` 移入 `mcp.auth`。
- `DbflowMcpToolConfiguration` 移入 `mcp.configuration`。
- `DbflowMcpNames` 移入 `mcp.support`，全量更新导入但不改变常量值。
- `DbflowMcpSkeletonResponse`、`DbflowMcpSmokeTool.DbflowMcpSmokeResponse` 移入 `mcp.dto`。
- MCP 输出中的脱敏统一调用 `SensitiveTextSanitizer`。

### Executor 包设计

目标结构：

```text
executor/
  service/
  datasource/
  dto/
  support/
```

调整要求：

- `SqlExecutionService`、`SqlExplainService`、`SchemaInspectService` 移入 `executor.service`。
- `HikariDataSourceRegistry`、`ProjectEnvironmentDataSourceRegistry`、`DataSourceConfigReloader`、`DataSourceReloadResult`
  移入 `executor.datasource`。
- `SqlExecutionRequest`、`SqlExecutionOptions`、`SqlExecutionResult`、`SqlExecutionWarning`、`SqlExplainRequest`、
  `SqlExplainResult`、`SqlExplainPlanRow`、`SqlExplainAdvice`、`SchemaInspectRequest`、`SchemaInspectResult`、
  `SchemaDatabaseMetadata`、`SchemaTableMetadata`、`SchemaColumnMetadata`、`SchemaIndexMetadata`、`SchemaViewMetadata`、
  `SchemaRoutineMetadata` 移入 `executor.dto`。
- `SqlExecutionService` 拆出至少以下协作组件：
    - `SqlExecutionAuditor`：封装 request received、denied、confirmation、executed、failed 审计写入。
    - `SqlJdbcExecutor`：封装 JDBC statement 执行、结果集映射、影响行数、截断判断。
    - `SqlExecutionResultFactory`：封装 deny、dry-run、confirmation-required、failure result 创建。
- `SqlExplainService` 拆出 `SqlExplainPlanMapper` 和 `SqlExplainAdviceGenerator`。
- `SchemaInspectService` 拆出 `SchemaMetadataMapper`。
- `HikariDataSourceRegistry.DataSourceKey` 保留私有 record，因为它只服务注册表内部 key 映射。

### Security 包设计

目标结构：

```text
security/
  configuration/
  properties/
  filter/
  token/
  request/
  support/
```

调整要求：

- `AdminSecurityConfiguration`、`McpSecurityConfiguration`、`ActuatorSecurityConfiguration` 移入 `security.configuration`。
- `AdminSecurityProperties`、`McpEndpointSecurityProperties`、`McpTokenProperties` 移入 `security.properties`。
- `McpBearerTokenAuthenticationFilter`、`McpEndpointGuardFilter` 移入 `security.filter`。
- `McpTokenService`、`McpTokenIssueResult`、`McpTokenValidationResult`、`McpAuthenticationToken` 移入 `security.token`。
- `McpRequestMetadata`、`McpRequestMetadataExtractor` 移入 `security.request`。
- `McpSecurityErrorResponseWriter` 和 request body wrapper 支撑类移入 `security.support`。
- `McpEndpointGuardFilter.WindowCounter` 保留私有 record，因为它是过滤器固定窗口算法的单类实现细节。

### Config 包设计

目标结构：

```text
config/
  properties/
  model/
```

调整要求：

- `DbflowProperties` 移入 `config.properties`。
- `DangerousDdlDecision`、`DangerousDdlOperation` 移入 `config.model`。
- `DbflowProperties` 保留配置聚合根职责；本轮不拆 `DatasourceDefaults`、`Hikari`、`Project`、`Environment`、`Policies`、
  `DangerousDdl`、`WhitelistEntry` 等嵌套配置绑定类，避免扩大 Spring Boot 配置绑定风险。
- `DbflowProperties` 本轮只允许抽出校验/文本工具到 `config.support` 或 `common.util`，不得改变 `dbflow.*` YAML 结构。

### Sqlpolicy 包设计

目标结构：

```text
sqlpolicy/
  service/
  dto/
  model/
  support/
```

调整要求：

- `SqlClassifier`、`DangerousDdlPolicyEngine`、`TruncateConfirmationService` 移入 `sqlpolicy.service`。
- `SqlClassification`、`DangerousDdlPolicyDecision`、`TruncateConfirmationRequest`、`TruncateConfirmationConfirmRequest`、
  `TruncateConfirmationDecision` 移入 `sqlpolicy.dto`。
- `SqlOperation`、`SqlParseStatus`、`SqlRiskLevel`、`SqlStatementType`、`DangerousDdlPolicyReasonCode` 移入
  `sqlpolicy.model`。
- `SqlClassifier.SqlTarget` 保留私有 record，因为它只服务 SQL 解析实现。

### Observability 包设计

目标结构：

```text
observability/
  filter/
  service/
  dto/
  configuration/
  support/
```

调整要求：

- `RequestIdFilter` 移入 `observability.filter`。
- `DbflowHealthService`、`DbflowMetricsService` 移入 `observability.service`。
- `DbflowHealthIndicators` 移入 `observability.configuration`。
- `LogContext` 移入 `observability.support`。
- `HealthSnapshot`、`HealthComponent`、`TargetDatasourceHealth` 移入 `observability.dto`。
- 健康错误脱敏复用 `SensitiveTextSanitizer`。

### Audit 包设计

当前 `audit/entity`、`audit/repository`、`audit/service` 已有二级包，保留该方向。

调整要求：

- `AuditEventDetail`、`AuditEventPageResponse`、`AuditEventSummary`、`AuditEventWriteRequest`、`AuditQueryCriteria`、
  `AuditRequestContext` 统一移入 `audit.dto`。
- 移除 `AuditTextSanitizer` 重复实现，审计展示脱敏统一调用 `SensitiveTextSanitizer`，并保留必要测试。
- 审计脱敏能力的覆盖面不得降低，必须继续覆盖 JDBC URL、password/pwd、`identified by`、token/access_token/authorization。

### Access 包设计

当前 `access/entity`、`access/repository`、`access/service` 已有二级包，保留该方向。

调整要求：

- `AccessDecision`、`AccessDecisionRequest`、`ConfiguredEnvironmentView` 移入 `access.dto`。
- `AccessDecisionReason` 移入 `access.model`。

## 迁移顺序

1. 建立新的二级包目录和 `package-info.java`。
2. 先迁移纯 DTO/record/enum，更新导入，保证测试仍能编译。
3. 收敛 `common.util`，用测试覆盖脱敏、截断、hash 和展示文本行为。
4. 迁移 Admin view/command/filter 类型，更新 controller/service/tests/templates 相关引用。
5. 迁移 MCP tool/resource/auth/dto/support 类型，保持 MCP 名称和响应字段不变。
6. 迁移 executor datasource/dto/service/support 类型，并拆 `SqlExecutionService` 协作组件。
7. 迁移 security configuration/properties/filter/token/request/support 类型。
8. 迁移 observability dto/filter/service/support 类型。
9. 迁移 config/sqlpolicy 类型，确保配置绑定和 SQL policy 测试不变。
10. 更新 `docs/ARCHITECTURE.md`、`docs/OBSERVABILITY.md`、`docs/generated/harness-manifest.md` 中真实包结构。
11. 删除废弃导入、通配导入和空包。

## 测试策略

- 迁移期间优先跑小范围测试，按包分批验证。
- 完整完成前必须通过：
    - `./mvnw test`
    - `python3 scripts/check_harness.py`
    - `git diff --check`
- 必须增加或保留以下回归断言：
    - 脱敏工具覆盖 JDBC URL、password/pwd、`identified by`、token/access_token/authorization。
    - 配置绑定键保持兼容。
    - MCP discovery 中 tool/resource/prompt 名称保持不变。
    - Admin controller 路由、模板名、flash message 行为保持不变。
    - SQL 执行链路仍保持授权、策略、执行、审计顺序。

## 验收标准

- `src/main/java/com/refinex/dbflow` 下主要领域包不再以大量 Java 文件平铺在一级包为主。
- `admin`、`mcp`、`executor`、`security`、`observability` 至少完成本设计指定的二级包拆分。
- 服务类内部不再保留公开 command/filter/view/result record。
- `src/main/java` 不再存在 `import com.refinex.dbflow.*.*;` 形式的领域通配导入。
- 通用脱敏、截断、hash、文本默认值能力有统一入口，且旧测试不降级。
- `DbflowMcpTools`、`AdminOperationsViewService`、`SqlExecutionService` 至少各拆出一个有独立职责的协作组件。
- 外部行为保持不变，所有既有测试通过。
- 架构文档与真实包路径一致。

## 风险与控制

- 大规模包移动可能造成导入 churn。控制方式：按领域分批迁移，每批运行对应测试。
- `DbflowProperties` 拆分可能影响 Spring Boot 配置绑定。控制方式：本轮不拆嵌套配置绑定类，只移动包并抽离非绑定 helper。
- MCP 输出字段或 tool 名称可能因响应装配重构变化。控制方式：保留 MCP discovery 和 server 测试，并增加字段级断言。
- Admin view model 移动可能影响 Thymeleaf 表达式。控制方式：保持 record accessor 名称不变，跑 Admin controller/UI 测试。
- 脱敏工具收敛可能降低覆盖面。控制方式：以 `AuditTextSanitizer` 当前能力为最低基线，新增统一工具测试后再替换调用点。

## 实施后文档要求

- 更新 `docs/ARCHITECTURE.md` 的源码树。
- 更新相关 `package-info.java`，说明二级包职责与依赖方向。
- 若新增 `common.util`，在 `docs/references/java-development-standards.md` 增加工具类使用边界。
- 若测试命令或质量命令有变化，同步更新 `docs/OBSERVABILITY.md`。
