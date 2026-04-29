# Execution Plan: Admin Session Security

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement Spring Security management-side session login with BCrypt-backed administrator password storage and tests.

## Scope

**In scope:**

- Spring Security dependencies and management-side `SecurityFilterChain` for form login, logout, and CSRF.
- Metadata-backed admin `UserDetailsService` using BCrypt password hashes from `dbf_users.password_hash`.
- Initial administrator account bootstrap from Spring external configuration, intended for environment variables or
  local profile use.
- Minimal admin endpoint surface for security tests.
- Security tests for unauthenticated admin redirect, successful login, failed login, and CSRF protection.
- Documentation updates for management session security and MCP Bearer Token separation.

**Out of scope:**

- MCP Bearer Token authentication implementation.
- Full management UI, user management screens, token issuing UI, and production authorization model beyond admin role.
- Real secret manager, Nacos secret loading, and password rotation workflows.

## Constraints

- Do not hard-code an administrator default password in source or documentation.
- MCP endpoint Bearer Token security must remain a separate future chain from management session security.
- Keep changes minimal and compatible with the current metadata schema.
- Preserve Chinese JavaDoc/comment standards.

## Acceptance Criteria

- [x] AC-1: `./mvnw test` passes.
- [x] AC-2: Management-side `/admin/**` requests require form login and support logout with CSRF enabled.
- [x] AC-3: Admin users authenticate through BCrypt hashes stored in `dbf_users.password_hash`.
- [x] AC-4: Initial administrator account can be created from external configuration without hard-coded default
  password.
- [x] AC-5: Security tests cover unauthenticated redirect, login success, login failure, and CSRF rejection.
- [x] AC-6: Documentation records the management-session/MCP-Bearer separation and secret boundary.

## Baseline Note

Baseline `./mvnw test` failed before Spring Security edits because Flyway migration V1 uses `create database/use dbf`,
which H2 MySQL mode rejects. This plan includes a minimal prerequisite migration cleanup so tests can prove the security
work: the migration should manage objects in the current connection schema only.

## Risk Notes

| Risk                                                            | Likelihood | Mitigation                                                                                             |
|-----------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------|
| Spring Security auto-configuration breaks existing slice tests. | Medium     | Use explicit security chain for admin/login/logout paths and keep non-admin paths permitted.           |
| Bootstrap creates weak or default credentials.                  | Medium     | Require explicit password or password hash configuration; no default password.                         |
| MCP and admin security concerns get mixed.                      | Medium     | Isolate this phase to admin form-login paths and document MCP Bearer Token as a separate future chain. |

## Implementation Steps

### Step 1: Restore test baseline prerequisite

**Files:** `src/main/resources/db/migration/V1__create_metadata_schema.sql`
**Verification:** `./mvnw test`

Status: ✅ Done
Evidence: Removed `create database/use dbf` from V1 migration so Flyway manages the current connection schema;
`./mvnw test` then passed.
Deviations:

### Step 2: Add security dependencies and admin properties

**Files:** `pom.xml`, `src/main/java/com/refinex/dbflow/security/*`
**Verification:** `./mvnw test`

Status: ✅ Done
Evidence: Added Spring Security runtime/test dependencies and `AdminSecurityProperties`.
Deviations:

### Step 3: Add admin session security chain and user model integration

**Files:** `src/main/java/com/refinex/dbflow/security/*`, `src/main/java/com/refinex/dbflow/access/*`
**Verification:** Security tests

Status: ✅ Done
Evidence: Added `AdminSecurityConfiguration`, `AdminUserDetailsService`, `InitialAdminUserInitializer`, and admin
password hash getters.
Deviations:

### Step 4: Add management security tests

**Files:** `src/test/java/com/refinex/dbflow/security/*`
**Verification:** `./mvnw test`

Status: ✅ Done
Evidence: Added `AdminSecurityTests` for redirect, login success, login failure, CSRF rejection, and BCrypt storage.
Deviations:

### Step 5: Update control-plane documentation

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`
**Verification:** `python3 scripts/check_harness.py`

Status: ✅ Done
Evidence: Updated architecture, observability, application configuration comments, and plan index.
Deviations:

### Step 6: Final verification

**Files:** Repository root
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, `git diff --check`

Status: ✅ Done
Evidence: `./mvnw test` exited 0 with Tests run: 28, Failures: 0, Errors: 0, Skipped: 0.
`python3 scripts/check_harness.py` exited 0 with 14 artifact(s) and all checks passed. `git diff --check` exited 0.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                             | Notes                  |
|------|--------|------------------------------------------------------------------------------------------------------|------------------------|
| 1    | ✅      | V1 migration now works in H2 MySQL mode without database switching.                                  | Baseline prerequisite. |
| 2    | ✅      | Security dependencies and initial admin properties added.                                            |                        |
| 3    | ✅      | Admin form-login chain, BCrypt encoder, metadata-backed user details service, and initializer added. |                        |
| 4    | ✅      | Security tests cover successful and failing paths.                                                   |                        |
| 5    | ✅      | Docs updated for session/Bearer separation and secret boundary.                                      |                        |
| 6    | ✅      | Maven tests, Harness validator, and diff whitespace check passed.                                    |                        |

## Decision Log

| Decision                                                                          | Context                                                                                                 | Alternatives Considered         | Rationale                                                                                                             |
|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|---------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Implement only an admin-path security chain now.                                  | MCP Bearer Token security is a separate requirement and should not inherit browser session assumptions. | One global chain for all paths. | Path-limited chain keeps admin session security independent and leaves MCP endpoints for a future Bearer-token chain. |
| Allow bootstrap from either plaintext external secret or precomputed BCrypt hash. | Local dev can provide an env password, while production may prefer secret-managed hashes.               | Only plaintext env password.    | Supporting both avoids committing defaults and keeps password handling flexible.                                      |

## Completion Summary

Completed: 2026-04-29
Duration: 6 steps
All acceptance criteria: PASS

Summary: Implemented management-side Spring Security form login, logout, CSRF, BCrypt admin password storage, and
external-configuration-based initial administrator bootstrap. MCP Bearer Token security remains a separate future chain.
