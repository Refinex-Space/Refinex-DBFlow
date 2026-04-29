# Execution Plan: DataSource Config Reloader

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement a candidate-first target datasource reload flow that validates and warms new pools before atomically replacing
the active registry.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/executor/`
- `src/test/java/com/refinex/dbflow/executor/`
- `docs/ARCHITECTURE.md`
- `docs/OBSERVABILITY.md`
- `docs/PLANS.md`

**Out of scope:**

- Nacos refresh listener integration.
- SQL execution, policy enforcement, confirmation, and audit DB writes.
- Per-environment partial reload semantics.
- Maven dependency changes.

## Constraints

- A bad candidate configuration must not make existing target environments unavailable.
- Candidate pools must be built and connection-warmed before active registry replacement.
- Old pools are closed only after a successful atomic swap.
- Replacement failures must preserve old pools and emit operational warning logs.
- Logs and exceptions must not expose database passwords or JDBC URLs containing secrets.

## Acceptance Criteria

- [x] AC-1: `DataSourceConfigReloader` validates candidate `DbflowProperties` before replacement.
- [x] AC-2: Candidate Hikari pools are preheated before swap.
- [x] AC-3: Successful reload replaces active registry and closes old pools after swap.
- [x] AC-4: Failed reload keeps old pools available and closes any candidate pools.
- [x] AC-5: Reload path emits operational info/warn logs with sanitized target counts/reasons.
- [x] AC-6: Tests cover successful replacement, failed preservation, and old pool close timing.
- [x] AC-7: `docs/ARCHITECTURE.md` documents the configuration refresh flow.
- [x] AC-8: `./mvnw test` and `python3 scripts/check_harness.py` pass.

## Risk Notes

| Risk                                             | Likelihood | Mitigation                                                                |
|--------------------------------------------------|------------|---------------------------------------------------------------------------|
| Closing old pools before swap causes outage      | Medium     | Build candidate map first, swap reference second, close old map last.     |
| Candidate creation partially succeeds then fails | Medium     | Close the partially built candidate map before returning failure.         |
| Reload API leaks driver error details            | Low        | Return/log sanitized messages and avoid propagating JDBC URL or password. |

## Implementation Steps

### Step 1: Add reload behavior tests

**Files:** `src/test/java/com/refinex/dbflow/executor/DataSourceConfigReloaderTests.java`
**Verification:** Targeted test fails before reloader and registry replacement APIs exist.

Status: ✅ Completed
Evidence:

- RED: `./mvnw -Dtest=DataSourceConfigReloaderTests test` failed because `DataSourceConfigReloader` and
  `DataSourceReloadResult` did not exist.
  Deviations:

### Step 2: Refactor registry for candidate replacement

**Files:** `src/main/java/com/refinex/dbflow/executor/HikariDataSourceRegistry.java`,
`src/main/java/com/refinex/dbflow/executor/DataSourceReloadResult.java`
**Verification:** Existing registry tests and new reload tests pass.

Status: ✅ Completed
Evidence:

- Implemented atomic registry snapshot replacement with candidate pool construction and forced connection warmup.
- `./mvnw -Dtest=DataSourceConfigReloaderTests,HikariDataSourceRegistryTests test` passed: 8 tests, 0 failures.
  Deviations:

### Step 3: Add reloader service

**Files:** `src/main/java/com/refinex/dbflow/executor/DataSourceConfigReloader.java`,
`src/main/java/com/refinex/dbflow/executor/package-info.java`
**Verification:** New reload tests cover success, failure, old-pool preservation, and old-pool closure.

Status: ✅ Completed
Evidence:

- Added `DataSourceConfigReloader` with candidate validation and sanitized operational info/warn logs.
- Added `DataSourceReloadResult` for explicit success/failure reporting.
- `./mvnw -Dtest=DataSourceConfigReloaderTests,HikariDataSourceRegistryTests test` passed: 8 tests, 0 failures.
  Deviations:

### Step 4: Update docs and archive

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, `docs/exec-plans/`
**Verification:** Architecture includes candidate validation and atomic swap flow; final verification commands pass.

Status: ✅ Completed
Evidence:

- Updated `docs/ARCHITECTURE.md` with implemented datasource refresh flow and current source paths.
- Updated `docs/OBSERVABILITY.md` with runtime reload boundary and test coverage.
  Deviations:

## Progress Log

| Step | Status | Evidence                                                 | Notes |
|------|--------|----------------------------------------------------------|-------|
| 1    | ✅      | RED compile failure for missing reloader/result classes. |       |
| 2    | ✅      | Targeted executor tests passed.                          |       |
| 3    | ✅      | Targeted executor tests passed.                          |       |
| 4    | ✅      | Architecture and observability docs updated.             |       |

## Decision Log

| Decision                           | Context                                                  | Alternatives Considered              | Rationale                                                                      |
|------------------------------------|----------------------------------------------------------|--------------------------------------|--------------------------------------------------------------------------------|
| Full registry snapshot replacement | Target config may change multiple environments together. | Partial per-environment replacement. | Atomic full snapshot is simpler and avoids mixed old/new config in this phase. |

## Completion Summary

Implemented candidate-first datasource reload through `DataSourceConfigReloader` and atomic registry snapshot
replacement
inside `HikariDataSourceRegistry`. Candidate properties are validated before touching the active registry, candidate
Hikari pools are preheated before swap, failed candidates preserve existing pools, and successful swaps close old pools
after replacement. Architecture and observability docs now record the implemented refresh boundary.

Final verification:

- `./mvnw test` passed: 64 tests, 0 failures, 0 errors.
- `python3 scripts/check_harness.py` passed: 14 manifest artifacts, all checks passed.
- `git diff --check` passed.
