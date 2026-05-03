# Execution Plan: React Admin Health Page

Created: 2026-05-03
Status: Completed
Author: agent

## Objective

Implement a read-only React `/health` page that displays DBFlow system health from `/admin/api/health`.

## Scope

**In scope:**

- `dbflow-admin/src/api/health.ts`
- `dbflow-admin/src/types/health.ts`
- `dbflow-admin/src/features/health/index.tsx`
- `dbflow-admin/src/features/health/components/health-card.tsx`
- `dbflow-admin/src/routes/_authenticated/health.tsx`
- Focused frontend API/page tests and generated TanStack Router route tree.

**Out of scope:**

- Backend API changes.
- Actuator endpoint changes.
- Health mutation/remediation actions.
- Thymeleaf admin health page changes.

## Constraints

- Follow existing React admin patterns for typed API clients, TanStack Query v5 object syntax, shadcn/ui, layout header,
  loading, error, and card-based read-only displays.
- Consume only the backend `HealthPageView` fields: `overall`, `tone`, `totalCount`, `unhealthyCount`, and `items`.
- Do not introduce or display JDBC passwords, complete JDBC URLs, Token plaintext, Token hashes, or password hashes.
- Use `queryClient.invalidateQueries({ queryKey })` for the refresh action.

## Assumptions

- `/admin/api/health` returns `HealthPageView` with `HealthItem` rows containing `name`, `component`, `status`,
  `description`, `detail`, and `tone`.
- Backend health details are already sanitized; the frontend still avoids defining sensitive fields and verifies
  rendered fixtures exclude them.
- Route `/health` is listed in sidebar data already; this task adds the matching React route.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/src/api/health.ts` fetches `/health` through the shared API client and typed health page
  model.
- [x] AC-2: `/health` displays overall status and `unhealthyCount/totalCount`.
- [x] AC-3: The page renders health items for metadata database, target datasource registry, Nacos, and MCP endpoint.
- [x] AC-4: Each health item displays `name`, `component`, `status`, `description`, `detail`, and `tone`.
- [x] AC-5: The refresh button invalidates the health query key.
- [x] AC-6: Production health frontend code does not define or render JDBC password, full JDBC URL, Token
  plaintext/hash, or password hash fields.
- [x] AC-7: `pnpm --dir dbflow-admin build` passes.

## Risk Notes

| Risk                                                            | Likelihood | Mitigation                                                             |
|-----------------------------------------------------------------|------------|------------------------------------------------------------------------|
| Health API field names drift from backend records               | Low        | Inspect Java records and cover exact fields in fixtures.               |
| Refresh button only calls refetch instead of invalidating query | Medium     | Test through QueryClient cache invalidation behavior.                  |
| Sensitive data appears in UI fixtures or types                  | Low        | Limit health types to sanitized backend records and run targeted grep. |

## Implementation Steps

### Step 1: Add focused frontend tests

**Files:** `dbflow-admin/src/api/health.test.ts`, `dbflow-admin/src/features/health/health-page.test.tsx`
**Verification:** Targeted Vitest run fails for missing health modules before implementation.

Status: ✅ Completed
Evidence:
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/health.test.ts src/features/health/health-page.test.tsx`
failed before implementation because `./health` and `./index` were unresolved.
Deviations:

### Step 2: Implement health API and types

**Files:** `dbflow-admin/src/api/health.ts`, `dbflow-admin/src/types/health.ts`
**Verification:** API test passes and confirms no sensitive fields in returned fixture.

Status: ✅ Completed
Evidence: Added typed `HealthPage` and `HealthItem` models plus `fetchHealthPage()` using the shared API client;
targeted Vitest passed after implementation.
Deviations:

### Step 3: Implement read-only health page and cards

**Files:** `dbflow-admin/src/features/health/index.tsx`, `dbflow-admin/src/features/health/components/health-card.tsx`
**Verification:** Page test passes for summary, item fields, refresh invalidation, and sensitive-field absence.

Status: ✅ Completed
Evidence: Page tests passed for overall summary, `unhealthyCount/totalCount`, component health items, refresh
invalidation, and sensitive-field absence.
Deviations:

### Step 4: Register authenticated route

**Files:** `dbflow-admin/src/routes/_authenticated/health.tsx`, `dbflow-admin/src/routeTree.gen.ts`
**Verification:** `pnpm --dir dbflow-admin build` includes the route and passes.

Status: ✅ Completed
Evidence: Added authenticated `/health` route; `pnpm --dir dbflow-admin build` regenerated `routeTree.gen.ts` and
completed successfully.
Deviations:

### Step 5: Verify and archive

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** Run targeted tests, frontend build, Maven tests, Harness validator, `git diff --check`, and
sensitive-field grep; then archive plan.

Status: ✅ Completed
Evidence: Targeted Vitest, frontend build, Maven tests, Harness validator, `git diff --check`, and sensitive-field grep
passed before archival.
Deviations:

## Progress Log

| Step | Status | Evidence                                                             | Notes                            |
|------|--------|----------------------------------------------------------------------|----------------------------------|
| 1    | ✅      | Red Vitest failed on missing health modules.                         | Test-first guard established.    |
| 2    | ✅      | Targeted Vitest passed after adding API/types.                       | Uses shared `apiGet`.            |
| 3    | ✅      | Targeted Vitest passed for summary, cards, refresh, and redaction.   | Read-only page only.             |
| 4    | ✅      | `pnpm --dir dbflow-admin build` passed.                              | TanStack Router route generated. |
| 5    | ✅      | Frontend, backend, Harness, diff, and sensitive-field checks passed. | Plan archived.                   |

## Decision Log

| Decision                           | Context                                                  | Alternatives Considered | Rationale                                                 |
|------------------------------------|----------------------------------------------------------|-------------------------|-----------------------------------------------------------|
| Use query invalidation for refresh | User explicitly requested invalidating the health query. | Direct `refetch()`.     | Aligns with requested behavior and TanStack Query v5 API. |

## Completion Summary

Implemented the React `/health` system health page for DBFlow Admin. The page consumes `/admin/api/health`, shows the
overall health state, unhealthy/total count, and component cards for metadata database, target datasource registry,
Nacos, and MCP endpoint. The refresh action invalidates the `['health']` query. Production health frontend code keeps
the sanitized backend contract and does not define or render JDBC password, complete JDBC URL, Token plaintext/hash, or
password hash fields.

Verification evidence:

-
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/health.test.ts src/features/health/health-page.test.tsx`
passed: 2 files, 3 tests.
- `pnpm --dir dbflow-admin build` passed.
- `./mvnw test` passed: 228 tests, 0 failures, 0 errors, 10 skipped.
- `python3 scripts/check_harness.py` passed.
- `git diff --check` passed.
- Sensitive-field grep on production health frontend files returned no matches.
