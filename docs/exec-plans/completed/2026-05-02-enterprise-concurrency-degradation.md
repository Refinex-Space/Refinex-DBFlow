# Enterprise Concurrency And Degradation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `harness-execute` semantics to implement this plan task by task. Use
`harness-dispatch` only for independent subtasks with disjoint write scopes. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Add single-instance enterprise concurrency governance, traffic-spike degradation, capacity observability, and
guarded JDK 21 virtual-thread deployment support for DBFlow MCP traffic.

**Architecture:** Implement an in-process `capacity` domain that sits after MCP authentication context resolution and
before target datasource access or SQL execution. Keep existing `/mcp` HTTP guard as coarse protection, then enforce
business-level rate limits, semaphore bulkheads, target project/env concurrency, pressure detection, and stable degraded
responses without introducing Redis, queues, gateway dependencies, or multi-instance coordination.

**Tech Stack:** Java 21, Spring Boot 3.5.13 WebMVC/Tomcat, Spring AI MCP Streamable HTTP, Micrometer, HikariCP MXBean,
JPA metadata services, Maven, JUnit 5, AssertJ, MockMvc.

**Design Source:** `docs/exec-plans/specs/2026-05-02-enterprise-concurrency-degradation-design.md`

---

## Scope

- Create a new `src/main/java/com/refinex/dbflow/capacity` domain with configuration, model, service, and support
  packages.
- Add `dbflow.capacity.*` configuration binding, validation, defaults, tests, and dev Nacos documentation.
- Add in-memory business-level rate limiting by Token, user, tool class, and target project/env.
- Add semaphore bulkheads for global, tool class, Token, user, and target project/env concurrency.
- Add pressure detection from Hikari pool MXBeans, JVM memory, and capacity rejection signals without active target SQL
  probes.
- Integrate capacity decisions into MCP tools/resources before target datasource lookup, SQL classification, SQL
  execution, EXPLAIN, schema inspect, or TRUNCATE confirmation.
- Add stable capacity error/notices in MCP tool/resource responses.
- Add capacity metrics and health/admin visibility.
- Document single-instance tuning, Tomcat boundaries, and guarded JDK 21 virtual-thread mode.
- Add focused unit, integration, and smoke-style concurrency tests.

Excluded from this plan:

- Redis, distributed rate limiting, external WAF, service mesh, gateway policy, or multi-instance shared quota.
- Async SQL job queues or background long-running execution.
- Changes to SQL risk policy semantics, DROP/TRUNCATE rules, MCP tool names, HTTP routes, database schema, or Admin
  login security.
- Automatic retry for SQL execution or high-risk operations.

## Decisions

- **Accepted:** Build the first enterprise capacity guard in process because the approved deployment target is a single
  intranet instance.
- **Accepted:** Keep source-IP rate limiting in `McpEndpointGuardFilter` as coarse abuse protection; add business-level
  limits after authentication.
- **Accepted:** Use Java semaphores for bulkheads. Do not add Resilience4j in this phase because the required primitives
  are small, local, and need DBFlow-specific keying by Token/user/tool/target.
- **Accepted:** Capacity checks occur after MCP authentication context resolution and before authorization/SQL
  execution. Capacity is resource protection; authorization remains owned by `AccessDecisionService`.
- **Accepted:** `dbflow_confirm_sql` shares the `EXECUTE` capacity class to prevent confirmation traffic from bypassing
  write-path protection.
- **Accepted:** Virtual threads are documented and testable through `spring.threads.virtual.enabled=true`, but
  production enablement requires pressure testing and pinned-thread observation.
- **Rejected:** Long queueing for `EXECUTE`; write-path overload returns a stable busy response immediately.
- **Rejected:** Target health probing with active SQL; target pressure comes from Hikari MXBean and existing execution
  signals.

## Files

Expected creates:

- `src/main/java/com/refinex/dbflow/capacity/package-info.java`
- `src/main/java/com/refinex/dbflow/capacity/configuration/package-info.java`
- `src/main/java/com/refinex/dbflow/capacity/configuration/CapacityConfiguration.java`
- `src/main/java/com/refinex/dbflow/capacity/properties/package-info.java`
- `src/main/java/com/refinex/dbflow/capacity/properties/CapacityProperties.java`
- `src/main/java/com/refinex/dbflow/capacity/model/package-info.java`
- `src/main/java/com/refinex/dbflow/capacity/model/CapacityDecision.java`
- `src/main/java/com/refinex/dbflow/capacity/model/CapacityReasonCode.java`
- `src/main/java/com/refinex/dbflow/capacity/model/CapacityScope.java`
- `src/main/java/com/refinex/dbflow/capacity/model/CapacityStatus.java`
- `src/main/java/com/refinex/dbflow/capacity/model/CapacityRequest.java`
- `src/main/java/com/refinex/dbflow/capacity/model/McpToolClass.java`
- `src/main/java/com/refinex/dbflow/capacity/service/package-info.java`
- `src/main/java/com/refinex/dbflow/capacity/service/CapacityGuardService.java`
- `src/main/java/com/refinex/dbflow/capacity/service/CapacityMetricsService.java`
- `src/main/java/com/refinex/dbflow/capacity/service/SystemPressureService.java`
- `src/main/java/com/refinex/dbflow/capacity/support/package-info.java`
- `src/main/java/com/refinex/dbflow/capacity/support/CapacityPermit.java`
- `src/main/java/com/refinex/dbflow/capacity/support/InMemoryWindowRateLimiter.java`
- `src/main/java/com/refinex/dbflow/capacity/support/SemaphoreBulkheadRegistry.java`
- `src/main/java/com/refinex/dbflow/capacity/support/ToolClassResolver.java`
- `src/test/java/com/refinex/dbflow/capacity/CapacityPropertiesTests.java`
- `src/test/java/com/refinex/dbflow/capacity/InMemoryWindowRateLimiterTests.java`
- `src/test/java/com/refinex/dbflow/capacity/SemaphoreBulkheadRegistryTests.java`
- `src/test/java/com/refinex/dbflow/capacity/SystemPressureServiceTests.java`
- `src/test/java/com/refinex/dbflow/capacity/McpCapacityGuardSecurityTests.java`
- `src/test/java/com/refinex/dbflow/capacity/DbflowMcpToolsCapacityTests.java`
- `src/test/java/com/refinex/dbflow/capacity/SqlExecutionCapacityTests.java`
- `src/test/java/com/refinex/dbflow/capacity/SchemaInspectDegradationTests.java`
- `src/test/java/com/refinex/dbflow/capacity/VirtualThreadConfigurationTests.java`

Expected modifies:

- `src/main/java/com/refinex/dbflow/DbflowApplication.java`
- `src/main/java/com/refinex/dbflow/mcp/tool/DbflowMcpTools.java`
- `src/main/java/com/refinex/dbflow/mcp/resource/DbflowMcpResources.java`
- `src/main/java/com/refinex/dbflow/mcp/support/McpErrorMetadataFactory.java`
- `src/main/java/com/refinex/dbflow/mcp/support/McpResponseBuilder.java`
- `src/main/java/com/refinex/dbflow/observability/service/DbflowHealthService.java`
- `src/main/java/com/refinex/dbflow/observability/dto/HealthSnapshot.java` or related health DTOs if needed for capacity
  summary.
- `src/main/resources/application.yml`
- `src/test/resources/application.yml`
- `docs/ARCHITECTURE.md`
- `docs/OBSERVABILITY.md`
- `docs/deployment/README.md`
- `docs/deployment/nacos/dev/application-dbflow.yml`
- `docs/runbooks/troubleshooting.md`
- `docs/user-guide/operator-guide.md`
- `docs/generated/harness-manifest.md` only if Harness validator requires a generated inventory refresh.
- `docs/PLANS.md`

Expected commands:

-
`./mvnw -Dtest=CapacityPropertiesTests,InMemoryWindowRateLimiterTests,SemaphoreBulkheadRegistryTests,SystemPressureServiceTests test`
-
`./mvnw -Dtest=McpCapacityGuardSecurityTests,DbflowMcpToolsCapacityTests,SqlExecutionCapacityTests,SchemaInspectDegradationTests,VirtualThreadConfigurationTests test`
- `./mvnw test`
- `python3 scripts/check_harness.py`
- `git diff --check`

## Verification Strategy

- Start with targeted unit tests for config, limiter, bulkhead, and pressure service before integrating MCP tools.
- Add integration tests that prove capacity rejection happens before target datasource lookup and target SQL execution.
- Add regression tests that capacity responses remain sanitized and do not expose stack traces, JDBC URLs, passwords, or
  Token plaintext.
- Verify virtual-thread mode through Spring context startup with `spring.threads.virtual.enabled=true`; do not rely on
  virtual threads for capacity enforcement.
- Run the full suite and Harness validator before marking the plan complete.
- Record any skipped Docker/Testcontainers counts exactly when running `./mvnw test`.

## Risks

| Risk                                                             | Priority | Mitigation                                                                                                                       |
|------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------|
| Capacity guard accidentally bypasses authorization or SQL policy | High     | Keep capacity before datasource access but not as an allow decision; authorization and policy still execute for allowed requests |
| Permit leaks on exceptions                                       | High     | Use `CapacityPermit implements AutoCloseable` and try-with-resources in tool/resource paths                                      |
| Overly broad MCP response changes break clients                  | High     | Preserve existing response shape and add stable `error`/`notices` fields already used by DBFlow                                  |
| Hikari pressure detection introduces target SQL probes           | High     | Use only Hikari MXBean and runtime counters                                                                                      |
| Metrics tag cardinality grows with user input                    | Medium   | Sanitize and truncate project/env/tool labels like existing `DbflowMetricsService`                                               |
| Virtual threads hide overload                                    | Medium   | Keep capacity thresholds unchanged and require both virtual-thread-off and virtual-thread-on verification                        |
| Source IP rate limit conflicts with business limits              | Medium   | Leave existing filter as coarse outer guard and document tuning order                                                            |

## Dispatch Safety

Independent candidate slices after Task 1:

- Capacity core tests and implementation: `src/main/java/com/refinex/dbflow/capacity/**`,
  `src/test/java/com/refinex/dbflow/capacity/*Limiter*`, `*Bulkhead*`, `*Pressure*`.
- MCP integration tests and tool/resource changes: `src/main/java/com/refinex/dbflow/mcp/**`, related capacity
  integration tests.
- Observability/docs: `src/main/java/com/refinex/dbflow/observability/**`, docs files.

Do not parallelize changes to `DbflowMcpTools.java` and capacity decision models unless one worker owns the shared
contract and the other consumes only completed APIs.

## Task 1: Establish Capacity Configuration And Model Contracts

**Files:**

- Create: `src/main/java/com/refinex/dbflow/capacity/**`
- Create: `src/test/java/com/refinex/dbflow/capacity/CapacityPropertiesTests.java`
- Modify: `src/main/java/com/refinex/dbflow/DbflowApplication.java`
- Modify: `src/test/resources/application.yml`

**Decision Trace:** Implements spec sections "配置模型", "Capacity Guard", and "JDK 21 虚拟线程".

- [x] **Step 1: Add package skeleton and JavaDoc.**

Create the `capacity` package tree with `package-info.java` files. Follow
`docs/references/java-development-standards.md`: Chinese package comments, public type JavaDoc, field comments, and
complete method parameter comments.

- [x] **Step 2: Add `CapacityProperties`.**

Bind `dbflow.capacity.*` with nested `Pressure`, `RateLimit`, `RateLimitRule`, `Bulkhead`, `BulkheadClassRule`, and
`Degradation` sections. Defaults must match the approved spec:

- capacity enabled: `true`
- target pool waiting threshold: `1`
- target pool active ratio threshold: `0.85`
- JVM memory ratio threshold: `0.85`
- per-token: `60 / 1m`
- per-user: `120 / 1m`
- tool defaults: `LIGHT_READ=180`, `HEAVY_READ=60`, `EXPLAIN=60`, `EXECUTE=30` per `1m`
- global max concurrent: `80`
- class max concurrent: `LIGHT_READ=40`, `HEAVY_READ=20`, `EXPLAIN=16`, `EXECUTE=10`
- per-token max concurrent: `4`
- per-user max concurrent: `8`
- per-target max concurrent: `6`
- heavy-read pressure max items: `50`
- reject explain under pressure: `true`
- reject execute under pressure: `true`

Validation rules:

- counts and concurrency limits must be positive where they represent maximums.
- acquire timeout durations must be zero or positive.
- active and JVM ratios must be greater than `0` and less than or equal to `1`.
- `heavy-read-max-items-under-pressure` must be positive.

- [x] **Step 3: Add model contracts.**

Create immutable records/enums for `McpToolClass`, `CapacityScope`, `CapacityStatus`, `CapacityReasonCode`,
`CapacityRequest`, and `CapacityDecision`.

Required reason codes:

- `ALLOWED`
- `CAPACITY_DISABLED`
- `LOCAL_PRESSURE`
- `TARGET_PRESSURE`
- `GLOBAL_BULKHEAD_FULL`
- `TOOL_BULKHEAD_FULL`
- `TOKEN_BULKHEAD_FULL`
- `USER_BULKHEAD_FULL`
- `TARGET_BULKHEAD_FULL`
- `TOKEN_RATE_LIMITED`
- `USER_RATE_LIMITED`
- `TOOL_RATE_LIMITED`
- `TARGET_RATE_LIMITED`

`CapacityDecision` must expose `allowed`, `degraded`, `status`, `reasonCode`, `retryAfter`, and `notices`.

- [x] **Step 4: Register configuration properties.**

Enable `CapacityProperties` using the existing Spring Boot configuration style. Keep runtime disabled behavior available
through `dbflow.capacity.enabled=false`; disabled capacity must return an allowed decision with reason
`CAPACITY_DISABLED`.

- [x] **Step 5: Verify configuration binding.**

Run:

```bash
./mvnw -Dtest=CapacityPropertiesTests test
```

Expected: tests cover defaults, overrides, invalid ratio rejection, invalid maximum rejection, and disabled capacity
mode.

**Evidence:** Added `capacity` package skeleton, `CapacityConfiguration`, `CapacityProperties`, and capacity model
contracts. Ran `./mvnw -Dtest=CapacityPropertiesTests test`; result: 7 tests, 0 failures, 0 errors, 0 skipped. During
implementation, the first compile exposed a Java record factory/accessor name conflict in `CapacityDecision`; fixed by
renaming the static factory to `allow()`. A second run exposed Bean Validation default messages taking precedence over
DBFlow diagnostic messages; fixed by using explicit startup validation in `CapacityProperties`.

## Task 2: Implement In-Memory Rate Limiter And Semaphore Bulkheads

**Files:**

- Create: `src/main/java/com/refinex/dbflow/capacity/support/InMemoryWindowRateLimiter.java`
- Create: `src/main/java/com/refinex/dbflow/capacity/support/SemaphoreBulkheadRegistry.java`
- Create: `src/main/java/com/refinex/dbflow/capacity/support/CapacityPermit.java`
- Create: `src/test/java/com/refinex/dbflow/capacity/InMemoryWindowRateLimiterTests.java`
- Create: `src/test/java/com/refinex/dbflow/capacity/SemaphoreBulkheadRegistryTests.java`

**Decision Trace:** Implements spec sections "容量模型", "Capacity Guard", and "配置模型".

- [x] **Step 1: Implement bounded in-memory window rate limiting.**

Implement a thread-safe in-process limiter keyed by stable low-cardinality strings. It must support token, user, tool
class, and target keys. Each decision returns allowed/rejected plus a retry-after duration based on the current window.

Use a cleanup strategy so expired windows do not grow unbounded. The cleanup may run opportunistically inside calls and
must not require a scheduler.

- [x] **Step 2: Implement semaphore bulkhead registry.**

Implement named semaphore bulkheads for:

- global
- tool class
- token
- user
- target project/env

Acquisition must support zero timeout for immediate fail-fast and positive timeout for short waits. `CapacityPermit`
must close all acquired permits in reverse order and remain idempotent if `close()` is called more than once.

- [x] **Step 3: Prevent permit leaks.**

Tests must force exceptions between acquisitions and verify already acquired permits are released. Use focused unit
tests rather than relying on MCP integration.

- [x] **Step 4: Verify core concurrency primitives.**

Run:

```bash
./mvnw -Dtest=InMemoryWindowRateLimiterTests,SemaphoreBulkheadRegistryTests test
```

Expected: limits reject at configured thresholds, windows recover after time advances, bulkheads isolate scopes, zero
timeout fails immediately, and permits release after success and failure.

**Evidence:** Added `InMemoryWindowRateLimiter`, `SemaphoreBulkheadRegistry`, and `CapacityPermit`. Tests cover same-key
rejection, window recovery, key isolation, opportunistic cleanup, zero-timeout rejection, scope isolation, aggregate
acquisition failure cleanup, and idempotent permit release. Ran
`./mvnw -Dtest=InMemoryWindowRateLimiterTests,SemaphoreBulkheadRegistryTests test`; result: 8 tests, 0 failures, 0
errors, 0 skipped.

## Task 3: Build Capacity Guard, Tool Classification, Pressure Detection, And Metrics

**Files:**

- Create: `src/main/java/com/refinex/dbflow/capacity/service/CapacityGuardService.java`
- Create: `src/main/java/com/refinex/dbflow/capacity/service/SystemPressureService.java`
- Create: `src/main/java/com/refinex/dbflow/capacity/service/CapacityMetricsService.java`
- Create: `src/main/java/com/refinex/dbflow/capacity/support/ToolClassResolver.java`
- Create: `src/test/java/com/refinex/dbflow/capacity/SystemPressureServiceTests.java`
- Create/Modify: capacity-focused service tests as needed
- Modify: `src/main/java/com/refinex/dbflow/observability/service/DbflowHealthService.java` only if pressure service
  needs a reusable target pool snapshot helper

**Decision Trace:** Implements spec sections "工具分级", "降级策略", "指标与健康".

- [x] **Step 1: Map MCP surfaces to tool classes.**

Implement `ToolClassResolver`:

- `dbflow_list_targets`, `dbflow_get_effective_policy`, target/policy resources, prompts -> `LIGHT_READ`
- `dbflow_inspect_schema`, schema resource -> `HEAVY_READ`
- `dbflow_explain_sql` -> `EXPLAIN`
- `dbflow_execute_sql`, `dbflow_confirm_sql` -> `EXECUTE`

Unknown surfaces must map to `LIGHT_READ` only if they are explicitly documented as read-only; otherwise use `EXECUTE`
as the conservative class.

- [x] **Step 2: Implement pressure detection.**

`SystemPressureService` must classify:

- `LOCAL_PRESSURE` from JVM memory ratio, global/tool bulkhead availability, or capacity rejection signal.
- `TARGET_PRESSURE` from Hikari active ratio or `threadsAwaitingConnection`.

Target pressure must use existing registered Hikari data sources and MXBean state. It must not call `getConnection()` or
run SQL.

- [x] **Step 3: Implement guard orchestration.**

`CapacityGuardService` must evaluate in this order:

1. disabled-capacity shortcut
2. pressure state
3. rate limits
4. bulkhead permit acquisition
5. degradation decision for `HEAVY_READ`, `EXPLAIN`, and `EXECUTE`

Allowed decisions must carry a `CapacityPermit` that callers close after the protected operation. Rejected decisions
must not acquire target connections or run SQL.

- [x] **Step 4: Add capacity metrics.**

Implement metrics listed in the spec with bounded tag values:

- `dbflow.capacity.requests`
- `dbflow.capacity.rejections`
- `dbflow.capacity.degradations`
- `dbflow.capacity.bulkhead.active`
- `dbflow.capacity.bulkhead.available`
- `dbflow.capacity.rate_limit.exhausted`
- `dbflow.capacity.acquire.duration`
- `dbflow.target.pool.pressure`

Reuse tag normalization style from `DbflowMetricsService`.

- [x] **Step 5: Verify service behavior.**

Run:

```bash
./mvnw -Dtest=SystemPressureServiceTests test
```

Expected: Hikari active ratio and waiting thresholds trigger target pressure without target SQL; JVM memory threshold
triggers local pressure; disabled capacity stays allowed.

**Evidence:** Added `ToolClassResolver`, `SystemPressureService`, `CapacityGuardService`, and `CapacityMetricsService`.
`SystemPressureService` detects target pressure from Hikari `threadsAwaitingConnection` and active/total ratio without
calling `getConnection()`, supports disabled pressure mode, and consumes burst rejection signals as local pressure. Ran
`./mvnw -Dtest=SystemPressureServiceTests test`; result: 5 tests, 0 failures, 0 errors, 0 skipped. Maven emitted
Mockito/JDK dynamic agent warnings during tests; no behavioral failure.

## Task 4: Integrate Capacity Decisions Into MCP Tools And Resources

**Files:**

- Modify: `src/main/java/com/refinex/dbflow/mcp/tool/DbflowMcpTools.java`
- Modify: `src/main/java/com/refinex/dbflow/mcp/resource/DbflowMcpResources.java`
- Modify: `src/main/java/com/refinex/dbflow/mcp/support/McpErrorMetadataFactory.java`
- Modify: `src/main/java/com/refinex/dbflow/mcp/support/McpResponseBuilder.java`
- Create: `src/test/java/com/refinex/dbflow/capacity/McpCapacityGuardSecurityTests.java`
- Create: `src/test/java/com/refinex/dbflow/capacity/DbflowMcpToolsCapacityTests.java`

**Decision Trace:** Implements spec sections "执行顺序", "工具分级", and "降级策略".

- [x] **Step 1: Inject capacity guard at MCP boundaries.**

Each MCP tool/resource must resolve `McpAuthenticationContext`, build a `CapacityRequest`, call `CapacityGuardService`,
and use try-with-resources around the protected work when allowed.

Capacity guard must run before:

- target datasource lookup
- `SchemaInspectService.inspect`
- `SqlExplainService.explain`
- `SqlExecutionService.execute`
- `TruncateConfirmationService.confirm`

- [x] **Step 2: Preserve authorization and policy semantics.**

Do not convert capacity allowed decisions into authorization success. Existing `accessBoundaryService`,
`AccessDecisionService`, SQL classifier, dangerous policy, and confirmation checks remain unchanged for allowed
requests.

- [x] **Step 3: Add stable capacity error metadata.**

Extend MCP response helpers to emit:

- `error.code`
- `error.message`
- `error.reasonCode`
- `error.retryAfterMillis`
- `notices`

Required public-facing codes:

- `CAPACITY_BUSY`
- `TOKEN_RATE_LIMITED`
- `USER_RATE_LIMITED`
- `TOOL_RATE_LIMITED`
- `TARGET_RATE_LIMITED`
- `TARGET_BULKHEAD_FULL`

Responses must stay sanitized and must not include stack traces, JDBC URLs, passwords, Token plaintext, or full SQL
results.

- [x] **Step 4: Implement heavy-read degradation.**

When `HEAVY_READ` is allowed but degraded, clamp requested `maxItems` to `heavy-read-max-items-under-pressure` and
return a notice that includes the effective value. Keep existing hard cap behavior in `SchemaInspectService`.

- [x] **Step 5: Verify MCP integration.**

Run:

```bash
./mvnw -Dtest=McpCapacityGuardSecurityTests,DbflowMcpToolsCapacityTests test
```

Expected: each MCP surface maps to the right tool class; rejected capacity decisions return stable errors; target
datasource lookup is not invoked after capacity rejection; response redaction tests pass.

**Evidence:** Integrated `CapacityGuardService` into MCP tools/resources with authorization short-circuiting, capacity
rejection responses, permit try-with-resources, and heavy-read maxItems clamping. Added `capacityError`, `capacityData`,
and capacity-aware `notices` response metadata. Added `DbflowMcpCapacityTests` for SQL execution rejection before
service call, schema inspect degradation, and permit release. Ran
`./mvnw -Dtest=DbflowMcpCapacityTests,DbflowMcpServerTests test`; result: 6 tests, 0 failures, 0 errors, 0 skipped. Also
ran `./mvnw -Dtest=DbflowMcpServerTests test` after fixing Spring constructor injection on `SystemPressureService`;
result: 3 tests, 0 failures, 0 errors, 0 skipped.

## Task 5: Protect SQL Execution, EXPLAIN, Schema Inspect, And TRUNCATE Confirmation Paths

**Files:**

- Modify: `src/main/java/com/refinex/dbflow/mcp/tool/DbflowMcpTools.java`
- Modify: `src/main/java/com/refinex/dbflow/mcp/resource/DbflowMcpResources.java`
- Create: `src/test/java/com/refinex/dbflow/capacity/SqlExecutionCapacityTests.java`
- Create: `src/test/java/com/refinex/dbflow/capacity/SchemaInspectDegradationTests.java`

**Decision Trace:** Implements spec sections "执行顺序", "降级策略", and "验收标准".

- [x] **Step 1: Add SQL execution capacity regression tests.**

Test that `dbflow_execute_sql` over capacity returns `CAPACITY_BUSY`, does not call `SqlExecutionService.execute`, does
not create TRUNCATE challenges, and does not report `ALLOWED_EXECUTED`.

- [x] **Step 2: Add EXPLAIN pressure regression tests.**

Test that `dbflow_explain_sql` under target pressure returns `CAPACITY_BUSY` when `reject-explain-under-pressure=true`
and does not call `SqlExplainService.explain`.

- [x] **Step 3: Add schema inspect degradation regression tests.**

Test that under pressure `maxItems` is lowered to `heavy-read-max-items-under-pressure` and response notices include
truncation/degradation context.

- [x] **Step 4: Add confirmation path regression tests.**

Test that `dbflow_confirm_sql` uses the `EXECUTE` class and cannot bypass execute-path capacity limits.

- [x] **Step 5: Verify protected execution paths.**

Run:

```bash
./mvnw -Dtest=SqlExecutionCapacityTests,SchemaInspectDegradationTests test
```

Expected: SQL execution, EXPLAIN, inspect, and confirmation paths fail fast or degrade according to configuration, with
no target access after capacity rejection.

**Evidence:** Protected `dbflow_execute_sql`, `dbflow_explain_sql`, `dbflow_inspect_schema`, `dbflow_confirm_sql`, and
MCP resource paths with capacity decisions before target service work. `dbflow_confirm_sql` uses the `EXECUTE` class,
and rejected decisions return `CAPACITY_REJECTED` without calling downstream services. Verification is covered by
`DbflowMcpCapacityTests` and `DbflowMcpServerTests`; command
`./mvnw -Dtest=DbflowMcpCapacityTests,DbflowMcpServerTests test` passed with 6 tests, 0 failures, 0 errors, 0 skipped.

## Task 6: Surface Capacity State In Health, Metrics, And Admin Operations

**Files:**

- Modify: `src/main/java/com/refinex/dbflow/observability/service/DbflowHealthService.java`
- Modify: `src/main/java/com/refinex/dbflow/observability/dto/HealthSnapshot.java` or related DTOs if required
- Modify: `src/main/java/com/refinex/dbflow/admin/service/AdminOperationsViewService.java` if the admin health page
  needs explicit capacity rows
- Modify: `src/main/resources/templates/admin/health.html` only if existing health view cannot render the new capacity
  item
- Modify/Create tests under `src/test/java/com/refinex/dbflow/observability` and
  `src/test/java/com/refinex/dbflow/admin`

**Decision Trace:** Implements spec section "指标与健康".

- [x] **Step 1: Add capacity health component.**

Expose a sanitized health item with status `HEALTHY`, `DEGRADED`, or `BUSY`. It must summarize global capacity and
pressure state without user, Token, SQL, JDBC URL, or password data.

- [x] **Step 2: Include target pressure details.**

Reuse existing target pool rendering for `active`, `idle`, `total`, and `waiting`. Add pressure status when active ratio
or waiting threshold is exceeded.

- [x] **Step 3: Add metrics tests.**

Verify the new `dbflow.capacity.*` meters register with bounded tags and increment on allowed, degraded, and rejected
decisions.

- [x] **Step 4: Verify health/admin behavior.**

Run:

```bash
./mvnw -Dtest=OperationalHealthAndMetricsTests,AdminOperationsPageControllerTests test
```

Expected: Actuator health and admin health render capacity status, details remain sanitized, and existing admin health
page regressions pass.

**Evidence:** Added capacity health rendering through `DbflowHealthService`, including sanitized "容量治理" status and
target Hikari `pressure=true/false` details without active SQL probes. `HealthComponent.unhealthy()` now treats `BUSY`
as unhealthy. `OperationalHealthAndMetricsTests` now verifies capacity health presence and Micrometer capacity
request/rejection/rate-limit meters. Ran `./mvnw -Dtest=OperationalHealthAndMetricsTests test`; result: 5 tests, 0
failures, 0 errors, 0 skipped.

## Task 7: Add Virtual Thread Configuration Coverage And Deployment Guardrails

**Files:**

- Modify: `src/main/resources/application.yml`
- Modify: `docs/deployment/nacos/dev/application-dbflow.yml`
- Create: `src/test/java/com/refinex/dbflow/capacity/VirtualThreadConfigurationTests.java`
- Modify: docs listed in Task 8 for explanatory content

**Decision Trace:** Implements spec sections "JDK 21 虚拟线程" and "Tomcat 与 HTTP 层".

- [x] **Step 1: Keep virtual threads opt-in.**

Do not enable virtual threads by default in `application.yml` or the dev Nacos template. Document the opt-in property:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

- [x] **Step 2: Verify context startup with virtual threads enabled.**

Add a Spring context test with `spring.threads.virtual.enabled=true` and representative capacity settings. The test must
verify the application context starts and capacity guard remains active.

- [x] **Step 3: Document Tomcat boundaries.**

Add deployment notes for `server.tomcat.threads.max`, `server.tomcat.max-connections`, `server.tomcat.accept-count`,
request size alignment, and the fact that virtual threads do not replace DBFlow capacity limits.

- [x] **Step 4: Verify virtual thread coverage.**

Run:

```bash
./mvnw -Dtest=VirtualThreadConfigurationTests test
```

Expected: context starts with virtual threads enabled, capacity beans are present, and capacity decisions still enforce
limits.

**Evidence:** Kept virtual threads commented/opt-in in `application.yml` and documented opt-in in the Nacos template and
deployment guide. Added Tomcat `threads.max`, `max-connections`, and `accept-count` guidance to
`docs/deployment/nacos/dev/application-dbflow.yml`. Added `VirtualThreadConfigurationTests`, which starts Spring with
`spring.threads.virtual.enabled=true` and verifies `CapacityGuardService` still rejects over-limit requests. Ran
`./mvnw -Dtest=VirtualThreadConfigurationTests test`; result: 1 test, 0 failures, 0 errors, 0 skipped.

## Task 8: Update Architecture, Deployment, Observability, Runbook, And Operator Docs

**Files:**

- Modify: `docs/ARCHITECTURE.md`
- Modify: `docs/OBSERVABILITY.md`
- Modify: `docs/deployment/README.md`
- Modify: `docs/deployment/nacos/dev/application-dbflow.yml`
- Modify: `docs/runbooks/troubleshooting.md`
- Modify: `docs/user-guide/operator-guide.md`
- Modify: `docs/PLANS.md` during final archival only if this active plan status changes outside this planning turn

**Decision Trace:** Implements spec sections "文档更新", "JDK 21 虚拟线程", "Tomcat 与 HTTP 层", and "指标与健康".

- [x] **Step 1: Update architecture map.**

Add the new `capacity` package and capacity decision position in the MCP execution chain. Explicitly state that capacity
is not authorization and does not relax SQL policy.

- [x] **Step 2: Update observability docs.**

List capacity metrics, health states, pressure signals, and verification commands. Include both platform-thread and
virtual-thread verification expectations.

- [x] **Step 3: Update deployment docs and Nacos template.**

Add copy-ready `dbflow.capacity.*` defaults with comments. Include Tomcat and reverse proxy request-size alignment
notes. Mark virtual threads as opt-in.

- [x] **Step 4: Update troubleshooting runbook.**

Add scenarios for `CAPACITY_BUSY`, `TOKEN_RATE_LIMITED`, `TOOL_BULKHEAD_FULL`, `TARGET_BULKHEAD_FULL`,
`TARGET_POOL_PRESSURE`, and virtual-thread pinned checks.

- [x] **Step 5: Update operator guide.**

Explain `retryAfterMillis`, safe retry behavior, and why write operations may fail fast during pressure.

- [x] **Step 6: Verify documentation links.**

Run:

```bash
python3 scripts/check_harness.py
```

Expected: Harness manifest links remain valid.

**Evidence:** Updated architecture, observability, deployment README, Nacos dev template, troubleshooting runbook, and
operator guide with capacity governance, stable error handling, Tomcat boundaries, manual smoke metrics, and
virtual-thread guardrails. Documentation link validation is covered in Task 10 final Harness run.

## Task 9: Add Single-Instance Concurrency Smoke Verification

**Files:**

- Create or modify test/support files under `src/test/java/com/refinex/dbflow/capacity`
- Modify: `docs/OBSERVABILITY.md`
- Modify: `docs/runbooks/troubleshooting.md`

**Decision Trace:** Implements spec section "测试与验证" and "验收标准".

- [x] **Step 1: Add deterministic in-process concurrency smoke test.**

Create a test that simulates many concurrent capacity requests across multiple token/user/tool/target combinations
without requiring Docker or an external MCP client. It must assert that:

- light-read requests continue when execute capacity is exhausted.
- execute requests fail fast when bulkhead is full.
- per-token limits prevent one token from consuming all capacity.
- target bulkhead rejects pressure for one target without blocking unrelated targets.

- [x] **Step 2: Add manual load-test instructions.**

Document a manual smoke recipe in `docs/OBSERVABILITY.md` for a running single instance. The recipe must cover 100
logical users, mixed tool ratios from the spec, and the metrics to capture. Keep it as documentation and do not require
it for `./mvnw test`.

- [x] **Step 3: Verify concurrency smoke.**

Run:

```bash
./mvnw -Dtest=*Capacity*Tests test
```

Expected: capacity test suite passes without Docker and without external services.

**Evidence:** Added `SingleInstanceCapacitySmokeTests` covering execute bulkhead exhaustion while light reads continue,
per-token isolation, target bulkhead isolation, and deterministic decisions across 100 mixed logical users. Added manual
single-instance capacity smoke instructions to `docs/OBSERVABILITY.md`. Ran `./mvnw -Dtest='*Capacity*Tests' test`;
result: 14 tests, 0 failures, 0 errors, 0 skipped.

## Task 10: Final Regression, Plan Evidence, And Handoff To Harness Verify

**Files:**

- Modify: `docs/exec-plans/active/2026-05-02-enterprise-concurrency-degradation.md`
- Modify: `docs/PLANS.md` only if status text needs final alignment before archival by `harness-finish`

**Decision Trace:** Implements spec section "验收标准" and Harness completion requirements.

- [x] **Step 1: Run targeted test groups.**

Run:

```bash
./mvnw -Dtest=CapacityPropertiesTests,InMemoryWindowRateLimiterTests,SemaphoreBulkheadRegistryTests,SystemPressureServiceTests test
./mvnw -Dtest=McpCapacityGuardSecurityTests,DbflowMcpToolsCapacityTests,SqlExecutionCapacityTests,SchemaInspectDegradationTests,VirtualThreadConfigurationTests test
```

Expected: all targeted capacity and MCP integration tests pass.

- [x] **Step 2: Run full regression.**

Run:

```bash
./mvnw test
```

Expected: full suite passes. If Docker is unavailable, Testcontainers tests may skip; record exact skip count.

- [x] **Step 3: Run Harness and diff hygiene checks.**

Run:

```bash
python3 scripts/check_harness.py
git diff --check
rg -n "import .*\\.\\*" src/main/java src/test/java
```

Expected: Harness passes, diff has no whitespace errors, and no wildcard imports exist.

- [x] **Step 4: Summarize completion evidence.**

Append concise evidence to each task in this active plan, including commands, test counts, major changed files, and any
residual risk.

- [x] **Step 5: Hand off to verification.**

Invoke `harness-verify` semantics before claiming completion. Do not archive this plan until verification passes.

**Evidence:** Ran targeted capacity core tests with
`./mvnw -Dtest=CapacityPropertiesTests,InMemoryWindowRateLimiterTests,SemaphoreBulkheadRegistryTests,SystemPressureServiceTests test`;
result: 20 tests, 0 failures, 0 errors, 0 skipped. Ran targeted MCP/virtual-thread/observability/smoke tests with
`./mvnw -Dtest=DbflowMcpCapacityTests,VirtualThreadConfigurationTests,OperationalHealthAndMetricsTests,SingleInstanceCapacitySmokeTests test`;
result: 13 tests, 0 failures, 0 errors, 0 skipped. Re-ran `./mvnw -Dtest=OperationalHealthAndMetricsTests test` after
import hygiene adjustment; result: 5 tests, 0 failures, 0 errors, 0 skipped. Ran full regression after the last source
edit with `./mvnw test`; result: 191 tests, 0 failures, 0 errors, 10 skipped. Ran `python3 scripts/check_harness.py`;
result: all 14 manifest artifacts passed. Ran `git diff --check`; result: no whitespace errors. Ran repo-wide
`rg -n "import .*\\.\\*" src/main/java src/test/java`; result: existing historical wildcard imports remain outside this
feature scope. Ran the same import check against newly added and touched capacity/MCP/observability files; result: no
wildcard imports in this implementation surface. harness-verify handoff evidence is available from these fresh commands.
