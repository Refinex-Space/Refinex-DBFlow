# Execution Plan: Admin Grant JSON API

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Expose the existing admin project/environment grant capabilities as administrator-only JSON APIs for the React admin
shell.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/admin/controller/`
- `src/main/java/com/refinex/dbflow/admin/dto/`
- `src/test/java/com/refinex/dbflow/admin/`
- `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`

**Out of scope:**

- Thymeleaf `/admin/grants` behavior changes.
- Access-management service semantics or database schema changes.
- React page implementation.

## Constraints

- Reuse `AdminAccessManagementService` for grant list, options, create, project update, and revoke behavior.
- Return JSON through `ApiResult` or an appropriate HTTP 4xx JSON response.
- Do not return database connection information, password material, Token plaintext, or hash values.
- Keep `/admin/api/**` administrator-only and keep CSRF required for all mutation requests.
- Preserve `update-project` empty `environmentKeys` semantics as “revoke all environments for that user/project”.
- Follow DBFlow Java comment standards with Chinese class, field, and parameter comments.

## Acceptance Criteria

- [x] AC-1: `GET /admin/api/grants/options` returns active users and configured environment options without connection
  data.
- [x] AC-2: `GET /admin/api/grants?username=&projectKey=&environmentKey=&status=` returns grouped grants and supports
  filters.
- [x] AC-3: `POST /admin/api/grants` creates a grant from JSON, requires CSRF, and returns no sensitive data.
- [x] AC-4: `POST /admin/api/grants/update-project` updates project environment grants and accepts empty
  `environmentKeys`.
- [x] AC-5: `POST /admin/api/grants/{grantId}/revoke` revokes a grant and requires CSRF.
- [x] AC-6: `./mvnw -Dtest=AdminGrantApiControllerTests test` passes.

## Risk Notes

| Risk                                            | Likelihood | Mitigation                                                                                   |
|-------------------------------------------------|------------|----------------------------------------------------------------------------------------------|
| Grant options accidentally expose JDBC settings | Low        | Reuse `GrantEnvironmentOption`, which carries only project/environment labels and keys.      |
| Empty project update is rejected or ignored     | Medium     | Add a dedicated API regression test for empty `environmentKeys` revoking all project grants. |
| API diverges from Thymeleaf grant behavior      | Low        | Reuse `AdminAccessManagementService` and leave `AdminHomeController` untouched.              |

## Implementation Steps

### Step 1: Add failing controller tests

**Files:** `src/test/java/com/refinex/dbflow/admin/AdminGrantApiControllerTests.java`
**Verification:** `./mvnw -Dtest=AdminGrantApiControllerTests test`

Status: ✅ Done
Evidence: RED verified with `./mvnw -Dtest=AdminGrantApiControllerTests test`; 6 tests ran, 5 failed because
`/admin/api/grants...` returned 404 before the controller existed, while the missing-CSRF security check was already
enforced.
Deviations:

### Step 2: Implement grant JSON API

**Files:** `src/main/java/com/refinex/dbflow/admin/controller/AdminGrantApiController.java`,
`src/main/java/com/refinex/dbflow/admin/dto/GrantEnvironmentRequest.java`,
`src/main/java/com/refinex/dbflow/admin/dto/UpdateProjectGrantsRequest.java`,
`src/main/java/com/refinex/dbflow/admin/dto/GrantOptionsResponse.java`
**Verification:** `./mvnw -Dtest=AdminGrantApiControllerTests test`

Status: ✅ Done
Evidence: GREEN verified with `./mvnw -Dtest=AdminGrantApiControllerTests test`; 6 tests ran, 0 failures, 0 errors.
Deviations:

### Step 3: Update repository control-plane documentation

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `python3 scripts/check_harness.py`

Status: ✅ Done
Evidence: `python3 scripts/check_harness.py` passed after architecture, observability, and plan index updates.
Deviations:

### Step 4: Run final verification and archive

**Files:** `docs/exec-plans/active/2026-05-02-admin-grant-json-api.md`,
`docs/exec-plans/completed/2026-05-02-admin-grant-json-api.md`
**Verification:** `./mvnw -Dtest=AdminGrantApiControllerTests test`, related admin tests, `./mvnw test`,
`git diff --check`

Status: ✅ Done
Evidence: Final gates passed: targeted test 6/0/0, related admin regression 18/0/0, full suite 224 tests with
0 failures and 10 skipped, Harness validator passed, diff whitespace check passed, touched-file trailing whitespace
check returned no output, and incremental wildcard import check returned no matches.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                                                   | Notes                                   |
|------|--------|----------------------------------------------------------------------------------------------------------------------------|-----------------------------------------|
| 1    | ✅      | RED: `./mvnw -Dtest=AdminGrantApiControllerTests test` failed with 404 for missing grant API mappings.                     | Expected TDD failure.                   |
| 2    | ✅      | GREEN: `./mvnw -Dtest=AdminGrantApiControllerTests test` passed 6 tests, 0 failures, 0 errors.                             | Controller and request DTOs added.      |
| 3    | ✅      | `python3 scripts/check_harness.py` passed after docs update.                                                               | Architecture and observability updated. |
| 4    | ✅      | Final gates passed: targeted 6/0/0, related 18/0/0, full suite 224/0/0 with 10 skipped; Harness/diff/import checks passed. | Archived.                               |

## Decision Log

| Decision                                        | Context                                                                  | Alternatives Considered                                   | Rationale                                                                              |
|-------------------------------------------------|--------------------------------------------------------------------------|-----------------------------------------------------------|----------------------------------------------------------------------------------------|
| Use existing grant view records as API payloads | Grant rows and options already avoid connection/password/token material. | Create separate response DTOs for every grant list shape. | Keeps the API aligned with the Thymeleaf projection and avoids duplicating safe views. |

## Completion Summary

Completed: 2026-05-02
Duration: 4 steps
All acceptance criteria: PASS

Summary:

- Added administrator-only JSON project/environment grant endpoints for options, list/filter, create, project-level
  update, and revoke.
- Reused `AdminAccessManagementService` and existing safe grant/user/environment projections.
- Preserved Thymeleaf `/admin/grants` behavior and kept CSRF required for JSON mutations.
- Verified targeted, related, full-suite, Harness, whitespace, and import gates.
