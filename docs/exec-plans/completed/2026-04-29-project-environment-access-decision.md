# Execution Plan: Project Environment Access Decision

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement project/environment grant management and a service-level access decision boundary that future MCP SQL
execution must call before touching any target database environment.

## Scope

**In scope:**

- Grant create, delete, and query by `projectKey` and `environmentKey`.
- `AccessDecisionService` with allow/deny output and stable denial reasons.
- User, token, project, environment, token state, and grant checks.
- YAML `dbflow.projects[].environments[]` catalog synchronization into metadata display rows.
- Sanitized configured environment view for admin display without database password exposure.
- JPA slice tests for unauthorized, authorized, missing environment, disabled user, disabled/revoked token, and
  sanitized catalog display.
- Architecture and observability documentation updates.

**Out of scope:**

- MCP endpoint authentication filter.
- MCP SQL tool implementation.
- SQL policy parsing or execution.
- Admin UI pages.
- Database credential encryption or Nacos integration.

## Constraints

- Future MCP SQL execution must call `AccessDecisionService` before any target database access.
- Admin-facing configured environment display must not expose database passwords.
- No plaintext MCP Token handling is added in this phase.
- Keep the implementation inside the current single-module Spring Boot application.

## Assumptions

- The access decision input is metadata-level identity: `userId`, `tokenId`, `projectKey`, and `environmentKey`.
- Grant deletion is a physical metadata delete because the current V1 schema has no grant revocation timestamp.
- YAML catalog synchronization creates missing project/environment metadata rows but does not update existing names yet.

## Acceptance Criteria

- [x] AC-1: `./mvnw test` passes.
- [x] AC-2: Grant create, delete, and query by `projectKey/environmentKey` are covered by tests.
- [x] AC-3: `AccessDecisionService` allows authorized active user/token/grant combinations.
- [x] AC-4: `AccessDecisionService` denies missing grant, missing environment, disabled user, and revoked/disabled token
  with explicit reasons.
- [x] AC-5: Configured project/environment catalog can be synchronized to metadata and exposed through a sanitized view
  that does not include passwords.
- [x] AC-6: Architecture and observability docs record the real package paths and the SQL pre-authorization boundary.

## Implementation Steps

### Step 1: Extend entity/repository/service access primitives

**Files:** `src/main/java/com/refinex/dbflow/access/entity/*`, `src/main/java/com/refinex/dbflow/access/repository/*`,
`src/main/java/com/refinex/dbflow/access/service/AccessService.java`
**Verification:** Access service tests

Status: ✅ Completed
Evidence: `AccessDecisionServiceJpaTests.shouldCreateQueryAndDeleteGrantByProjectEnvironmentKey` covers key-based
grant create/query/delete; targeted Maven tests passed.
Deviations:

### Step 2: Add access decision and catalog services

**Files:** `src/main/java/com/refinex/dbflow/access/service/*`
**Verification:** Access decision tests

Status: ✅ Completed
Evidence: `AccessDecisionService` and `ProjectEnvironmentCatalogService` added under
`com.refinex.dbflow.access.service`.
Deviations:

### Step 3: Add JPA slice tests

**Files:** `src/test/java/com/refinex/dbflow/access/*`
**Verification:** Targeted Maven tests

Status: ✅ Completed
Evidence: `./mvnw test -Dtest=AccessDecisionServiceJpaTests,AccessServiceJpaTests` exited 0 with 12 tests passing.
Deviations:

### Step 4: Update control-plane documentation

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`
**Verification:** Harness validator

Status: ✅ Completed
Evidence: `docs/ARCHITECTURE.md` and `docs/OBSERVABILITY.md` record access-decision package paths, password-free
configured environment display, and the future MCP SQL pre-authorization boundary.
Deviations:

### Step 5: Final verification

**Files:** Repository root
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, `git diff --check`

Status: ✅ Completed
Evidence: `./mvnw test` exited 0 with 42 tests passing. Final Harness and diff checks are recorded in the handoff.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                   | Notes                                                          |
|------|--------|--------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| 1    | ✅      | Targeted access tests passing                                                              | Entity/repository/service primitives support key-based grants. |
| 2    | ✅      | `AccessDecisionServiceJpaTests` passing                                                    | Access decision and catalog synchronization behavior covered.  |
| 3    | ✅      | `./mvnw test -Dtest=AccessDecisionServiceJpaTests,AccessServiceJpaTests`: 12 tests passing | Main allow/deny and grant paths covered.                       |
| 4    | ✅      | Architecture and observability docs updated                                                | Real source paths and security boundary documented.            |
| 5    | ✅      | `./mvnw test`: 42 tests passing                                                            | Full suite regression check completed.                         |

## Decision Log

| Decision                                                 | Context                                                                                     | Alternatives Considered          | Rationale                                                                                                                  |
|----------------------------------------------------------|---------------------------------------------------------------------------------------------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| Use a service-level access decision instead of a filter. | Current phase has no MCP endpoint yet.                                                      | Implement MCP Bearer filter now. | Keeps SQL pre-authorization testable without prematurely coupling to transport.                                            |
| Physically delete grants.                                | V1 grant schema has status but no revoked timestamp.                                        | Soft-delete with status only.    | Physical deletion matches the requested delete behavior and allows clean re-granting under the existing unique constraint. |
| Synchronize missing YAML catalog entries into metadata.  | Grants and decisions need stable metadata ids while admin display comes from configuration. | Use YAML only or metadata only.  | Synchronization bridges configured environments and grant metadata without exposing passwords.                             |

## Completion Summary

Implemented project/environment access control baseline:

- Added key-based grant create/query/delete in `AccessService`.
- Added `AccessDecisionService` with explicit allow/deny reasons for user, token, project, environment, and grant
  checks.
- Added `ProjectEnvironmentCatalogService` to synchronize YAML project environments into metadata display rows and
  return password-free configured environment views.
- Added JPA tests for grant CRUD, allow decisions, denial reasons, disabled user, disabled/revoked token, token
  mismatch, expired token, missing environment, and password-free display.
- Updated control-plane documentation with real source paths and the future MCP SQL pre-authorization boundary.
