# Execution Plan: Admin Readonly JSON APIs

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Expose read-only JSON APIs for the React admin overview, configuration, dangerous policy, and health pages while
preserving the existing Thymeleaf controllers and management security boundary.

## Scope

**In scope:**

- Add `GET /admin/api/overview` backed by `AdminOverviewViewService.overview()`.
- Add `GET /admin/api/config` backed by `AdminOperationsViewService.configPage()`.
- Add `GET /admin/api/policies/dangerous` backed by `AdminOperationsViewService.dangerousPolicyPage()`.
- Add `GET /admin/api/health` backed by `AdminOperationsViewService.healthPage()`.
- Wrap all responses with `ApiResult.ok(data)`.
- Add `AdminOperationsApiControllerTests` for JSON shape, admin-only access, and sensitive-data exclusion.
- Update Harness docs and verification evidence.

**Out of scope:**

- Changing existing Thymeleaf routes or templates.
- Adding write APIs.
- Changing security rules beyond relying on existing `/admin/api/**` administrator-only protection.
- Adding React client calls.

## Constraints

- API responses must not expose full JDBC URLs, passwords, Token plaintext, Token hashes, or password hashes.
- Existing service view models are the source of truth; do not duplicate redaction logic in controllers.
- Ordinary `/admin/**` Thymeleaf behavior must remain unchanged.

## Assumptions

- `ConfigPageView`, `DangerousPolicyPageView`, `HealthPageView`, and `OverviewPageView` already contain sanitized
  display data suitable for admin UI rendering.
- Existing `AdminSecurityConfiguration` continues to enforce `ROLE_ADMIN` for `/admin/api/**`.

## Acceptance Criteria

- [x] AC-1: `GET /admin/api/overview` returns HTTP `200` JSON `ApiResult.ok` for an administrator.
- [x] AC-2: `GET /admin/api/config` returns HTTP `200` JSON `ApiResult.ok` for an administrator and omits full JDBC
  URL/password-like secrets.
- [x] AC-3: `GET /admin/api/policies/dangerous` returns HTTP `200` JSON `ApiResult.ok` for an administrator and omits
  Token/password/hash fields.
- [x] AC-4: `GET /admin/api/health` returns HTTP `200` JSON `ApiResult.ok` for an administrator and omits full JDBC
  URL/password-like secrets.
- [x] AC-5: Non-admin users cannot access the new `/admin/api/**` endpoints.
- [x] AC-6: `./mvnw -Dtest=AdminOperationsApiControllerTests test` passes.

## Implementation Steps

### Step 1: Add red API tests

**Files:** `src/test/java/com/refinex/dbflow/admin/AdminOperationsApiControllerTests.java`
**Verification:** targeted test fails before implementation because the API routes do not exist.

Status: Done
Evidence: `./mvnw -Dtest=AdminOperationsApiControllerTests test` failed before implementation with HTTP 404 for
`/admin/api/overview`, `/admin/api/config`, `/admin/api/policies/dangerous`, and `/admin/api/health`.

### Step 2: Implement read-only JSON controllers

**Files:**

- `src/main/java/com/refinex/dbflow/admin/controller/AdminOverviewApiController.java`
- `src/main/java/com/refinex/dbflow/admin/controller/AdminOperationsApiController.java`

**Verification:** targeted tests pass.

Status: Done
Evidence: Added `AdminOverviewApiController` and `AdminOperationsApiController`;
`./mvnw -Dtest=AdminOperationsApiControllerTests test`
passed with 5 tests.

### Step 3: Verify, document, and archive

**Files:** docs and repository state
**Verification:** targeted Maven test, relevant admin regression tests, full Maven suite, Harness validator, diff
hygiene.

Status: Done
Evidence:
`./mvnw -Dtest=AdminOperationsApiControllerTests,AdminOperationsPageControllerTests,AdminUiControllerTests,AdminSecurityTests test`
passed with 22 tests. `./mvnw test` passed with 211 tests, 0 failures, 0 errors, and 10 Docker-optional skips.
`python3 scripts/check_harness.py` passed. `git diff --check` passed. Diff-only wildcard import check found no newly
added wildcard imports.

## Progress Log

| Step      | Status | Evidence                                                                           | Notes                                      |
|-----------|--------|------------------------------------------------------------------------------------|--------------------------------------------|
| Preflight | Done   | `python3 scripts/check_harness.py`; `./mvnw test`; missing targeted test red check | Harness passed; full baseline 206/0/10     |
| 1         | Done   | Red run: new API routes returned 404                                               | Non-admin security path remained covered   |
| 2         | Done   | `./mvnw -Dtest=AdminOperationsApiControllerTests test`                             | 5 tests passed                             |
| 3         | Done   | `./mvnw test`; `python3 scripts/check_harness.py`; `git diff --check`              | 211 tests passed, 10 Docker-optional skips |

## Decision Log

| Decision                      | Context                                      | Alternatives Considered      | Rationale                                                           |
|-------------------------------|----------------------------------------------|------------------------------|---------------------------------------------------------------------|
| Reuse existing view services  | Thymeleaf pages already use sanitized models | Add new DTO projection layer | Keeps React API behavior aligned with current admin UI redaction    |
| Rely on `/admin/api/**` chain | Security already requires `ROLE_ADMIN`       | Add method-level annotations | Avoids duplicate policy while tests still prove non-admin rejection |
| Keep controllers read-only    | User asked only for view APIs                | Add mutation or refresh APIs | Keeps scope narrow and avoids new CSRF/write behavior in this stage |
