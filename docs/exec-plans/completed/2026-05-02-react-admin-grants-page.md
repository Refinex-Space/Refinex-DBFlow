# Execution Plan: React Admin Grants Page

Created: 2026-05-02
Completed: 2026-05-02
Status: Completed
Author: agent

## Objective

Implement the DBFlow React admin `/grants` project/environment authorization page backed by the existing
administrator-only grant JSON APIs.

## Scope

**In scope:**

- `dbflow-admin/src/api/grants.ts`
- Grant-related frontend access types in `dbflow-admin/src/types/access.ts`
- `dbflow-admin/src/features/grants/index.tsx`
- `dbflow-admin/src/features/grants/components/grants-table.tsx`
- `dbflow-admin/src/features/grants/components/create-grant-sheet.tsx`
- `dbflow-admin/src/features/grants/components/edit-project-grants-sheet.tsx`
- `dbflow-admin/src/features/grants/components/grant-filter-bar.tsx`
- `dbflow-admin/src/routes/_authenticated/grants.tsx`
- Generated TanStack route tree update for `/grants`
- Focused frontend tests for API wiring and grant page interactions

**Out of scope:**

- Changing Spring Boot grant API contracts.
- Changing Thymeleaf `/admin/grants`.
- Implementing audit page navigation beyond existing route availability.
- Displaying JDBC URLs, passwords, Token data, or database connection details.

## Constraints

- Use `POST /admin/api/grants/update-project` for both create and edit because the requested UI supports multiple
  environment checkboxes.
- Keep `POST /admin/api/grants/{grantId}/revoke` for direct single-environment revocation.
- Use the unified API client so CSRF behavior remains centralized.
- Use TanStack Query for list/options loading and mutation invalidation.
- Keep filters synchronized with TanStack Router search params.
- Group `environmentOptions` by `projectKey` in the frontend; do not infer projects from grant rows.
- Empty environment options must render configuration guidance, not a runtime error.

## Assumptions

- Grant types remain the backend-supported string values `READ`, `WRITE`, and `ADMIN`.
- Grant statuses remain display strings such as `ACTIVE` and `REVOKED`; unknown statuses render through the existing
  generic status badge fallback.
- A create operation with no selected environment is blocked in the UI; an edit operation with no selected environment
  is allowed and means revoke all environments for that user/project.

## Acceptance Criteria

- [x] AC-1: `fetchGrantGroups()` reads `GET /admin/api/grants` and supports username, projectKey, environmentKey, and
  status params.
- [x] AC-2: `fetchGrantOptions()` reads `GET /admin/api/grants/options`.
- [x] AC-3: `/grants` renders grouped rows by user and project with username, projectKey, environment badges, and
  actions.
- [x] AC-4: Filters for username, projectKey, environmentKey, and status synchronize to URL search.
- [x] AC-5: Create grant Sheet selects user, project, grant type, and multiple environments; submit calls
  `update-project`.
- [x] AC-6: Edit project grants Sheet lets operators check/uncheck project environments and save through
  `update-project`, including empty environment lists.
- [x] AC-7: Direct revoke calls `POST /admin/api/grants/{grantId}/revoke`.
- [x] AC-8: `environmentOptions` are grouped by `projectKey`; empty options show configuration guidance instead of an
  error.
- [x] AC-9: Successful mutations invalidate grant queries and show toast feedback; backend errors are displayed.
- [x] AC-10: The page does not display JDBC, password, or Token data.
- [x] AC-11: `pnpm --dir dbflow-admin build` passes.

## Implementation Steps

### Step 1: Add grant API and type contract

**Files:** `dbflow-admin/src/api/grants.ts`, `dbflow-admin/src/types/access.ts`
**Verification:** focused API test proves paths, params, payloads, update-project, revoke, and safe data shape.

Status: ✅ Done
Evidence: RED test failed because `./grants` did not exist; GREEN targeted API test passed with list/options,
update-project, revoke, and sensitive-shape assertions.
Deviations:

### Step 2: Build grants page and route

**Files:** `dbflow-admin/src/features/grants/**`, `dbflow-admin/src/routes/_authenticated/grants.tsx`,
`dbflow-admin/src/routeTree.gen.ts`
**Verification:** focused browser tests cover list render, filters, create, edit, revoke, empty environment guidance,
and sensitive-data absence.

Status: ✅ Done
Evidence: Browser tests passed for grouped table rendering, URL filter callback, create Sheet, edit Sheet with empty
environment list, revoke confirmation, empty configuration guidance, and backend error display.
Deviations:

### Step 3: Verify build and control plane

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** targeted frontend tests, full frontend tests/build, Maven tests, Harness validator, and diff hygiene.

Status: ✅ Done
Evidence: Targeted frontend tests, full frontend tests, React build, Maven tests, Harness validator, and diff hygiene
passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                               | Notes                                     |
|------|--------|--------------------------------------------------------------------------------------------------------|-------------------------------------------|
| 1    | ✅      | `pnpm --dir dbflow-admin test src/api/grants.test.ts src/features/grants/grants-page.test.tsx` passed. | Grant API client added.                   |
| 2    | ✅      | Browser tests covered table, filters, create, edit, revoke, empty options, and backend errors.         | `/grants` route now renders `GrantsPage`. |
| 3    | ✅      | Build, full frontend tests, Maven tests, Harness validator, sensitive grep, and diff check passed.     | Route tree includes `/grants`.            |

## Decision Log

| Decision                               | Context                                                           | Alternatives Considered                   | Rationale                                                                                          |
|----------------------------------------|-------------------------------------------------------------------|-------------------------------------------|----------------------------------------------------------------------------------------------------|
| Use `update-project` for create Sheet. | The requested create UI supports multiple environment checkboxes. | Call `POST /grants` once per environment. | One project-level mutation matches the backend batch contract and avoids partial success handling. |

## Completion Summary

Implemented the React `/grants` project/environment authorization page for DBFlow Admin. The page loads grant groups
and grant options, keeps username/project/environment/status filters in route search, renders user-by-project grouped
rows, groups environment options by project, supports multi-environment create and edit through `update-project`,
supports direct single-environment revoke, displays empty environment configuration guidance, invalidates grant queries
after mutations, and displays backend error messages.

Verification passed:

- `pnpm --dir dbflow-admin test src/api/grants.test.ts src/features/grants/grants-page.test.tsx` with 2 files and 10
  tests passed
- `pnpm --dir dbflow-admin build`
- `pnpm --dir dbflow-admin test` with 29 files and 148 tests passed
- `./mvnw test` with 228 tests, 0 failures, 0 errors, and 10 skipped
- `python3 scripts/check_harness.py`
- production grants source sensitive grep for `jdbc|JDBC|password|Password|Token|token` returned no matches
- `git diff --check`
