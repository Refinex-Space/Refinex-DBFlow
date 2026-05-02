# Execution Plan: Admin Token JSON API

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Expose the existing admin MCP Token management capabilities as administrator-only JSON APIs for the React admin shell
while preserving the one-time plaintext Token display boundary.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/admin/controller/`
- `src/main/java/com/refinex/dbflow/admin/dto/`
- `src/test/java/com/refinex/dbflow/admin/`
- `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`

**Out of scope:**

- Thymeleaf `/admin/tokens` behavior changes.
- Token generation, hashing, validation, or storage semantics.
- Audit writer behavior changes.
- React page implementation.

## Constraints

- Reuse `AdminAccessManagementService` for list, issue, revoke, reissue, and active-user option behavior.
- `GET /admin/api/tokens` must not serialize `plaintextToken` or `tokenHash`; list rows may include non-secret
  operation identifiers needed by the admin UI.
- `POST /admin/api/tokens` and `POST /admin/api/users/{userId}/tokens/reissue` may return `plaintextToken` only in
  that successful response.
- Revoke responses must not return plaintext.
- Keep `/admin/api/**` administrator-only and keep CSRF required for all mutation requests.
- Follow DBFlow Java comment standards with Chinese class, field, and parameter comments.

## Acceptance Criteria

- [x] AC-1: `GET /admin/api/tokens?username=&status=` returns filtered Token rows without `plaintextToken` or
  `tokenHash`.
- [x] AC-2: `POST /admin/api/tokens` issues a Token from JSON, requires CSRF, and returns one-time `plaintextToken`.
- [x] AC-3: After issue, subsequent list responses and audit rows do not contain the issued plaintext Token.
- [x] AC-4: `POST /admin/api/users/{userId}/tokens/reissue` returns a new one-time `plaintextToken` and leaves prior
  plaintext absent from list/audit payloads.
- [x] AC-5: `POST /admin/api/tokens/{tokenId}/revoke` revokes a Token and returns no plaintext.
- [x] AC-6: `./mvnw -Dtest=AdminTokenApiControllerTests test` passes.

## Risk Notes

| Risk                                          | Likelihood | Mitigation                                                                                       |
|-----------------------------------------------|------------|--------------------------------------------------------------------------------------------------|
| List API leaks `tokenHash: null` field        | Medium     | Do not serialize existing `TokenRow` directly; map to a dedicated list DTO without `tokenHash`.  |
| Plaintext appears outside issue/reissue reply | Medium     | Add tests that capture plaintext then assert list and audit content do not contain it afterward. |
| API diverges from Thymeleaf Token behavior    | Low        | Reuse `AdminAccessManagementService` and leave `AdminHomeController` untouched.                  |

## Implementation Steps

### Step 1: Add failing controller tests

**Files:** `src/test/java/com/refinex/dbflow/admin/AdminTokenApiControllerTests.java`
**Verification:** `./mvnw -Dtest=AdminTokenApiControllerTests test`

Status: ✅ Completed
Evidence: `./mvnw -Dtest=AdminTokenApiControllerTests test` fails with 404 for `/admin/api/tokens`, confirming the
JSON API is not implemented yet.
Deviations:

### Step 2: Implement Token JSON API

**Files:** `src/main/java/com/refinex/dbflow/admin/controller/AdminTokenApiController.java`,
`src/main/java/com/refinex/dbflow/admin/dto/IssueTokenRequest.java`,
`src/main/java/com/refinex/dbflow/admin/dto/ReissueTokenRequest.java`
**Verification:** `./mvnw -Dtest=AdminTokenApiControllerTests test`

Status: ✅ Completed
Evidence: `./mvnw -Dtest=AdminTokenApiControllerTests test` -> 4 tests, 0 failures, 0 errors, 0 skipped.
Deviations:

### Step 3: Update repository control-plane documentation

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `python3 scripts/check_harness.py`

Status: ✅ Completed
Evidence: Updated architecture, observability, and plans documentation; `python3 scripts/check_harness.py` passed.
Deviations:

### Step 4: Run final verification and archive

**Files:** `docs/exec-plans/active/2026-05-02-admin-token-json-api.md`,
`docs/exec-plans/completed/2026-05-02-admin-token-json-api.md`
**Verification:** `./mvnw -Dtest=AdminTokenApiControllerTests test`, related admin/security tests, `./mvnw test`,
`git diff --check`

Status: ✅ Completed
Evidence:
`./mvnw -Dtest=AdminTokenApiControllerTests,AdminAccessManagementServiceTests,AdminAccessManagementControllerTests,AdminUserApiControllerTests,AdminGrantApiControllerTests,AdminAuditEventControllerTests,AdminSecurityTests test`
-> 31 tests, 0 failures, 0 errors, 0 skipped; `./mvnw test` -> 228 tests, 0 failures, 0 errors, 10 skipped;
`git diff --check` passed; touched-file whitespace/import checks passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                               | Notes                                                |
|------|--------|----------------------------------------------------------------------------------------|------------------------------------------------------|
| 1    | ✅      | RED: `./mvnw -Dtest=AdminTokenApiControllerTests test` -> 3 failures, 404 API missing  | Expected pre-implementation failure.                 |
| 2    | ✅      | `./mvnw -Dtest=AdminTokenApiControllerTests test` -> 4 tests passed                    | Added Token controller and DTOs.                     |
| 3    | ✅      | `python3 scripts/check_harness.py` -> passed                                           | Updated architecture, observability, and plans docs. |
| 4    | ✅      | Related tests 31 passed; full suite 228 passed, 10 skipped; diff/hygiene checks passed | Testcontainers skips are expected without Docker.    |

## Decision Log

| Decision                                | Context                                                                                | Alternatives Considered     | Rationale                                                           |
|-----------------------------------------|----------------------------------------------------------------------------------------|-----------------------------|---------------------------------------------------------------------|
| Use a dedicated Token list response DTO | Existing `TokenRow` has a `tokenHash` property that serializes to JSON even when null. | Return `TokenRow` directly. | A dedicated DTO makes the no-`tokenHash` JSON contract enforceable. |

## Completion Summary

Completed. Added administrator-only React admin Token JSON APIs for list, options, issue, revoke, and reissue. The
list response maps through a dedicated safe DTO that omits `plaintextToken` and `tokenHash`; issue and reissue return
plaintext only in the immediate success response; revoke returns no plaintext. Verification passed for targeted,
related, full Maven, Harness, and diff/hygiene checks.
