# Execution Plan: Admin User JSON API

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Expose the existing admin user-management capabilities as administrator-only JSON APIs for the React admin shell.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/admin/controller/`
- `src/main/java/com/refinex/dbflow/admin/dto/`
- `src/test/java/com/refinex/dbflow/admin/`
- `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`

**Out of scope:**

- Thymeleaf `/admin/users` behavior changes.
- User-management service semantics or database schema changes.
- React page implementation.

## Constraints

- Reuse `AdminAccessManagementService` for list, create, disable, enable, and reset-password behavior.
- Return JSON through `ApiResult` or an appropriate HTTP 4xx JSON response.
- Do not return `passwordHash`, reset-password plaintext, Token plaintext, or other sensitive values.
- Keep `/admin/api/**` administrator-only and keep CSRF required for all mutation requests.
- Follow DBFlow Java comment standards with Chinese class, field, and parameter comments.

## Acceptance Criteria

- [x] AC-1: `GET /admin/api/users?username=&status=` returns admin-only `ApiResult.ok` user rows and never includes
  `passwordHash`.
- [x] AC-2: `POST /admin/api/users` creates a user from JSON, requires CSRF, returns no password material, and converts
  invalid input to JSON 4xx.
- [x] AC-3: Disable and enable JSON endpoints call the existing service methods and update the visible user status.
- [x] AC-4: Reset-password JSON endpoint calls the existing service method, requires CSRF, and never returns the new
  password.
- [x] AC-5: Anonymous and non-admin callers cannot access the user JSON API.
- [x] AC-6: `./mvnw -Dtest=AdminUserApiControllerTests test` passes.

## Risk Notes

| Risk                                            | Likelihood | Mitigation                                                                                     |
|-------------------------------------------------|------------|------------------------------------------------------------------------------------------------|
| API error responses leak service exception text | Low        | Return stable `ApiResult.failed(ErrorCode.INVALID_REQUEST)` for validation/business failures.  |
| Tests bypass real CSRF behavior                 | Medium     | Include explicit missing-CSRF rejection plus successful POSTs with Spring Security CSRF token. |
| New API diverges from Thymeleaf user behavior   | Low        | Reuse `AdminAccessManagementService` and leave `AdminHomeController` untouched.                |

## Implementation Steps

### Step 1: Add failing controller tests

**Files:** `src/test/java/com/refinex/dbflow/admin/AdminUserApiControllerTests.java`
**Verification:** `./mvnw -Dtest=AdminUserApiControllerTests test` fails because the JSON API is not implemented.

Status: ✅ Done
Evidence: RED verified with `./mvnw -Dtest=AdminUserApiControllerTests test`; 7 tests ran, 5 failed because
`/admin/api/users...` returned 404 before the controller existed.
Deviations:

### Step 2: Implement user JSON API

**Files:** `src/main/java/com/refinex/dbflow/admin/controller/AdminUserApiController.java`,
`src/main/java/com/refinex/dbflow/admin/dto/AdminCreateUserRequest.java`,
`src/main/java/com/refinex/dbflow/admin/dto/AdminResetPasswordRequest.java`
**Verification:** `./mvnw -Dtest=AdminUserApiControllerTests test`

Status: ✅ Done
Evidence: GREEN verified with `./mvnw -Dtest=AdminUserApiControllerTests test`; 7 tests ran, 0 failures, 0 errors.
Deviations:

### Step 3: Update repository control-plane documentation

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `python3 scripts/check_harness.py`

Status: ✅ Done
Evidence: `python3 scripts/check_harness.py` passed after documentation updates; architecture and observability docs now
list the user JSON API and test coverage.
Deviations:

### Step 4: Run final verification and archive

**Files:** `docs/exec-plans/active/2026-05-02-admin-user-json-api.md`,
`docs/exec-plans/completed/2026-05-02-admin-user-json-api.md`
**Verification:** `./mvnw -Dtest=AdminUserApiControllerTests test`, related admin/security tests, `./mvnw test`,
`git diff --check`

Status: ✅ Done
Evidence: Final gates passed: targeted test 7/0/0, related regression 26/0/0, full suite 218 tests with 0 failures and
10 skipped, Harness validator passed, diff whitespace check passed, and incremental wildcard import check returned no
matches.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                                          | Notes                                   |
|------|--------|-------------------------------------------------------------------------------------------------------------------|-----------------------------------------|
| 1    | ✅      | RED: `./mvnw -Dtest=AdminUserApiControllerTests test` failed with 404 for missing `/admin/api/users...` mappings. | Expected TDD failure.                   |
| 2    | ✅      | GREEN: `./mvnw -Dtest=AdminUserApiControllerTests test` passed 7 tests, 0 failures, 0 errors.                     | Controller and request DTOs added.      |
| 3    | ✅      | `python3 scripts/check_harness.py` passed after docs update.                                                      | Architecture and observability updated. |
| 4    | ✅      | Final gates passed: full suite 218 tests, 0 failures, 10 skipped; `git diff --check` passed.                      | Archived.                               |

## Decision Log

| Decision                                          | Context                                                                   | Alternatives Considered                       | Rationale                                                     |
|---------------------------------------------------|---------------------------------------------------------------------------|-----------------------------------------------|---------------------------------------------------------------|
| Use existing `UserRow` as API list/create payload | It already omits password material and is used by the current admin page. | Create new response DTOs for every operation. | Keeps the API small and avoids duplicating a safe projection. |

## Completion Summary

Completed: 2026-05-02
Duration: 4 steps
All acceptance criteria: PASS

Summary:

- Added administrator-only JSON user-management endpoints for list/filter/create/disable/enable/reset-password.
- Reused `AdminAccessManagementService` and the safe `UserRow` projection.
- Kept CSRF required for mutations and preserved existing Thymeleaf `/admin/users` behavior.
- Verified targeted, related, full-suite, Harness, whitespace, and import gates.
