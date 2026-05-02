# Execution Plan: React Admin Users Page

Created: 2026-05-02
Completed: 2026-05-02
Status: Completed
Author: agent

## Objective

Implement the DBFlow React admin `/users` page backed by the existing administrator-only JSON user API.

## Scope

**In scope:**

- `dbflow-admin/src/api/users.ts`
- `dbflow-admin/src/types/access.ts`
- `dbflow-admin/src/features/users/index.tsx`
- `dbflow-admin/src/features/users/components/users-table.tsx`
- `dbflow-admin/src/features/users/components/create-user-sheet.tsx`
- `dbflow-admin/src/features/users/components/reset-password-dialog.tsx`
- `dbflow-admin/src/features/users/components/user-actions.tsx`
- `dbflow-admin/src/routes/_authenticated/users.tsx`
- Focused frontend tests for API wiring and user page interactions

**Out of scope:**

- Changing Spring Boot user API contracts.
- Rebuilding project grants, Token management, or audit pages.
- Adding new UI framework dependencies.
- Exposing password hashes or plaintext passwords in user lists.

## Constraints

- Use the unified Axios API client and Spring Security CSRF behavior already established in
  `dbflow-admin/src/api/client.ts`.
- Use TanStack Query for loading and mutation invalidation.
- Keep filters synchronized with TanStack Router search params.
- Use existing DBFlow/shadcn primitives for headers, badges, tables, dialogs, sheets, and empty states.
- Display backend error messages for failed mutations.

## Acceptance Criteria

- [x] AC-1: `fetchUsers()` reads `GET /admin/api/users` and supports `username` and `status` params.
- [x] AC-2: `/users` renders id, username, displayName, role, status, grantCount, activeTokenCount, and actions.
- [x] AC-3: Username and status filters synchronize to URL search.
- [x] AC-4: Create user Sheet submits username, displayName, and optional password.
- [x] AC-5: Disable/enable actions require AlertDialog confirmation.
- [x] AC-6: Reset password Dialog submits a new password.
- [x] AC-7: Successful mutations invalidate user queries and show toast feedback.
- [x] AC-8: Backend mutation errors are displayed to the operator.
- [x] AC-9: User list code and tests do not expose or render `passwordHash`.
- [x] AC-10: `pnpm --dir dbflow-admin build` passes.

## Implementation Steps

### Step 1: Add API and type contract

**Files:** `dbflow-admin/src/api/users.ts`, `dbflow-admin/src/types/access.ts`
**Verification:** focused API test proves paths, params, payloads, and password-hash exclusion.

Status: ✅ Done
Evidence: RED test failed because `./users` did not exist; GREEN targeted API test passed with list/create/toggle/reset
endpoint coverage and safe row assertions.
Deviations:

### Step 2: Build user management page

**Files:** `dbflow-admin/src/features/users/**`, `dbflow-admin/src/routes/_authenticated/users.tsx`
**Verification:** focused browser tests cover list render, URL filter callback, create, disable/enable confirmation,
reset password, and error display.

Status: ✅ Done
Evidence: Browser tests passed for safe table rendering, URL filter callback, create user Sheet, disable confirmation,
and reset-password backend error display.
Deviations:

### Step 3: Verify build and Harness gates

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** targeted frontend tests, React build, Harness validator, and diff hygiene pass.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin build`, `pnpm --dir dbflow-admin test`, `./mvnw test`,
`python3 scripts/check_harness.py`, and `git diff --check` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                            | Notes                                         |
|------|--------|-----------------------------------------------------------------------------------------------------|-----------------------------------------------|
| 1    | ✅      | `pnpm --dir dbflow-admin test src/api/users.test.ts src/features/users/users-page.test.tsx` passed. | User API client added.                        |
| 2    | ✅      | Browser tests covered table, filters, create, disable, reset, and error display.                    | `/users` route now renders `UsersPage`.       |
| 3    | ✅      | Build, full frontend tests, Maven tests, Harness validator, and diff check passed.                  | `routeTree.gen.ts` formatting noise reverted. |

## Decision Log

| Decision                                           | Context                                             | Alternatives Considered                        | Rationale                                                           |
|----------------------------------------------------|-----------------------------------------------------|------------------------------------------------|---------------------------------------------------------------------|
| Keep user list response type limited to `UserRow`. | Backend `UserRow` exposes only safe display fields. | Mirror the JPA entity shape in frontend types. | A narrow frontend type prevents accidental password hash rendering. |

## Completion Summary

Implemented the React `/users` management page for DBFlow Admin. The page now loads `GET /admin/api/users`, keeps
username/status filters in route search, renders safe user rows, supports create user Sheet, enable/disable
confirmation,
reset password dialog, mutation invalidation, success toast feedback, and backend error messages.

Verification passed:

- `pnpm --dir dbflow-admin test src/api/users.test.ts src/features/users/users-page.test.tsx` with 2 files and 8 tests
  passed
- `pnpm --dir dbflow-admin build`
- `pnpm --dir dbflow-admin test` with 27 files and 138 tests passed
- `./mvnw test` with 228 tests, 0 failures, 0 errors, and 10 skipped
- `python3 scripts/check_harness.py`
- `git diff --check`
