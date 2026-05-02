# Execution Plan: Admin Session API

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Add a JSON session endpoint for the React admin SPA at `GET /admin/api/session`, reusing the existing management shell
view service, returning only non-sensitive authenticated user and shell metadata, and making anonymous API access
machine-readable as `401` JSON instead of an HTML login page.

## Scope

**In scope:**

- Add `AdminSessionApiController` for `GET /admin/api/session`.
- Add a narrow DTO for session and shell response fields.
- Reuse `AdminShellViewService` for admin name, MCP status, tone, and config source label.
- Configure the management security chain so unauthenticated `/admin/api/**` requests receive JSON `401`.
- Add `AdminSessionApiControllerTests` for authenticated and anonymous access.
- Update Harness documentation and verification evidence.

**Out of scope:**

- React client implementation.
- Changing Thymeleaf `/admin/**` page behavior.
- Changing the `/mcp` bearer-token security chain.
- Returning password hashes, API tokens, datasource secrets, or raw configuration values.

## Constraints

- `/admin/api/**` remains administrator-only.
- Anonymous React clients must be able to identify unauthenticated state from JSON.
- Existing `/admin` anonymous requests must continue redirecting to `/login`.
- `AdminShellViewService` remains the single source for shell metadata.

## Acceptance Criteria

- [x] AC-1: Authenticated `GET /admin/api/session` returns `authenticated=true`, username/display name, roles, and shell
  metadata.
- [x] AC-2: Anonymous `GET /admin/api/session` with JSON accept returns HTTP `401` JSON.
- [x] AC-3: Response DTO does not expose password hash, token, or sensitive configuration fields.
- [x] AC-4: `/admin` Thymeleaf security behavior is unchanged.
- [x] AC-5: `./mvnw -Dtest=AdminSessionApiControllerTests test` passes.

## Implementation Steps

### Step 1: Add red session API tests

**Files:** `src/test/java/com/refinex/dbflow/admin/AdminSessionApiControllerTests.java`
**Verification:** targeted test fails before implementation because the endpoint and JSON entry point are absent.

Status: Completed
Evidence: `./mvnw -Dtest=AdminSessionApiControllerTests test` failed before implementation because authenticated
`GET /admin/api/session` returned `404` and anonymous JSON access returned a form-login `302`.

### Step 2: Implement controller, DTO, and API 401 JSON

**Files:**

- `src/main/java/com/refinex/dbflow/admin/controller/AdminSessionApiController.java`
- `src/main/java/com/refinex/dbflow/admin/dto/AdminSessionResponse.java`
- `src/main/java/com/refinex/dbflow/security/configuration/AdminSecurityConfiguration.java`

**Verification:** targeted tests pass.

Status: Completed
Evidence: Added `AdminSessionApiController`, `AdminSessionResponse`, and a split authentication entry point for JSON
API `401` versus page login redirects. `./mvnw -Dtest=AdminSessionApiControllerTests test` passed with 2 tests,
0 failures, 0 errors, 0 skipped.

### Step 3: Verify, document, and archive

**Files:** docs and repository state
**Verification:** targeted Maven test, relevant security regression, Harness validator, diff hygiene.

Status: Completed
Evidence:
`./mvnw -Dtest=AdminSessionApiControllerTests,AdminSpaControllerTests,AdminSecurityTests,AdminCsrfSpaTests,McpSecurityTests test`
passed with 22 tests, 0 failures, 0 errors, 0 skipped. `./mvnw test` passed with 202 tests, 0 failures, 0 errors,
10 skipped. `python3 scripts/check_harness.py` passed. `git diff --check` passed.

## Progress Log

| Step      | Status | Evidence                                                                        | Notes                                                            |
|-----------|--------|---------------------------------------------------------------------------------|------------------------------------------------------------------|
| Preflight | Done   | `python3 scripts/check_harness.py`; `./mvnw test`                               | Harness passed; baseline Maven test passed 200 tests, 10 skipped |
| 1         | Done   | Red targeted test failed with `404` authenticated and `302` anonymous           | Missing controller and JSON API entry point confirmed            |
| 2         | Done   | `./mvnw -Dtest=AdminSessionApiControllerTests test`                             | 2 tests, 0 failures, 0 errors, 0 skipped                         |
| 3         | Done   | Targeted security regression, full Maven suite, Harness validator, diff hygiene | 22 targeted tests passed; full suite passed 202 tests            |

## Decision Log

| Decision                      | Context                                             | Alternatives Considered                 | Rationale                                                        |
|-------------------------------|-----------------------------------------------------|-----------------------------------------|------------------------------------------------------------------|
| Reuse shell view service      | SPA needs same admin shell metadata as server UI    | Duplicate health/config lookup in API   | Keeps MCP status and safe config label behavior in one service   |
| Return JSON 401 for API paths | React client must distinguish unauthenticated state | Preserve form-login redirect everywhere | Keeps page redirects for `/admin` while making APIs machine-safe |
| Keep display name as username | Authentication principal currently carries username | Query admin user entity in session API  | Avoids accidental exposure of persistence fields in this pass    |
