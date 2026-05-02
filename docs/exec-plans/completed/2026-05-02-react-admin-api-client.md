# Execution Plan: React Admin API Client

Created: 2026-05-02
Status: Active
Author: agent

## Objective

Add a unified Axios-based API client for `dbflow-admin` that supports Spring Security CSRF cookies, DBFlow `ApiResult`
unwrapping, and stable typed errors without hard-coding backend origins.

## Scope

**In scope:**

- `dbflow-admin/src/api/client.ts`
- `dbflow-admin/src/api/csrf.ts`
- `dbflow-admin/src/types/api.ts`
- `dbflow-admin/src/lib/errors.ts`
- Focused frontend tests when practical
- `docs/PLANS.md` and this execution plan

**Out of scope:**

- React page integration with the new client.
- Session/router handling for `401`.
- Backend API or Spring Security changes.
- Dependency installation, because `axios` is already present in `dbflow-admin/package.json`.

## Constraints

- Use `axios.create()` with a relative base URL.
- For non-GET requests, read `XSRF-TOKEN` from browser cookies and set `X-XSRF-TOKEN`.
- Recognize DBFlow `ApiResult`; unwrap successful `data`; convert unsuccessful results into typed application errors.
- Do not perform a global redirect on `401`; callers and future router/session code must decide.
- Provide `apiGet<T>`, `apiPost<T>`, and `apiDelete<T>` helpers.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/src/api/client.ts`, `dbflow-admin/src/api/csrf.ts`,
  `dbflow-admin/src/types/api.ts`, and `dbflow-admin/src/lib/errors.ts` exist.
- [x] AC-2: Axios uses relative `/admin/api` as `baseURL` and never hard-codes `localhost:8080`.
- [x] AC-3: Non-GET requests copy `XSRF-TOKEN` cookie into `X-XSRF-TOKEN`; GET requests do not add the header.
- [x] AC-4: Successful `ApiResult` responses unwrap to typed `data`; failed `ApiResult` responses throw an error with
  `errorCode` and `message`.
- [x] AC-5: `401` responses propagate as errors without global redirect side effects.
- [x] AC-6: `pnpm --dir dbflow-admin build` passes.

## Implementation Steps

### Step 1: Establish frontend baseline and API client contract

**Files:** this plan
**Verification:** `python3 scripts/check_harness.py`, `pnpm --dir dbflow-admin build`

Status: ✅ Completed
Evidence: Harness validation passed; baseline React admin build passed before edits.
Deviations:

### Step 2: Add typed API client and CSRF helpers

**Files:** `dbflow-admin/src/api/client.ts`, `dbflow-admin/src/api/csrf.ts`,
`dbflow-admin/src/types/api.ts`, `dbflow-admin/src/lib/errors.ts`
**Verification:** `pnpm --dir dbflow-admin build`

Status: ✅ Completed
Evidence: Added `client.ts`, `csrf.ts`, `api.ts`, and `errors.ts`; `pnpm --dir dbflow-admin build` passed after
adjusting `ApiClientError` for the current ES2020 TypeScript target.
Deviations:

### Step 3: Add focused frontend coverage

**Files:** `dbflow-admin/src/api/client.test.ts`, `dbflow-admin/src/api/csrf.test.ts`,
`dbflow-admin/src/lib/errors.test.ts`
**Verification:** targeted `pnpm --dir dbflow-admin test ...` or build fallback if browser test tooling is not
appropriate for the new utility layer

Status: ✅ Completed
Evidence:
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/client.test.ts src/api/csrf.test.ts src/lib/errors.test.ts`
-> 3 files passed, 8 tests passed.
Deviations: Added focused tests because the client owns non-trivial security/error behavior.

### Step 4: Final verification and archive

**Files:** `docs/exec-plans/active/2026-05-02-react-admin-api-client.md`,
`docs/exec-plans/completed/2026-05-02-react-admin-api-client.md`, `docs/PLANS.md`
**Verification:** `pnpm --dir dbflow-admin build`, `python3 scripts/check_harness.py`, `git diff --check`

Status: ✅ Completed
Evidence: `pnpm --dir dbflow-admin build` passed; focused Vitest run passed 3 files / 8 tests; targeted ESLint and
Prettier checks passed; `python3 scripts/check_harness.py` passed; `git diff --check` passed; `./mvnw test` passed
228 tests with 0 failures, 0 errors, and 10 Docker-related skips.
Deviations:

## Progress Log

| Step | Status | Evidence                                               | Notes                                                  |
|------|--------|--------------------------------------------------------|--------------------------------------------------------|
| 1    | ✅      | Harness and frontend build baseline passed             | axios already present.                                 |
| 2    | ✅      | `pnpm --dir dbflow-admin build` passed                 | Added typed Axios API client.                          |
| 3    | ✅      | Focused Vitest run -> 3 files, 8 tests passed          | Covered CSRF, unwrapping, errors, and 401 propagation. |
| 4    | ✅      | Frontend, Harness, diff, and Maven verification passed | Testcontainers skips are expected without Docker.      |

## Decision Log

| Decision                               | Context                                                                                                  | Alternatives Considered                  | Rationale                                                              |
|----------------------------------------|----------------------------------------------------------------------------------------------------------|------------------------------------------|------------------------------------------------------------------------|
| Use `/admin/api` as the Axios base URL | Vite dev server already proxies `/admin/api` to Spring Boot and production serves under the same origin. | Empty base URL with full paths per call. | A relative API base centralizes routing without coupling to localhost. |

## Completion Summary

Completed. Added the React admin Axios API client, CSRF cookie/header helper, typed `ApiResult` contract, and
application API error type. The client uses relative `/admin/api` requests, unwraps successful `ApiResult` payloads,
throws typed errors for failed `ApiResult` payloads, applies `X-XSRF-TOKEN` only to non-safe methods, and leaves `401`
handling to callers.
