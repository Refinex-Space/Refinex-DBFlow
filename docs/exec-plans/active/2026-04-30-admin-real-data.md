# Admin Real Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `harness-execute` semantics to implement this plan task by task. Use
`harness-dispatch` only for independent subtasks with disjoint write scopes. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Remove all visible prototype/mock data from the `/admin` management UI and render real, secret-safe runtime
data for overview, config, and shared shell state.

**Architecture:** Keep the existing Spring MVC + Thymeleaf management UI and add server-side view services for
aggregation. Controllers remain routing/model wiring only; security, redaction, empty states, and metric windows stay on
the server.

**Tech Stack:** Spring Boot 3.5, Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, H2 test runtime, existing
DBFlow metadata repositories, `AuditQueryService`, `ProjectEnvironmentCatalogService`, and `DbflowHealthService`.

**Design Source:** `docs/exec-plans/specs/2026-04-30-admin-real-data-design.md`

---

## Scope

In scope:

- Replace `/admin` overview hardcoded metric, audit, attention, and environment rows with real view-model data.
- Replace `/admin/config` hardcoded rows with real redacted project/environment configuration rows.
- Replace shared topbar hardcoded admin/config/MCP state with an authenticated, runtime-derived shell view.
- Remove the prototype state sample drawer and the global fake `q` search.
- Add repository/service methods only where needed for bounded statistics.
- Add MockMvc/service tests for real data, empty states, permissions, redaction, and prototype text removal.
- Update `docs/PLANS.md` active plan index.

Out of scope:

- New SPA, Node/Vite build chain, JSON API, WebSocket, SSE, or polling.
- Configuration write/edit capability.
- Role schema changes.
- MCP protocol, SQL execution, policy, or audit write semantic changes.
- Target database business SQL probes during page render.

## Decisions

| Decision                                                           | Rationale                                                                                               | Rejected Alternative                                          |
|--------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| Add `AdminOverviewViewService` for `/admin` aggregation.           | Keeps controller thin and follows existing `AdminOperationsViewService` pattern.                        | Build all metrics in `AdminHomeController`.                   |
| Add `AdminShellViewService` for shared topbar state.               | Removes fixed `admin.refinex`, `Nacos prod`, and `MCP 正常` without duplicating model code across routes. | Keep private `addCommonModel` hardcoded fields.               |
| Extend `AdminOperationsViewService` for `/admin/config`.           | Configuration is an operations/read-only page and already belongs near policy/health view logic.        | Create a second config-only service with overlapping helpers. |
| Use existing server-side Thymeleaf rendering.                      | It preserves current management UI architecture and security model.                                     | Add frontend JSON fetch/render logic.                         |
| Use bounded counts and top-5 queries.                              | Overview must not load unbounded audit/token/grant data.                                                | Pull all metadata into memory and filter in Java.             |
| Remove global `q` search instead of wiring vague search semantics. | Existing audit page supports concrete fields, while `q` has no backend behavior.                        | Keep a fake topbar search form.                               |

## Files

Create:

- `src/main/java/com/refinex/dbflow/admin/AdminOverviewViewService.java` - `/admin` metric, recent audit, attention, and
  environment view model.
- `src/main/java/com/refinex/dbflow/admin/AdminShellViewService.java` - shared topbar/shell model from authentication,
  MCP readiness, and config source.
- `src/test/java/com/refinex/dbflow/admin/AdminOverviewRealDataControllerTests.java` - real overview, shell,
  empty-state, permission, and prototype-removal tests.
- `src/test/java/com/refinex/dbflow/admin/AdminConfigRealDataControllerTests.java` - real config rendering and redaction
  tests.

Modify:

- `src/main/java/com/refinex/dbflow/admin/AdminHomeController.java` - inject new services, remove hardcoded sample
  methods/records, wire `overview`, `config`, and shared shell model.
- `src/main/java/com/refinex/dbflow/admin/AdminOperationsViewService.java` - add config page view model and safe
  JDBC/config parsing.
- `src/main/java/com/refinex/dbflow/audit/repository/DbfAuditEventRepository.java` - add bounded recent audit query if
  needed; use existing `JpaSpecificationExecutor` for counts where practical.
- `src/main/java/com/refinex/dbflow/access/repository/DbfApiTokenRepository.java` - add status and expiry count methods.
- `src/main/java/com/refinex/dbflow/access/repository/DbfUserEnvGrantRepository.java` - add active grant count method.
- `src/main/resources/templates/admin/overview.html` - render `${overview}` and true empty states.
- `src/main/resources/templates/admin/config.html` - render `${configPage}` and true empty states.
- `src/main/resources/templates/admin/fragments/layout.html` - render `${shell}`, remove state sample drawer and fake
  global search.
- `src/test/java/com/refinex/dbflow/admin/AdminUiControllerTests.java` - adjust broad smoke assertions to the new
  shell/empty-state behavior if needed.
- `src/test/java/com/refinex/dbflow/admin/AdminOperationsPageControllerTests.java` - keep existing health/policy/audit
  expectations green if template shell changes affect response text.
- `docs/PLANS.md` - index this active plan.

No docs outside Harness plan/index are required unless implementation changes externally documented admin behavior.

## Verification Strategy

Run targeted tests while implementing:

```bash
./mvnw -Dtest=AdminOverviewRealDataControllerTests,AdminConfigRealDataControllerTests test
```

Expected: new tests pass and prove real data, empty states, redaction, permissions, and prototype text removal.

Run admin regression tests:

```bash
./mvnw -Dtest=AdminUiControllerTests,AdminOperationsPageControllerTests,AdminAccessManagementControllerTests test
```

Expected: existing admin smoke, operations, and access-management pages stay green.

Run full verification before completion:

```bash
./mvnw test
python3 scripts/check_harness.py
git diff --check
```

Expected: Maven suite passes, Harness validator passes, and no whitespace errors are reported.

## Risks

| Risk                                                              | Priority         | Mitigation                                                                                                                                     |
|-------------------------------------------------------------------|------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| Sensitive data leaks through config parsing or shell state.       | Security         | View records expose only redacted fields; tests assert no full JDBC URL, query string, `password`, Token hash, or password hash.               |
| Overview counts become slow on larger audit tables.               | Performance      | Use bounded 24-hour time window, top-5 recent query, and count/specification methods instead of unbounded in-memory filtering.                 |
| Authentication name is unavailable in non-standard contexts.      | Correctness      | `AdminShellViewService` falls back to `-` only when authentication is null or unauthenticated; normal MockMvc `user()` tests cover real names. |
| Prototype sample text appears as a legitimate configured project. | Test reliability | Tests use randomized project keys and assert fixed sample text is absent only in default/controlled contexts.                                  |
| Removing topbar search surprises users.                           | UX correctness   | Audit page still has concrete filters; this plan removes only the non-functional global `q` search.                                            |

## Task 1: Add Bounded Data Access For Overview Metrics

**Files:**

- Modify: `src/main/java/com/refinex/dbflow/audit/repository/DbfAuditEventRepository.java`
- Modify: `src/main/java/com/refinex/dbflow/access/repository/DbfApiTokenRepository.java`
- Modify: `src/main/java/com/refinex/dbflow/access/repository/DbfUserEnvGrantRepository.java`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminOverviewRealDataControllerTests.java`

**Decision Trace:** Spec sections “总览页数据口径” and “性能边界”.

- [x] **Step 1: Add bounded repository methods.**

Add only the query methods needed by `AdminOverviewViewService`:

- `DbfAuditEventRepository.findTop5ByOrderByCreatedAtDesc()`.
- Use existing `JpaSpecificationExecutor.count(Specification<DbfAuditEvent>)` for 24-hour audit counts, or add explicit
  count methods only when the Specification approach becomes too awkward.
- `DbfApiTokenRepository.countByStatus(String status)`.
- `DbfApiTokenRepository.countByStatusAndExpiresAtBetween(String status, Instant from, Instant to)`.
- `DbfUserEnvGrantRepository.countByStatus(String status)`.

Keep repository comments in Chinese and do not expose Token hash or password fields.

- [x] **Step 2: Add RED tests for real overview statistics.**

Create tests that write real audit events and active tokens through existing services/repositories, then assert `/admin`
displays those real counts and does not display hardcoded sample counts such as `128` from the prototype.

Run:

```bash
./mvnw -Dtest=AdminOverviewRealDataControllerTests test
```

Expected: tests fail before the service/template wiring exists, proving the current prototype behavior.

**Evidence:** Added bounded repository methods on audit, API Token, and grant repositories. Initial RED run
`./mvnw -Dtest=AdminOverviewRealDataControllerTests test` failed as expected because the old `/admin` response still
rendered prototype values including `admin.refinex`, `billing-core`, `状态样例`, and `128`.

## Task 2: Build Real Overview And Shell View Services

**Files:**

- Create: `src/main/java/com/refinex/dbflow/admin/AdminOverviewViewService.java`
- Create: `src/main/java/com/refinex/dbflow/admin/AdminShellViewService.java`
- Modify: `src/main/java/com/refinex/dbflow/admin/AdminHomeController.java`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminOverviewRealDataControllerTests.java`

**Decision Trace:** Spec sections “视图服务边界”, “总览页数据口径”, and “安全与权限”.

- [x] **Step 1: Implement `AdminOverviewViewService`.**

Inject `DbfAuditEventRepository`, `DbfConfirmationChallengeRepository`, `DbfApiTokenRepository`,
`DbfUserEnvGrantRepository`, `ProjectEnvironmentCatalogService`, `AuditQueryService` or sanitizer-backed mapping
helpers, and `DbflowHealthService`.

Produce records for:

- `OverviewPageView`
- `MetricCard`
- `RecentAuditRow`
- `AttentionItem`
- `EnvironmentOption`

Use a 24-hour window based on `Instant.now()` for time-bound metrics, top 5 recent audit rows, and at most 5 attention
items.

- [x] **Step 2: Implement `AdminShellViewService`.**

Inject `DbflowHealthService` and `org.springframework.core.env.Environment`.

Produce `ShellView` with:

- current admin name from `Authentication.getName()`
- MCP readiness label and tone from `DbflowHealthService.mcpEndpointReadiness()`
- config source label from Nacos Config/Discovery enablement and namespace, or `Local application config` when both are
  disabled

Never expose Nacos credentials or server-address secrets.

- [x] **Step 3: Wire controller model attributes.**

Update `AdminHomeController` so every admin route calls the shared shell service and adds `shell`, `activeNav`, and
`routeHint`. Change `/admin` to add `overview`, and `/admin/config` to add `configPage` from
`AdminOperationsViewService`.

Remove hardcoded sample helper methods and obsolete sample records from `AdminHomeController`.

- [x] **Step 4: Verify targeted service/controller behavior.**

Run:

```bash
./mvnw -Dtest=AdminOverviewRealDataControllerTests test
```

Expected: overview and shell tests pass, including current authenticated admin name and absence of `admin.refinex` in
controlled responses.

**Evidence:** Implemented `AdminOverviewViewService`, `AdminShellViewService`, and `AdminHomeController` wiring.
`./mvnw -Dtest=AdminOverviewRealDataControllerTests,AdminConfigRealDataControllerTests test` passed after wiring the
real overview rows, shell values, SQL hash column, empty states, and prototype-removal assertions.

## Task 3: Add Real Redacted Config Page View

**Files:**

- Modify: `src/main/java/com/refinex/dbflow/admin/AdminOperationsViewService.java`
- Modify: `src/main/java/com/refinex/dbflow/admin/AdminHomeController.java`
- Create: `src/test/java/com/refinex/dbflow/admin/AdminConfigRealDataControllerTests.java`

**Decision Trace:** Spec sections “配置页数据口径” and “脱敏规则”.

- [x] **Step 1: Add config page records and mapping.**

Extend `AdminOperationsViewService` with `configPage()` and records such as `ConfigPageView` and `ConfigRow`.

Read environments from `ProjectEnvironmentCatalogService.listConfiguredEnvironments()`. Map only:

- project key/name
- environment key/name
- datasource summary
- database type
- host
- port
- schema/database
- effective username
- connection pool limits
- metadata sync status

Do not expose complete JDBC URL, password, query string, or raw exception text containing the URL.

- [x] **Step 2: Parse JDBC values safely.**

Use structured parsing before fallback string handling. Strip query parameters before extracting host/port/schema. For
parse failures, return `-` or `解析失败` without echoing the raw URL.

Summarize Hikari defaults from `DbflowProperties.getDatasourceDefaults().getHikari()`.

- [x] **Step 3: Add config real-data and redaction tests.**

Use test properties with a unique configured `dbflow.projects` entry and a JDBC URL containing harmless query
parameters. Assert the rendered page includes safe host/schema/username values and excludes the complete JDBC URL,
`password`, and query string.

Run:

```bash
./mvnw -Dtest=AdminConfigRealDataControllerTests test
```

Expected: config tests pass and prove redaction boundaries.

**Evidence:** Extended `AdminOperationsViewService` with `configPage()` and redacted `ConfigRow` mapping from configured
environments. Config tests passed in
`./mvnw -Dtest=AdminOverviewRealDataControllerTests,AdminConfigRealDataControllerTests test`; assertions cover safe
host/schema/username rendering and exclusion of full JDBC URL, query string, password text, and fixed
`source=nacos:dbflow-targets-prod.yml`.

## Task 4: Update Thymeleaf Templates And Remove Prototype UI

**Files:**

- Modify: `src/main/resources/templates/admin/overview.html`
- Modify: `src/main/resources/templates/admin/config.html`
- Modify: `src/main/resources/templates/admin/fragments/layout.html`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminOverviewRealDataControllerTests.java`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminConfigRealDataControllerTests.java`

**Decision Trace:** Spec sections “模板改造” and “验收标准”.

- [x] **Step 1: Render overview from `${overview}`.**

Replace `${metrics}`, `${auditRows}`, and `${attentionItems}` with records from `${overview}`. Render true empty states
for no recent audit events and no attention items. Build environment options from `${overview.environmentOptions}` and
keep only the real “全部环境” default plus configured environments.

- [x] **Step 2: Render config from `${configPage}`.**

Replace `${configs}` and fixed `source=nacos:dbflow-targets-prod.yml` with `${configPage.rows}` and
`${configPage.sourceLabel}`. Add an empty state when no configured environments exist.

- [x] **Step 3: Render shared shell from `${shell}`.**

Remove the topbar global `q` search form, remove the `状态样例` button, and delete the `stateDrawer` markup. Render MCP
status, config source, and current administrator through `${shell}`.

- [x] **Step 4: Verify prototype text removal.**

Run:

```bash
./mvnw -Dtest=AdminOverviewRealDataControllerTests,AdminConfigRealDataControllerTests test
```

Expected: responses do not contain `状态样例`, `admin.refinex`, `billing-core`, `risk-lab`, or
`source=nacos:dbflow-targets-prod.yml` in controlled contexts where those values are not configured.

**Evidence:** Updated overview/config/layout templates to render `overview`, `configPage`, and `shell`; removed the fake
topbar `q` search, `状态样例` button, and state drawer. Residue scan over admin overview/config/layout and new tests
found prototype strings only in negative assertions. Targeted tests passed and validate absence of `状态样例`,
`admin.refinex`, `billing-core`, `risk-lab`, and `source=nacos:dbflow-targets-prod.yml` in controlled responses.

## Task 5: Preserve Existing Admin Behavior And Security Regression Coverage

**Files:**

- Modify: `src/test/java/com/refinex/dbflow/admin/AdminUiControllerTests.java`
- Modify: `src/test/java/com/refinex/dbflow/admin/AdminOperationsPageControllerTests.java`
- Modify: `src/test/java/com/refinex/dbflow/admin/AdminAccessManagementControllerTests.java` only if shell text changes
  break broad assertions.

**Decision Trace:** Spec sections “安全与权限” and “测试策略”.

- [x] **Step 1: Adjust existing smoke tests for new shell content.**

Keep assertions focused on stable page behavior: page title, route, admin-only access, and core table content. Remove
assertions that depend on prototype route hints or sample text if they conflict with real-data rendering.

- [x] **Step 2: Re-run admin regression tests.**

Run:

```bash
./mvnw -Dtest=AdminUiControllerTests,AdminOperationsPageControllerTests,AdminAccessManagementControllerTests test
```

Expected: existing admin pages still render, non-admin users remain forbidden, and access-management write flows still
pass.

**Evidence:** Existing admin smoke/regression tests did not require source edits after shell/template changes.
`./mvnw -Dtest=AdminUiControllerTests,AdminOperationsPageControllerTests,AdminAccessManagementControllerTests test`
passed with 16 tests, 0 failures, 0 errors, 0 skipped.

## Task 6: Final Verification And Plan Handoff

**Files:**

- Modify: `docs/exec-plans/active/2026-04-30-admin-real-data.md`
- Modify: `docs/PLANS.md`
- Modify: `docs/ARCHITECTURE.md` only if implementation materially changes the repository map or admin UI capability
  description.
- Modify: `docs/OBSERVABILITY.md` only if verification commands or observability behavior change.

**Decision Trace:** Spec sections “验收标准” and Harness lifecycle requirements.

- [x] **Step 1: Run full verification.**

Run:

```bash
./mvnw test
python3 scripts/check_harness.py
git diff --check
```

Expected: all commands pass.

- [x] **Step 2: Record evidence and prepare completion.**

Fill each task Evidence field with command outputs and key observations. Do not mark completion until `harness-verify`
has fresh verification evidence.

- [x] **Step 3: Keep docs index accurate.**

Ensure `docs/PLANS.md` still lists this file under Active Plans until implementation is verified and archived.

**Evidence:** `./mvnw test` passed with 140 tests, 0 failures, 0 errors, 10 skipped; skipped tests are
Docker/Testcontainers MySQL paths in this environment. `python3 scripts/check_harness.py` passed all 14 manifest
artifact checks. `git diff --check` produced no output. `docs/PLANS.md` still lists this plan under Active Plans pending
finish/archive.

## Dispatch Safety

Potentially independent work after Task 1:

- Task 3 config service/tests and Task 4 template edits can be split only if workers coordinate on the `configPage`
  record shape.
- Task 5 regression adjustment should run after Tasks 2-4 to avoid chasing intermediate failures.

Recommended default: execute inline with `harness-execute`, because `AdminHomeController`, shared layout, and tests
share write scope.
