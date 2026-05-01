# Backend Code Organization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `harness-execute` semantics to implement this plan task by task. Use
`harness-dispatch` only for independent subtasks with disjoint write scopes. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Normalize the DBFlow backend package structure, extract nested public records, consolidate shared utilities,
and split selected oversized classes without changing external behavior.

**Architecture:** Keep the existing domain-first boundaries (`admin`, `mcp`, `executor`, `security`, `sqlpolicy`,
`audit`, `access`, `config`, `observability`, `common`) and add second-level semantic packages inside each domain.
Service classes remain orchestration points; DTOs, commands, views, formatters, mappers, sanitizers, and JDBC helpers
move into explicit packages/components. Public routes, MCP names, configuration keys, database schema, SQL policy order,
audit semantics, and Thymeleaf template paths must remain unchanged.

**Tech Stack:** Java 21, Spring Boot WebMVC, Spring Security, Spring AI MCP Streamable HTTP, Spring Data JPA, Flyway,
HikariCP, JSQLParser, Thymeleaf, JUnit, Maven.

**Design Source:** `docs/exec-plans/specs/2026-05-01-backend-code-organization-design.md`

---

## Scope

- Create second-level packages and `package-info.java` files for backend domains.
- Move public nested records into top-level `command`, `dto`, `view`, or `model` packages.
- Move configuration, filter, token, request, resource, tool, prompt, datasource, and service classes into the accepted
  target packages.
- Add shared utilities under `common.util` for text normalization, sensitive text sanitization, hash generation, and
  safe truncation.
- Split `DbflowMcpTools`, `DbflowMcpResources`, `AdminOperationsViewService`, `SqlExecutionService`,
  `SqlExplainService`, and `SchemaInspectService` with explicit support components.
- Update imports, tests, architecture documentation, Java standards, observability documentation, and generated Harness
  manifest.
- Exclude feature work, behavior changes, Maven module splitting, database migrations, new lint tooling, and external
  API changes.

## Decisions

- Use domain-first packages rather than top-level technical layers because DBFlow correctness depends on preserving
  security, MCP, SQL policy, executor, and audit boundaries.
- Move `DbflowMcpNames` to `mcp.support` and keep constant values unchanged.
- Move audit query/write DTOs to `audit.dto`.
- Move access decision DTOs to `access.dto` and access reason enum to `access.model`.
- Move `DbflowProperties` to `config.properties` but keep nested binding classes inside it for this refactor.
- Remove the duplicate `AuditTextSanitizer` implementation after `SensitiveTextSanitizer` covers the same cases.
- Keep private implementation records such as `HikariDataSourceRegistry.DataSourceKey`,
  `McpEndpointGuardFilter.WindowCounter`, and `SqlClassifier.SqlTarget` private when they remain single-class
  implementation details.
- Do not keep wildcard imports in production or test Java files.

## Files

- Create packages under `src/main/java/com/refinex/dbflow/`: `common/util`, `admin/controller`, `admin/service`,
  `admin/command`, `admin/view`, `admin/support`, `mcp/tool`, `mcp/resource`, `mcp/prompt`, `mcp/auth`, `mcp/dto`,
  `mcp/support`, `mcp/configuration`, `executor/service`, `executor/datasource`, `executor/dto`, `executor/support`,
  `security/configuration`, `security/properties`, `security/filter`, `security/token`, `security/request`,
  `security/support`, `config/properties`, `config/model`, `sqlpolicy/service`, `sqlpolicy/dto`, `sqlpolicy/model`,
  `sqlpolicy/support`, `observability/filter`, `observability/service`, `observability/dto`,
  `observability/configuration`, `observability/support`, `audit/dto`, `access/dto`, `access/model`.
- Modify Java files currently under `src/main/java/com/refinex/dbflow/**` whose packages or imports change.
- Modify tests under `src/test/java/com/refinex/dbflow/**` to follow moved types and add utility/support tests.
- Modify `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/references/java-development-standards.md`, and
  `docs/generated/harness-manifest.md`.
- Modify `docs/PLANS.md` only for active/completed lifecycle indexing during this plan.

## Verification Strategy

- Run targeted package tests after each major migration batch.
- Run focused behavior tests for Admin routing, MCP discovery/server behavior, SQL execution, SQL explain, schema
  inspect, security filters, audit sanitization, and configuration binding.
- Final required commands:

```bash
./mvnw test
python3 scripts/check_harness.py
git diff --check
```

Expected final evidence: Maven test suite passes, Harness validator reports all checks passed, and whitespace check
reports no errors.

## Risks

- Security risk: moving filters or token classes can accidentally change filter ordering or authentication context
  wiring. Control by running security and MCP tests after the security/MCP migration.
- Correctness risk: moving SQL executor classes can break authorization -> policy -> execution -> audit ordering.
  Control with existing SQL execution tests and targeted assertions.
- Compatibility risk: moving `DbflowProperties` can break configuration scanning. Control with `DbflowPropertiesTests`,
  `MetadataSchemaMigrationTests`, and application context tests.
- UI risk: moving Admin view records can break Thymeleaf expressions. Control by keeping record accessor names unchanged
  and running Admin controller/UI tests.
- Audit risk: sanitizer consolidation can weaken redaction. Control by adding explicit utility tests for JDBC URL,
  `password`, `pwd`, `identified by`, `token`, `access_token`, and `authorization`.

## Task 1: Establish Package Skeleton And Shared Utility Baseline

**Files:**

- Create: `src/main/java/com/refinex/dbflow/common/util/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/common/util/TextUtils.java`
- Create: `src/main/java/com/refinex/dbflow/common/util/SensitiveTextSanitizer.java`
- Create: `src/main/java/com/refinex/dbflow/common/util/HashUtils.java`
- Create: `src/main/java/com/refinex/dbflow/common/util/TruncationUtils.java`
- Create: `src/test/java/com/refinex/dbflow/common/TextUtilsTests.java`
- Create: `src/test/java/com/refinex/dbflow/common/SensitiveTextSanitizerTests.java`
- Create: `src/test/java/com/refinex/dbflow/common/HashUtilsTests.java`
- Create: `src/test/java/com/refinex/dbflow/common/TruncationUtilsTests.java`
- Modify: `src/main/java/com/refinex/dbflow/common/package-info.java`

**Decision Trace:** Implements "通用工具规则" and "Audit 包设计" sanitizer baseline from the spec.

- [x] **Step 1: Create shared utility classes with Chinese JavaDoc and private constructors.**

Implement:

- `TextUtils.hasText`, `TextUtils.trimToNull`, `TextUtils.trimToEmpty`, `TextUtils.displayText`.
- `SensitiveTextSanitizer.sanitize`, covering JDBC URL, quoted/unquoted `password`/`pwd`, SQL `identified by`, `token`,
  `access_token`, and `authorization`.
- `HashUtils.sha256Hex`.
- `TruncationUtils.truncate`.

- [x] **Step 2: Add utility tests before replacing call sites.**

Run:

```bash
./mvnw -Dtest=TextUtilsTests,SensitiveTextSanitizerTests,HashUtilsTests,TruncationUtilsTests test
```

Expected: utility tests pass and demonstrate that sanitizer behavior is at least as strong as current audit sanitizer
behavior.

**Evidence:** Created `TextUtils`, `SensitiveTextSanitizer`, `HashUtils`, and `TruncationUtils` with Chinese JavaDoc and
private constructors. Added 11 focused utility tests. Ran
`./mvnw -Dtest=TextUtilsTests,SensitiveTextSanitizerTests,HashUtilsTests,TruncationUtilsTests test`; first run failed
because the test expected an incorrect SHA-256 value for `dbflow`, then the expected digest was corrected. Final run
passed with 11 tests, 0 failures, 0 errors, 0 skipped.

## Task 2: Move Access And Audit Contracts Out Of Service Packages

**Files:**

- Create: `src/main/java/com/refinex/dbflow/access/dto/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/access/model/package-info.java`
- Move: `src/main/java/com/refinex/dbflow/access/service/AccessDecision.java` ->
  `src/main/java/com/refinex/dbflow/access/dto/AccessDecision.java`
- Move: `src/main/java/com/refinex/dbflow/access/service/AccessDecisionRequest.java` ->
  `src/main/java/com/refinex/dbflow/access/dto/AccessDecisionRequest.java`
- Move: `src/main/java/com/refinex/dbflow/access/service/ConfiguredEnvironmentView.java` ->
  `src/main/java/com/refinex/dbflow/access/dto/ConfiguredEnvironmentView.java`
- Move: `src/main/java/com/refinex/dbflow/access/service/AccessDecisionReason.java` ->
  `src/main/java/com/refinex/dbflow/access/model/AccessDecisionReason.java`
- Create: `src/main/java/com/refinex/dbflow/audit/dto/package-info.java`
- Move: `src/main/java/com/refinex/dbflow/audit/service/AuditEventDetail.java` ->
  `src/main/java/com/refinex/dbflow/audit/dto/AuditEventDetail.java`
- Move: `src/main/java/com/refinex/dbflow/audit/service/AuditEventPageResponse.java` ->
  `src/main/java/com/refinex/dbflow/audit/dto/AuditEventPageResponse.java`
- Move: `src/main/java/com/refinex/dbflow/audit/service/AuditEventSummary.java` ->
  `src/main/java/com/refinex/dbflow/audit/dto/AuditEventSummary.java`
- Move: `src/main/java/com/refinex/dbflow/audit/service/AuditEventWriteRequest.java` ->
  `src/main/java/com/refinex/dbflow/audit/dto/AuditEventWriteRequest.java`
- Move: `src/main/java/com/refinex/dbflow/audit/service/AuditQueryCriteria.java` ->
  `src/main/java/com/refinex/dbflow/audit/dto/AuditQueryCriteria.java`
- Move: `src/main/java/com/refinex/dbflow/audit/service/AuditRequestContext.java` ->
  `src/main/java/com/refinex/dbflow/audit/dto/AuditRequestContext.java`
- Delete: `src/main/java/com/refinex/dbflow/audit/service/AuditTextSanitizer.java`
- Modify: access/audit services and tests that import moved types.

**Decision Trace:** Implements "Audit 包设计" and "Access 包设计".

- [x] **Step 1: Move DTO/model files and update packages/imports.**

Use explicit imports only. Preserve class names, record components, enum constants, JavaDoc, and public method
signatures that callers observe.

- [x] **Step 2: Replace audit text sanitizer call sites.**

Change audit query/detail sanitization to call `SensitiveTextSanitizer.sanitize`. Keep behavior for blank values
unchanged.

- [x] **Step 3: Verify access and audit behavior.**

Run:

```bash
./mvnw -Dtest=AccessServiceJpaTests,AccessDecisionServiceJpaTests,AuditAndConfirmationServiceJpaTests,AuditEventWriterTests,AuditQueryServiceTests test
```

Expected: access decisions, audit writing/querying, confirmation behavior, and sanitization assertions pass.

**Evidence:** Moved access decision DTO/model types into `access.dto` and `access.model`; moved audit query/write DTOs
into `audit.dto`; deleted duplicate `AuditTextSanitizer`; changed `AuditQueryService` to call `SensitiveTextSanitizer`.
Verification had two compile-feedback iterations for missing imports in Admin and audit tests after package moves. Final
command
`./mvnw -Dtest=AccessServiceJpaTests,AccessDecisionServiceJpaTests,AuditAndConfirmationServiceJpaTests,AuditEventWriterTests,AuditQueryServiceTests test`
passed with 19 tests, 0 failures, 0 errors, 0 skipped.

## Task 3: Normalize Admin Controller, Service, Command, View, And Support Packages

**Files:**

- Create: `src/main/java/com/refinex/dbflow/admin/controller/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/admin/service/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/admin/command/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/admin/view/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/admin/support/package-info.java`
- Move: `src/main/java/com/refinex/dbflow/admin/AdminHomeController.java` ->
  `src/main/java/com/refinex/dbflow/admin/controller/AdminHomeController.java`
- Move: `src/main/java/com/refinex/dbflow/admin/AdminAuditEventController.java` ->
  `src/main/java/com/refinex/dbflow/admin/controller/AdminAuditEventController.java`
- Move: `src/main/java/com/refinex/dbflow/admin/AdminAccessManagementService.java` ->
  `src/main/java/com/refinex/dbflow/admin/service/AdminAccessManagementService.java`
- Move: `src/main/java/com/refinex/dbflow/admin/AdminOverviewViewService.java` ->
  `src/main/java/com/refinex/dbflow/admin/service/AdminOverviewViewService.java`
- Move: `src/main/java/com/refinex/dbflow/admin/AdminOperationsViewService.java` ->
  `src/main/java/com/refinex/dbflow/admin/service/AdminOperationsViewService.java`
- Move: `src/main/java/com/refinex/dbflow/admin/AdminShellViewService.java` ->
  `src/main/java/com/refinex/dbflow/admin/service/AdminShellViewService.java`
- Create: command/view/support records and helpers extracted from moved services.
- Modify: Admin tests under `src/test/java/com/refinex/dbflow/admin/`.

**Decision Trace:** Implements "Admin 包设计" and the accepted split of public nested records.

- [x] **Step 1: Move Admin controller/service classes into second-level packages.**

Update imports in controllers, services, security config, and tests. Keep route annotations, template return strings,
flash message texts, and model attribute names unchanged.

- [x] **Step 2: Extract Admin command records.**

Create top-level records for `CreateUserCommand`, `IssueTokenCommand`, `GrantEnvironmentCommand`, and
`UpdateProjectGrantsCommand` in `admin.command`. Update controller and service call sites.

- [x] **Step 3: Extract Admin filter and view records.**

Move `UserFilter`, `TokenFilter`, `GrantFilter`, user/token/grant rows, overview view models, operations page view
models, policy rows, audit rows, config rows, and health rows into `admin.view`.

- [x] **Step 4: Extract Admin support components.**

Create support helpers for JDBC URL safe display parsing, common display text formatting, and operations page row
mapping. `AdminOperationsViewService` must remain the orchestration surface and delegate conversion details to support
classes.

- [x] **Step 5: Verify Admin behavior.**

Run:

```bash
./mvnw -Dtest=AdminAccessManagementControllerTests,AdminAccessManagementServiceTests,AdminAuditEventControllerTests,AdminConfigRealDataControllerTests,AdminOperationsPageControllerTests,AdminOverviewRealDataControllerTests,AdminThemeControllerTests,AdminUiControllerTests test
```

Expected: routes, templates, flash messages, redaction, real-data views, permissions, and theme behavior pass.

**Evidence:** Moved Admin controllers into `admin.controller` and Admin services into `admin.service`. Extracted command
records into `admin.command` and view records into `admin.view`, including `ShellView`. The two previous nested
`EnvironmentOption` records had incompatible shapes, so they were split into `GrantEnvironmentOption` and
`OverviewEnvironmentOption` while preserving model attribute names and template accessor behavior. Added
`AdminDisplayFormatter` and `JdbcUrlSummaryParser`/`JdbcParts` under `admin.support`, and changed operations/config view
mapping to delegate JDBC URL parsing. Ran
`./mvnw -Dtest=AdminAccessManagementControllerTests,AdminAccessManagementServiceTests,AdminAuditEventControllerTests,AdminConfigRealDataControllerTests,AdminOperationsPageControllerTests,AdminOverviewRealDataControllerTests,AdminThemeControllerTests,AdminUiControllerTests test`;
passed with 29 tests, 0 failures, 0 errors, 0 skipped.

## Task 4: Normalize Security Packages Without Changing Filter Semantics

**Files:**

- Create: `src/main/java/com/refinex/dbflow/security/configuration/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/security/properties/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/security/filter/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/security/token/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/security/request/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/security/support/package-info.java`
- Move security configuration, properties, filter, token, request metadata, and support classes per spec.
- Modify: `src/main/java/com/refinex/dbflow/security/package-info.java`
- Modify: security tests under `src/test/java/com/refinex/dbflow/security/`.

**Decision Trace:** Implements "Security 包设计".

- [x] **Step 1: Move security configuration and properties.**

Move `AdminSecurityConfiguration`, `McpSecurityConfiguration`, `ActuatorSecurityConfiguration`,
`AdminSecurityProperties`, `McpEndpointSecurityProperties`, and `McpTokenProperties`. Ensure `@ConfigurationProperties`
scanning still binds the same prefixes.

- [x] **Step 2: Move filters and request/token support.**

Move `McpBearerTokenAuthenticationFilter`, `McpEndpointGuardFilter`, `McpRequestMetadata`,
`McpRequestMetadataExtractor`, `McpSecurityErrorResponseWriter`, `McpTokenService`, `McpTokenIssueResult`,
`McpTokenValidationResult`, and `McpAuthenticationToken`. Preserve filter order in `McpSecurityConfiguration`.

- [x] **Step 3: Extract cached request body support.**

Move the reusable request body wrapper to `security.support` while keeping `WindowCounter` private in
`McpEndpointGuardFilter`.

- [x] **Step 4: Verify security behavior.**

Run:

```bash
./mvnw -Dtest=AdminSecurityTests,McpEndpointGuardSecurityTests,McpSecurityTests,McpTokenServiceJpaTests test
```

Expected: admin login, MCP bearer token authentication, endpoint guard checks, query token rejection,
origin/size/rate-limit behavior, and token lifecycle tests pass.

**Evidence:** Moved security configuration/properties/filter/token/request/support classes into explicit second-level
packages and added package docs. Extracted the cached-body wrapper from `McpEndpointGuardFilter` into
`security.support.CachedBodyRequest` while leaving `WindowCounter` private in the filter. Compile feedback required
explicit imports across moved main classes and three security tests after package boundaries changed. Ran
`./mvnw -Dtest=AdminSecurityTests,McpEndpointGuardSecurityTests,McpSecurityTests,McpTokenServiceJpaTests test`; passed
with 20 tests, 0 failures, 0 errors, 0 skipped.

## Task 5: Normalize MCP Tool, Resource, Auth, Prompt, DTO, And Support Packages

**Files:**

- Create: `src/main/java/com/refinex/dbflow/mcp/tool/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/mcp/resource/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/mcp/prompt/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/mcp/auth/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/mcp/dto/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/mcp/support/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/mcp/configuration/package-info.java`
- Move MCP names, tools, resources, prompts, auth context/boundary services, DTOs, and configuration per spec.
- Create: `src/main/java/com/refinex/dbflow/mcp/support/McpResponseBuilder.java`
- Create: `src/main/java/com/refinex/dbflow/mcp/support/McpSchemaMetadataMapper.java`
- Create: `src/main/java/com/refinex/dbflow/mcp/support/McpErrorMetadataFactory.java`
- Modify: MCP tests under `src/test/java/com/refinex/dbflow/mcp/`.

**Decision Trace:** Implements "MCP 包设计" and MCP behavior preservation requirements.

- [x] **Step 1: Move MCP classes into accepted packages.**

Keep all `@McpTool`, `@McpResource`, and prompt names/descriptions compatible with existing discovery tests. Move
`DbflowMcpNames` to `mcp.support` without changing constant names or values.

- [x] **Step 2: Extract shared MCP response/support logic.**

Move response `Map` construction, schema metadata mapping, and error metadata creation out of `DbflowMcpTools`/
`DbflowMcpResources`. Use `SensitiveTextSanitizer` for MCP error text sanitization.

- [x] **Step 3: Remove wildcard executor imports.**

Replace all `import com.refinex.dbflow.executor.*` and similar domain wildcard imports with explicit imports.

- [x] **Step 4: Verify MCP behavior.**

Run:

```bash
./mvnw -Dtest=DbflowMcpDiscoveryTests,DbflowMcpServerTests test
```

Expected: MCP tool/resource/prompt discovery, authentication/authorization boundary use, and smoke behavior remain
unchanged.

**Evidence:** Moved MCP tools, resources, prompts, auth boundary/context, DTOs, constants, and tool registration into
`mcp.tool`, `mcp.resource`, `mcp.prompt`, `mcp.auth`, `mcp.dto`, `mcp.support`, and `mcp.configuration`. Extracted
`DbflowMcpSmokeResponse` from the smoke tool, moved shared skeleton/resource/map construction into `McpResponseBuilder`,
centralized MCP error metadata and sanitization in `McpErrorMetadataFactory`, and moved schema/warning/EXPLAIN mapping
into `McpSchemaMetadataMapper`. Removed the `executor.*` wildcard import from MCP tool/resource code. Ran
`./mvnw -Dtest=DbflowMcpDiscoveryTests,DbflowMcpServerTests test`; passed with 4 tests, 0 failures, 0 errors, 0 skipped.

## Task 6: Normalize Executor Datasource, DTO, Service, And Support Packages

**Files:**

- Create: `src/main/java/com/refinex/dbflow/executor/service/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/executor/datasource/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/executor/dto/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/executor/support/package-info.java`
- Move executor datasource classes, executor DTO records, and executor service classes per spec.
- Create: `src/main/java/com/refinex/dbflow/executor/support/SqlExecutionAuditor.java`
- Create: `src/main/java/com/refinex/dbflow/executor/support/SqlJdbcExecutor.java`
- Create: `src/main/java/com/refinex/dbflow/executor/support/SqlExecutionResultFactory.java`
- Create: `src/main/java/com/refinex/dbflow/executor/support/SqlExplainPlanMapper.java`
- Create: `src/main/java/com/refinex/dbflow/executor/support/SqlExplainAdviceGenerator.java`
- Create: `src/main/java/com/refinex/dbflow/executor/support/SchemaMetadataMapper.java`
- Modify: executor tests under `src/test/java/com/refinex/dbflow/executor/`.

**Decision Trace:** Implements "Executor 包设计" and the behavior-preserving split of SQL execution/explain/inspect
services.

- [x] **Step 1: Move datasource classes and DTO records.**

Move `HikariDataSourceRegistry`, `ProjectEnvironmentDataSourceRegistry`, `DataSourceConfigReloader`, and
`DataSourceReloadResult` into `executor.datasource`. Move all SQL execution, explain, and schema inspect records into
`executor.dto`.

- [x] **Step 2: Move executor services and update imports.**

Move `SqlExecutionService`, `SqlExplainService`, and `SchemaInspectService` into `executor.service`. Replace wildcard
imports in production and test code.

- [x] **Step 3: Split `SqlExecutionService`.**

Move audit write orchestration into `SqlExecutionAuditor`, JDBC execution/result set mapping into `SqlJdbcExecutor`, and
result creation into `SqlExecutionResultFactory`. Preserve authorization -> classification -> dangerous policy ->
confirmation -> execution -> audit order.

- [x] **Step 4: Split explain and schema inspect mapping.**

Move explain plan row mapping/advice generation and schema metadata mapping into support classes while preserving MySQL
8/5.7 behavior and bounded results.

- [x] **Step 5: Verify executor behavior.**

Run:

```bash
./mvnw -Dtest=DataSourceConfigReloaderTests,HikariDataSourceRegistryTests,SqlExecutionServiceMysql8Tests,SqlExplainServiceTests,SqlExplainServiceMysqlTests,SchemaInspectServiceTests,SchemaInspectServiceMysqlTests test
```

Expected: datasource reload/registry, SQL execution, EXPLAIN, and schema inspect tests pass with unchanged external
results.

**Evidence:** Moved datasource classes into `executor.datasource`, SQL/schema/explain/data-source result records into
`executor.dto`, and executor services into `executor.service`; added package docs and updated imports in MCP,
observability, services, and executor tests. Split `SqlExecutionService` by moving audit routing into
`SqlExecutionAuditor`, JDBC execution/result-set handling into `SqlJdbcExecutor`, JDBC failure details into
`SqlJdbcExecutionException`, and result construction into `SqlExecutionResultFactory`. Split EXPLAIN and schema inspect
mapping by adding `SqlExplainPlanMapper`, `SqlExplainAdviceGenerator`, and `SchemaMetadataMapper`. Ran
`./mvnw -Dtest=DataSourceConfigReloaderTests,HikariDataSourceRegistryTests,SqlExecutionServiceMysql8Tests,SqlExplainServiceTests,SqlExplainServiceMysqlTests,SchemaInspectServiceTests,SchemaInspectServiceMysqlTests test`;
passed with 22 tests, 0 failures, 0 errors, 10 skipped. The skipped tests are Testcontainers MySQL tests because this
environment has no valid Docker socket (`/var/run/docker.sock`).

## Task 7: Normalize Config, SQL Policy, And Observability Packages

**Files:**

- Create: `src/main/java/com/refinex/dbflow/config/properties/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/config/model/package-info.java`
- Move: `DbflowProperties`, `DangerousDdlDecision`, `DangerousDdlOperation`
- Create: `src/main/java/com/refinex/dbflow/sqlpolicy/service/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/sqlpolicy/dto/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/sqlpolicy/model/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/sqlpolicy/support/package-info.java`
- Move SQL policy service, DTO, and model classes per spec.
- Create: `src/main/java/com/refinex/dbflow/observability/filter/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/observability/service/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/observability/dto/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/observability/configuration/package-info.java`
- Create: `src/main/java/com/refinex/dbflow/observability/support/package-info.java`
- Move observability filter, service, DTO, configuration, and support classes per spec.
- Modify: config, sqlpolicy, and observability tests.

**Decision Trace:** Implements "Config 包设计", "Sqlpolicy 包设计", and "Observability 包设计".

- [x] **Step 1: Move config types without changing binding shape.**

Move `DbflowProperties` to `config.properties` and config enums to `config.model`. Keep nested configuration binding
classes inside `DbflowProperties` and keep all `dbflow.*` keys unchanged.

- [x] **Step 2: Move SQL policy classes.**

Move classifier, dangerous DDL policy, truncate confirmation services, SQL model enums, and policy DTOs into accepted
packages. Keep `SqlClassifier.SqlTarget` private.

- [x] **Step 3: Move observability classes and DTOs.**

Move `RequestIdFilter`, health/metrics services, health indicators, `LogContext`, and health DTO records. Use
`SensitiveTextSanitizer` for health error details.

- [x] **Step 4: Verify config, policy, and observability behavior.**

Run:

```bash
./mvnw -Dtest=DbflowPropertiesTests,MetadataSchemaMigrationTests,NacosProfileConfigurationTests,DangerousDdlPolicyEngineTests,SqlClassifierTests,TruncateConfirmationServiceJpaTests,OperationalHealthAndMetricsTests,RequestIdFilterTests test
```

Expected: configuration binding, Nacos profile config, SQL policy decisions, classifier behavior, truncate confirmation,
health/metrics, and request ID behavior pass.

**Evidence:** Moved `DbflowProperties` into `config.properties` and dangerous DDL config enums into `config.model`
without changing the `dbflow.*` binding keys. Moved SQL policy services into `sqlpolicy.service`, policy DTO records
into `sqlpolicy.dto`, and enums/reason codes into `sqlpolicy.model`, keeping `SqlClassifier.SqlTarget` private. Moved
`RequestIdFilter`, health/metrics services, Actuator health indicators, and `LogContext` into `observability.filter`,
`observability.service`, `observability.configuration`, and `observability.support`; extracted `HealthSnapshot`,
`HealthComponent`, and `TargetDatasourceHealth` into `observability.dto`; changed health error details to use
`SensitiveTextSanitizer`. Ran
`./mvnw -Dtest=DbflowPropertiesTests,MetadataSchemaMigrationTests,NacosProfileConfigurationTests,DangerousDdlPolicyEngineTests,SqlClassifierTests,TruncateConfirmationServiceJpaTests,OperationalHealthAndMetricsTests,RequestIdFilterTests test`;
first run exposed old-package test references, then imports were corrected. Final run passed with 45 tests, 0 failures,
0 errors, 0 skipped.

## Task 8: Remove Wildcard Imports And Enforce Package Hygiene

**Files:**

- Modify: all production Java files under `src/main/java/com/refinex/dbflow/**`
- Modify: all test Java files under `src/test/java/com/refinex/dbflow/**`
- Modify: package-info files created or moved by prior tasks.

**Decision Trace:** Implements global package rules and acceptance criteria.

- [x] **Step 1: Remove domain wildcard imports.**

Search and replace all `import com.refinex.dbflow.*.*;` style wildcard imports with explicit type imports.

- [x] **Step 2: Remove empty packages and stale package-info files.**

Delete package-info files whose packages no longer exist. Ensure every new second-level package has a Chinese
package-level responsibility comment.

- [x] **Step 3: Scan for remaining nested public records.**

Run:

```bash
rg -n "public record|import com\\.refinex\\.dbflow\\.[a-z]+\\.\\*" src/main/java src/test/java
```

Expected: `public record` appears only in top-level record files or accepted DTO/view/model files; no domain wildcard
imports remain.

- [x] **Step 4: Compile after hygiene cleanup.**

Run:

```bash
./mvnw -DskipTests compile
```

Expected: main sources compile successfully.

**Evidence:** Replaced wildcard imports across production and test code, including executor package wildcards,
access/audit service/entity wildcards, SQL/JPA/HTTP wildcards, and static MockMvc result matcher wildcards.
`find src/main/java/com/refinex/dbflow -type d -empty -print` reported no empty package directories. Ran
`rg -n "import .*\\.\\*" src/main/java src/test/java`; no matches remained. Ran
`rg -n "public record|import com\\.refinex\\.dbflow\\.[a-z]+\\.\\*" src/main/java src/test/java`; record matches are
top-level DTO/view/command/model/support records, and no domain wildcard imports were reported. Ran
`./mvnw -DskipTests compile`; BUILD SUCCESS.

## Task 9: Update Architecture, Standards, Observability, And Harness Manifest

**Files:**

- Modify: `docs/ARCHITECTURE.md`
- Modify: `docs/OBSERVABILITY.md`
- Modify: `docs/references/java-development-standards.md`
- Modify: `docs/generated/harness-manifest.md`
- Modify: `docs/PLANS.md` during completion/archive only.

**Decision Trace:** Implements documentation requirements and Harness auditability.

- [x] **Step 1: Update architecture source tree and package descriptions.**

Reflect the real second-level package layout and keep descriptions factual. Do not document packages that do not exist
after implementation.

- [x] **Step 2: Update Java standards with package and utility boundaries.**

Add the rule that domain DTOs stay in domain packages, only cross-domain stateless utilities belong in `common.util`,
and no domain wildcard imports are allowed.

- [x] **Step 3: Update observability docs if commands or test counts change.**

Keep the documented final verification commands aligned with this plan.

- [x] **Step 4: Refresh Harness manifest.**

Run the repo-local Harness generator if present in `scripts/check_harness.py` guidance; otherwise update
`docs/generated/harness-manifest.md` manually to match actual Harness artifacts without inventing non-Harness source
files.

- [x] **Step 5: Verify Harness control plane.**

Run:

```bash
python3 scripts/check_harness.py
```

Expected: Harness validator passes.

**Evidence:** Updated `docs/ARCHITECTURE.md` package-boundary table to the current second-level semantic packages,
including `common.util`, `config.properties/model`, `security.*`, `access.dto/model`, `mcp.*`,
`sqlpolicy.service/dto/model/support`, `executor.datasource/dto/service/support`, `audit.dto`, `admin.*`, and
`observability.*`. Updated `docs/references/java-development-standards.md` with package organization, top-level public
record, wildcard import, and `common.util` reuse rules. Updated `docs/OBSERVABILITY.md` with package hygiene
verification. Refreshed `docs/generated/harness-manifest.md` verification dates to 2026-05-01. Ran
`python3 scripts/check_harness.py`; all 14 manifest artifacts passed.

## Task 10: Full Regression And Completion Gate

**Files:**

- Modify: `docs/exec-plans/active/2026-05-01-backend-code-organization.md` evidence fields during execution.
- Modify: `docs/PLANS.md` only when later archiving through `harness-finish`.

**Decision Trace:** Implements final verification strategy and acceptance criteria.

- [x] **Step 1: Run full Maven tests.**

Run:

```bash
./mvnw test
```

Expected: full test suite passes.

- [x] **Step 2: Run Harness validator.**

Run:

```bash
python3 scripts/check_harness.py
```

Expected: all Harness checks pass.

- [x] **Step 3: Run whitespace diff check.**

Run:

```bash
git diff --check
```

Expected: no whitespace errors.

- [x] **Step 4: Run final structural scans.**

Run:

```bash
rg -n "import com\\.refinex\\.dbflow\\.[a-z]+\\.\\*" src/main/java src/test/java
rg -n "public record" src/main/java/com/refinex/dbflow/admin src/main/java/com/refinex/dbflow/mcp src/main/java/com/refinex/dbflow/executor src/main/java/com/refinex/dbflow/observability
```

Expected: no wildcard imports; `public record` results are top-level DTO/view files or accepted record files, not nested
inside services/controllers/components/configurations.

- [x] **Step 5: Prepare completion handoff.**

Summarize package layout changes, behavior-preservation tests, remaining risks, and any skipped verification. Hand off
to `harness-verify` before claiming completion.

**Evidence:** Ran `./mvnw clean test`; first clean run exposed one remaining explicit-import cleanup issue for
`HashSet`, then the import was added and the final run passed with 154 tests, 0 failures, 0 errors, 10 skipped. The
skipped tests are Testcontainers MySQL tests because this environment has no valid Docker socket (
`/var/run/docker.sock`). Ran `python3 scripts/check_harness.py`; all 14 manifest artifacts passed. Ran
`git diff --check`; no whitespace errors were reported. Ran
`rg -n "import com\\.refinex\\.dbflow\\.[a-z]+\\.\\*" src/main/java src/test/java` and
`rg -n "import .*\\.\\*" src/main/java src/test/java`; both scans had no matches. Ran
`rg -n "public record" src/main/java/com/refinex/dbflow/admin src/main/java/com/refinex/dbflow/mcp src/main/java/com/refinex/dbflow/executor src/main/java/com/refinex/dbflow/observability`;
remaining records are top-level DTO, view, command, auth, and support records, not nested public records inside
services/controllers/components/configurations. Completion handoff is ready for `harness-verify`.
