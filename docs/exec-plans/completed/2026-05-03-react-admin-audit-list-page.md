# Execution Plan: React Admin Audit List Page

Created: 2026-05-03
Status: Completed
Author: agent

## Objective

Implement a React `/audit` audit list page that reads `/admin/api/audit-events`, keeps filters and pagination in the
URL, and matches the current Thymeleaf `/admin/audit` filtering capability.

## Scope

**In scope:**

- `dbflow-admin/src/api/audit.ts`
- `dbflow-admin/src/types/audit.ts`
- `dbflow-admin/src/features/audit/list/index.tsx`
- `dbflow-admin/src/features/audit/list/components/audit-table.tsx`
- `dbflow-admin/src/features/audit/list/components/audit-filter-sheet.tsx`
- `dbflow-admin/src/features/audit/list/components/audit-filter-chips.tsx`
- Authenticated `/audit` route, generated TanStack Router route tree, and minimal `/audit/:eventId` route target
  required by the list action.
- Focused frontend API/page tests.

**Out of scope:**

- Backend audit API changes.
- React audit detail page implementation beyond a route target for navigation.
- Thymeleaf `/admin/audit` behavior changes.
- Audit mutation/remediation actions.

## Constraints

- Use the existing shared Axios API client and DBFlow React admin layout patterns.
- Query parameters must be synchronized with the URL: `from`, `to`, `userId`, `project`, `env`, `risk`, `decision`,
  `sqlHash`, `tool`, `page`, `size`, `sort`, and `direction`.
- Request `GET /admin/api/audit-events` with backend parameter names exactly matching `AuditQueryFilter`.
- Support server-side pagination through `page` and `size`; do not page client-side.
- Filter Sheet must include all current Thymeleaf filters: user ID, project, env, risk, decision, SQL hash, tool, and
  page size. The React route also preserves time and sorting parameters.
- Do not define or render database passwords, full JDBC URLs, Token plaintext/hash, or password hash fields.

## Assumptions

- Backend `AuditEventSummary` returns sanitized list rows with `id`, `userId`, `projectKey`, `environmentKey`, `tool`,
  `operationType`, `riskLevel`, `decision`, `sqlHash`, `resultSummary`, and `createdAt`.
- User display in the list can use `userId` because the JSON list summary currently does not expose username.
- Detail navigation can route to `/audit/:eventId`; detail content is intentionally minimal in this task and can be
  expanded in a later prompt.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/src/api/audit.ts` fetches `/audit-events` through the shared API client using normalized
  server-side filters.
- [x] AC-2: `/audit` synchronizes all requested query parameters with TanStack Router search state.
- [x] AC-3: The table displays `createdAt`, user, project/env, tool, operation, risk, decision, `sqlHash`, summary, and
  a detail action.
- [x] AC-4: Page/size controls use backend pagination metadata and update the URL.
- [x] AC-5: Filter Sheet provides all current Thymeleaf filters and preserves time/sort parameters.
- [x] AC-6: Active filter chips show applied filters and allow reset.
- [x] AC-7: SQL Hash can be copied.
- [x] AC-8: Detail action navigates to `/audit/:eventId`.
- [x] AC-9: Production audit frontend code does not define or render JDBC password, full JDBC URL, Token plaintext/hash,
  or password hash fields.
- [x] AC-10: `pnpm --dir dbflow-admin build` passes.

## Risk Notes

| Risk                                            | Likelihood | Mitigation                                                                           |
|-------------------------------------------------|------------|--------------------------------------------------------------------------------------|
| Backend and route parameter names drift         | Low        | Use `AuditQueryFilter` as source of truth and cover normalized params in API tests.  |
| Pagination accidentally becomes client-side     | Medium     | Type API response as backend page metadata and drive buttons from `page/totalPages`. |
| Detail route scope grows beyond list prompt     | Medium     | Add only minimal route target; leave full detail page for a later task.              |
| Sensitive audit data leaks into frontend fields | Low        | Limit types to sanitized DTOs and run targeted sensitive-field grep.                 |

## Implementation Steps

### Step 1: Add focused frontend tests

**Files:** `dbflow-admin/src/api/audit.test.ts`, `dbflow-admin/src/features/audit/list/audit-list-page.test.tsx`
**Verification:** Targeted Vitest run fails for missing audit modules before implementation.

Status: ✅ Completed
Evidence:
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/audit.test.ts src/features/audit/list/audit-list-page.test.tsx`
failed before implementation because `./audit` and `./index` were unresolved.
Deviations:

### Step 2: Implement audit API and types

**Files:** `dbflow-admin/src/api/audit.ts`, `dbflow-admin/src/types/audit.ts`
**Verification:** API tests pass for query normalization and sanitized type surface.

Status: ✅ Completed
Evidence: API test passed and verified `/audit-events` GET parameters for all requested server-side filters plus
pagination and sorting.
Deviations:

### Step 3: Implement audit table, filter sheet, and chips

**Files:** audit list feature components
**Verification:** Page tests pass for row rendering, active chips, filter submission, pagination, copy affordance, and
detail navigation target.

Status: ✅ Completed
Evidence: Page tests passed for row rendering, active chips, filter submission, server-page navigation, SQL Hash copy,
sensitive-field absence, and detail link target.
Deviations:

### Step 4: Register authenticated audit routes

**Files:** `dbflow-admin/src/routes/_authenticated/audit.tsx`,
`dbflow-admin/src/routes/_authenticated/audit.$eventId.tsx`, `dbflow-admin/src/routeTree.gen.ts`
**Verification:** `pnpm --dir dbflow-admin build` regenerates the route tree and passes.

Status: ✅ Completed
Evidence: `pnpm --dir dbflow-admin build` passed and regenerated `routeTree.gen.ts` with `/audit` and `/audit/$eventId`.
Deviations:

### Step 5: Verify and archive

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** Run targeted frontend tests, frontend build, Maven tests if backend safety scope requires, Harness
validator, `git diff --check`, and sensitive-field grep; then archive plan.

Status: ✅ Completed
Evidence: Targeted Vitest, frontend build, Maven tests, Harness validator, `git diff --check`, and sensitive-field grep
passed before archival.
Deviations:

## Progress Log

| Step | Status | Evidence                                                             | Notes                         |
|------|--------|----------------------------------------------------------------------|-------------------------------|
| 1    | ✅      | Red Vitest failed on missing audit modules.                          | Test-first guard established. |
| 2    | ✅      | Targeted Vitest passed.                                              | Shared `apiGet` client used.  |
| 3    | ✅      | Targeted Vitest passed.                                              | Read-only list UI only.       |
| 4    | ✅      | Frontend build passed and route tree includes audit routes.          | Minimal detail target added.  |
| 5    | ✅      | Frontend, backend, Harness, diff, and sensitive-field checks passed. | Plan archived.                |

## Decision Log

| Decision                                     | Context                                                                         | Alternatives Considered            | Rationale                                                               |
|----------------------------------------------|---------------------------------------------------------------------------------|------------------------------------|-------------------------------------------------------------------------|
| Use `userId` as the list user display        | JSON summary exposes `userId`, while Thymeleaf view model may enrich user text. | Add backend username projection.   | Avoid backend scope change and keep this task frontend-only.            |
| Add a minimal `/audit/:eventId` route target | The list must navigate to `/audit/:eventId`, but detail UI is not requested.    | Use plain anchor to unknown route. | A real route avoids SPA 404 and keeps detail expansion straightforward. |

## Completion Summary

Implemented the React `/audit` audit list page for DBFlow Admin. The page consumes `/admin/api/audit-events`, keeps the
requested filters and pagination parameters in route search state, renders server-side paged audit rows, provides a
Thymeleaf-equivalent filter Sheet plus active chips, supports SQL Hash copy, and routes row details to
`/audit/:eventId`.

Verification evidence:

-
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/audit.test.ts src/features/audit/list/audit-list-page.test.tsx`
passed: 2 files, 6 tests.
- `pnpm --dir dbflow-admin build` passed.
- `./mvnw test` passed: 228 tests, 0 failures, 0 errors, 10 skipped.
- `python3 scripts/check_harness.py` passed.
- `git diff --check` passed.
- Sensitive-field grep on production audit frontend files returned no matches.
