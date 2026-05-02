# Execution Plan: Admin JSON Login

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Allow the React admin SPA to log in and log out through the existing `/login` and `/logout` endpoints using JSON
responses, while preserving the existing Thymeleaf form-login redirects and keeping CSRF protection enabled.

## Scope

**In scope:**

- Add JSON-aware authentication success and failure handling for `POST /login`.
- Add JSON-aware logout success handling for `POST /logout`.
- Return `ApiResult.ok(sessionInfo)` for JSON login success.
- Return HTTP `401` JSON for JSON login failure.
- Return `ApiResult.ok` for JSON logout success and invalidate the session through Spring Security's normal logout
  flow.
- Add `AdminJsonLoginTests` and keep `AdminSecurityTests` passing.
- Update Harness documentation and verification evidence.

**Out of scope:**

- Adding a separate login API path.
- Disabling or weakening CSRF on `/login` or `/logout`.
- Changing the Thymeleaf login page or existing form parameter names.
- Implementing React client code.
- Changing the `/mcp` bearer-token security chain.

## Constraints

- JSON requests are detected by `Accept: application/json` or `X-Requested-With: XMLHttpRequest`.
- Non-JSON login success continues to redirect to `/admin`.
- Non-JSON login failure continues to redirect to `/login?error`.
- Non-JSON logout continues to redirect to `/login?logout`.
- Session response data must use the same safe session projection as `GET /admin/api/session`.

## Assumptions

- The React SPA can read the existing `XSRF-TOKEN` cookie and send it back through `X-XSRF-TOKEN`.
- The existing `username` and `password` form parameters remain the login credential contract.
- JSON logout can return a small success payload without repeating full session data because the session is invalidated.

## Acceptance Criteria

- [x] AC-1: JSON `POST /login` with valid credentials and CSRF returns HTTP `200` JSON `ApiResult.ok(sessionInfo)`.
- [x] AC-2: JSON `POST /login` with invalid credentials and CSRF returns HTTP `401` JSON.
- [x] AC-3: Existing ordinary form-login success and failure redirects remain unchanged.
- [x] AC-4: JSON `POST /logout` with CSRF returns HTTP `200` JSON and invalidates the HTTP session.
- [x] AC-5: Login and logout continue to require CSRF.
- [x] AC-6: `./mvnw -Dtest=AdminJsonLoginTests,AdminSecurityTests test` passes.

## Implementation Steps

### Step 1: Add red JSON login/logout tests

**Files:** `src/test/java/com/refinex/dbflow/security/AdminJsonLoginTests.java`
**Verification:** targeted test fails before implementation because JSON login/logout still use redirect behavior.

Status: Done
Evidence: `./mvnw -Dtest=AdminJsonLoginTests,AdminSecurityTests test` failed before implementation with JSON login
redirecting to `/admin` and JSON failure redirecting to `/login?error`, while missing-CSRF login remained forbidden.

### Step 2: Implement JSON-aware handlers

**Files:**

- `src/main/java/com/refinex/dbflow/security/configuration/AdminSecurityConfiguration.java`
- Optional handler/support classes under `src/main/java/com/refinex/dbflow/security/...`
- Optional shared session projection service under `src/main/java/com/refinex/dbflow/admin/service/...`

**Verification:** targeted tests pass.

Status: Done
Evidence: Added JSON-aware login success/failure and logout success handlers in `AdminSecurityConfiguration`; extracted
safe session projection into `AdminSessionViewService`; `./mvnw -Dtest=AdminJsonLoginTests,AdminSecurityTests test`
passed with 9 tests.

### Step 3: Verify, document, and archive

**Files:** docs and repository state
**Verification:** targeted Maven test, relevant security regression, full Maven suite, Harness validator, diff hygiene.

Status: Done
Evidence: `./mvnw -Dtest=AdminJsonLoginTests,AdminSecurityTests,AdminSessionApiControllerTests,AdminCsrfSpaTests test`
passed with 15 tests. `./mvnw test` passed with 206 tests, 0 failures, 0 errors, and 10 Docker-optional skips.

## Progress Log

| Step      | Status | Evidence                                                    | Notes                                                            |
|-----------|--------|-------------------------------------------------------------|------------------------------------------------------------------|
| Preflight | Done   | `python3 scripts/check_harness.py`; `./mvnw test`           | Harness passed; baseline Maven test passed 202 tests, 10 skipped |
| 1         | Done   | Red run: JSON login/logout expected JSON but got redirects  | Missing-CSRF JSON login stayed forbidden                         |
| 2         | Done   | `./mvnw -Dtest=AdminJsonLoginTests,AdminSecurityTests test` | 9 tests passed after JSON-aware handlers                         |
| 3         | Done   | `./mvnw test`                                               | 206 tests passed, 10 Docker-optional skips                       |

## Decision Log

| Decision                            | Context                                      | Alternatives Considered               | Rationale                                                                  |
|-------------------------------------|----------------------------------------------|---------------------------------------|----------------------------------------------------------------------------|
| Reuse `/login` and `/logout`        | User explicitly wants no weak CSRF bypass    | Add `/admin/api/login`                | Keeps Spring Security's session and CSRF model as the single auth boundary |
| Detect JSON by Accept or XHR header | SPA and fetch clients may vary request style | Only inspect `Accept`                 | Matches requested behavior and avoids changing ordinary browser form flows |
| Reuse safe session projection       | Login success needs sessionInfo              | Duplicate DTO construction in handler | Prevents drift from `GET /admin/api/session` and avoids sensitive fields   |
