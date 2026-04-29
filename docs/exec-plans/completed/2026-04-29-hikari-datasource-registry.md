# Execution Plan: Hikari DataSource Registry

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement a project/environment scoped Hikari `DataSource` registry backed only by `dbflow.projects[*].environments[*]`.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/config/DbflowProperties.java`
- `src/main/java/com/refinex/dbflow/executor/`
- `src/test/java/com/refinex/dbflow/executor/`
- `docs/ARCHITECTURE.md`
- `docs/OBSERVABILITY.md`

**Out of scope:**

- MCP SQL execution tools.
- SQL parsing, policy enforcement, confirmation flow, and audit write integration.
- Metadata database connection routing.
- Maven multi-module refactor.

## Constraints

- All Java comments and documentation comments use Chinese and follow the existing JavaDoc style.
- Target database pools must be isolated by `projectKey/environmentKey`.
- Registry must not fall back to the Spring Boot metadata `DataSource`.
- Database passwords must not appear in logs, exception messages, tests, or docs.
- Startup connection validation must be configurable.

## Acceptance Criteria

- [x] AC-1: Registry creates one independent `HikariDataSource` per configured project/environment.
- [x] AC-2: Registry applies shared `dbflow.datasource-defaults.hikari` pool settings to every target pool.
- [x] AC-3: Registry provides a service interface to resolve `DataSource` by `projectKey/environmentKey`.
- [x] AC-4: Missing project/environment lookup fails without default database fallback.
- [x] AC-5: Startup connection validation can be enabled or disabled by configuration.
- [x] AC-6: Registry shutdown closes all managed Hikari pools.
- [x] AC-7: Failure messages do not expose configured database passwords.
- [x] AC-8: `./mvnw test` and `python3 scripts/check_harness.py` pass.

## Risk Notes

| Risk                                                                | Likelihood | Mitigation                                                                                    |
|---------------------------------------------------------------------|------------|-----------------------------------------------------------------------------------------------|
| Hikari starts network connections during normal startup             | Medium     | Default startup validation to disabled and set initialization fail timeout accordingly.       |
| Sanitized error handling accidentally includes JDBC URL or password | Medium     | Use stable DBFlow error messages and avoid propagating driver messages into user-facing text. |
| Registry gets mixed with metadata `DataSource`                      | Low        | Build only from `DbflowProperties.projects` and never inject Spring Boot `DataSource`.        |

## Implementation Steps

### Step 1: Add configuration model

**Files:** `src/main/java/com/refinex/dbflow/config/DbflowProperties.java`,
`src/test/java/com/refinex/dbflow/config/DbflowPropertiesTests.java`
**Verification:** Configuration binding tests cover Hikari defaults and startup validation flag.

Status: ✅ Done
Evidence: `./mvnw -Dtest=DbflowPropertiesTests,HikariDataSourceRegistryTests test` passed with 14 tests after adding
Hikari binding and JDBC URL password rejection.
Deviations:

### Step 2: Add executor registry

**Files:** `src/main/java/com/refinex/dbflow/executor/ProjectEnvironmentDataSourceRegistry.java`,
`src/main/java/com/refinex/dbflow/executor/HikariDataSourceRegistry.java`,
`src/main/java/com/refinex/dbflow/executor/package-info.java`
**Verification:** Registry tests prove lookup, isolation, startup validation, sanitized failures, and shutdown behavior.

Status: ✅ Done
Evidence: `HikariDataSourceRegistryTests` covered isolated pools, lookup, missing target rejection, validation toggle,
sanitized failure, and shutdown.
Deviations:

### Step 3: Update docs and plan state

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`
**Verification:** Documentation includes real executor source paths and configuration/secret boundaries.

Status: ✅ Done
Evidence: `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, and `application.yml` now document executor paths, target
datasource boundaries, Hikari settings, and secret constraints.
Deviations:

### Step 4: Verify and archive

**Files:** `docs/exec-plans/active/2026-04-29-hikari-datasource-registry.md`,
`docs/exec-plans/completed/2026-04-29-hikari-datasource-registry.md`
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

Status: ✅ Done
Evidence: `./mvnw test` passed with 58 tests; `python3 scripts/check_harness.py` passed with 14 manifest artifacts;
`git diff --check` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                          | Notes |
|------|--------|-----------------------------------------------------------------------------------|-------|
| 1    | ✅      | Config binding tests passed.                                                      |       |
| 2    | ✅      | Registry lifecycle tests passed.                                                  |       |
| 3    | ✅      | Architecture and observability docs updated.                                      |       |
| 4    | ✅      | `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` passed. |       |

## Decision Log

| Decision                                                      | Context                                                           | Alternatives Considered | Rationale                                                                                                   |
|---------------------------------------------------------------|-------------------------------------------------------------------|-------------------------|-------------------------------------------------------------------------------------------------------------|
| Use H2-backed Hikari pools in tests instead of Testcontainers | Registry lifecycle can be proven without MySQL-specific behavior. | Testcontainers MySQL.   | Faster and still exercises real Hikari lifecycle; no SQL execution semantics are implemented in this phase. |

## Completion Summary

Completed: 2026-04-29
Duration: 4 steps
All acceptance criteria: PASS

Summary:

- Added `dbflow.datasource-defaults.hikari` and `validate-on-startup` binding.
- Implemented project/environment scoped `HikariDataSourceRegistry`.
- Rejected password-bearing JDBC URLs and sanitized registry failure messages.
- Added registry lifecycle tests and updated architecture/observability docs.
