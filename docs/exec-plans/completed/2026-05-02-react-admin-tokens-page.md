# Execution Plan: React Admin Tokens Page

Created: 2026-05-02
Completed: 2026-05-02
Status: Completed
Author: agent

## Objective

Implement the DBFlow React admin `/tokens` MCP Token management page backed by the existing administrator-only Token
JSON APIs, with strict one-time plaintext Token display handling.

## Scope

**In scope:**

- `dbflow-admin/src/api/tokens.ts`
- `dbflow-admin/src/types/token.ts`
- `dbflow-admin/src/features/tokens/index.tsx`
- `dbflow-admin/src/features/tokens/components/tokens-table.tsx`
- `dbflow-admin/src/features/tokens/components/issue-token-sheet.tsx`
- `dbflow-admin/src/features/tokens/components/token-reveal-dialog.tsx`
- `dbflow-admin/src/features/tokens/components/token-actions.tsx`
- `dbflow-admin/src/routes/_authenticated/tokens.tsx`
- Generated TanStack route tree update for `/tokens`
- Focused frontend tests for API wiring, Token list redaction, issue/reissue reveal, revoke confirmation, and storage
  safety.

**Out of scope:**

- Changing Spring Boot Token API contracts.
- Changing Thymeleaf `/admin/tokens`.
- Persisting session/auth state changes.
- Displaying Token hash or plaintext Token in list rows.

## Constraints

- Use the unified Axios API client so CSRF behavior remains centralized.
- Use TanStack Query mutations and invalidate Token list/options queries on successful writes.
- Keep username/status filters synchronized with TanStack Router search params.
- Token plaintext may be held only in local React component state for the reveal dialog.
- Closing `TokenRevealDialog` must clear plaintext state immediately.
- Do not write plaintext Token values to `localStorage`, `sessionStorage`, Zustand persist, URL search params, or query
  cache.
- The Token list must render only safe row fields: id, username, tokenPrefix, status, expiresAt, lastUsedAt, actions.

## Assumptions

- Token statuses are backend strings such as `ACTIVE`, `REVOKED`, and `EXPIRED`; unknown statuses render through the
  existing generic status badge fallback.
- `GET /admin/api/tokens/options` exists and returns active users for issue/reissue forms.
- Default reissue expiry is 30 days.

## Acceptance Criteria

- [x] AC-1: `fetchTokens()` reads `GET /admin/api/tokens` and supports username/status params.
- [x] AC-2: `fetchTokenOptions()` reads `GET /admin/api/tokens/options`.
- [x] AC-3: `issueToken()` posts `POST /admin/api/tokens` with `userId` and `expiresInDays`.
- [x] AC-4: `reissueToken()` posts `POST /admin/api/users/{userId}/tokens/reissue` with `expiresInDays`.
- [x] AC-5: `revokeToken()` posts `POST /admin/api/tokens/{tokenId}/revoke`.
- [x] AC-6: `/tokens` renders list columns id, username, tokenPrefix, status, expiresAt, lastUsedAt, and actions.
- [x] AC-7: Username/status filters synchronize to URL search.
- [x] AC-8: Issue Token Sheet selects active user and `expiresInDays`, then opens `TokenRevealDialog` on success.
- [x] AC-9: Reissue action supports default `expiresInDays = 30`, then opens `TokenRevealDialog` on success.
- [x] AC-10: Revoke action requires confirmation and never shows plaintext.
- [x] AC-11: Closing `TokenRevealDialog` clears plaintext local state.
- [x] AC-12: Production Token page source does not persist `plaintextToken` to `localStorage`, `sessionStorage`, or
  Zustand persist.
- [x] AC-13: Token list never displays `plaintextToken`, token hash, JDBC, password, or database connection details.
- [x] AC-14: `pnpm --dir dbflow-admin build` passes.

## Implementation Steps

### Step 1: Add Token API and type contract

**Files:** `dbflow-admin/src/api/tokens.ts`, `dbflow-admin/src/types/token.ts`
**Verification:** focused API test proves paths, params, payloads, issue/reissue plaintext response shape, revoke, and
safe list shape.

Status: ✅ Done
Evidence: RED test failed because `./tokens` and `./index` did not exist; GREEN targeted API test passed with list
filters/options, issue, reissue, revoke, and safe-list shape assertions.
Deviations:

### Step 2: Build Token page, route, and one-time reveal flow

**Files:** `dbflow-admin/src/features/tokens/**`, `dbflow-admin/src/routes/_authenticated/tokens.tsx`,
`dbflow-admin/src/routeTree.gen.ts`
**Verification:** focused browser tests cover list rendering, filters, issue reveal, reissue reveal, revoke
confirmation, close-clears-plaintext behavior, and sensitive-data absence from list.

Status: ✅ Done
Evidence: Targeted browser tests passed for safe list rendering, URL filter callback, issue reveal, reissue reveal,
revoke confirmation, close-clears-plaintext behavior, and backend error display.
Deviations:

### Step 3: Verify build, storage boundary, and control plane

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** targeted frontend tests, full frontend tests/build, Maven tests when relevant, Harness validator,
sensitive grep, storage grep, and diff hygiene.

Status: ✅ Done
Evidence: React build, full frontend tests, Maven tests, storage-boundary grep, sensitive-field grep, Harness validator,
and diff hygiene passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                                         | Notes                          |
|------|--------|------------------------------------------------------------------------------------------------------------------|--------------------------------|
| 1    | ✅      | `pnpm --dir dbflow-admin test src/api/tokens.test.ts src/features/tokens/tokens-page.test.tsx` passed.           | Token API client added.        |
| 2    | ✅      | Browser tests covered list safety, filters, issue/reissue reveal, revoke, close clear, and errors.               | `/tokens` route source added.  |
| 3    | ✅      | Build, full frontend tests, Maven tests, storage grep, sensitive grep, Harness validator, and diff check passed. | Route tree includes `/tokens`. |

## Decision Log

| Decision                                        | Context                                                             | Alternatives Considered                                          | Rationale                                                                                          |
|-------------------------------------------------|---------------------------------------------------------------------|------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| Keep plaintext in parent page local state only. | The issue/reissue responses legitimately return one-time plaintext. | Put plaintext in query cache, Zustand, route search, or storage. | Local state is inspectable and cleared on dialog close; it avoids persistence and cache retention. |

## Completion Summary

Implemented the React `/tokens` MCP Token management page for DBFlow Admin. The page loads safe Token rows and active
user options, keeps username/status filters in route search, renders id/username/prefix/status/expiry/last-used/action
columns, supports Token issue and reissue with one-time `TokenRevealDialog`, supports revoke confirmation, invalidates
Token queries after mutations, and keeps plaintext out of list rendering and persistent browser storage.

Verification passed:

- `pnpm --dir dbflow-admin test src/api/tokens.test.ts src/features/tokens/tokens-page.test.tsx` with 2 files and 9
  tests passed
- `pnpm --dir dbflow-admin build`
- `pnpm --dir dbflow-admin test` with 31 files and 157 tests passed
- `./mvnw test` with 228 tests, 0 failures, 0 errors, and 10 skipped
- `python3 scripts/check_harness.py`
- production Token source storage grep for `localStorage|sessionStorage|zustand|persist(...)|createJSONStorage`
  returned no matches
- production Token source sensitive grep for `tokenHash|jdbc|JDBC|password|Password` returned no matches
- `git diff --check`
