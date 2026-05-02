# Execution Plan: React Admin Overview Dashboard

Created: 2026-05-02
Completed: 2026-05-02
Status: Completed
Author: agent

## Objective

Migrate the DBFlow management overview surface from the temporary React shell page to a real React Dashboard backed by
`GET /admin/api/overview`.

## Scope

**In scope:**

- `dbflow-admin/src/api/overview.ts`
- `dbflow-admin/src/types/overview.ts`
- `dbflow-admin/src/features/dashboard/index.tsx`
- `dbflow-admin/src/features/dashboard/components/*`
- Focused frontend tests for API wiring and dashboard render states

**Out of scope:**

- Changing Spring Boot overview API contracts.
- Removing the existing Thymeleaf `/admin` overview.
- Implementing functional environment filtering before the backend supports it.
- Adding new UI dependencies.

## Constraints

- Use TanStack Query with query key `['overview']`.
- Use existing DBFlow primitives such as `PageHeader`, `MetricCard`, `RiskBadge`, `DecisionBadge`, `StatusBadge`,
  `EnvBadge`, and `EmptyState`.
- Environment selector may remain visible but must be disabled or explicitly marked as pending backend filtering.
- Preserve dense internal-tool layout; no marketing hero or decorative dashboard copy.

## Acceptance Criteria

- [x] AC-1: `fetchOverview()` reads `GET /admin/api/overview` through the unified API client.
- [x] AC-2: Dashboard page uses TanStack Query key `['overview']` and renders backend `windowLabel` in `PageHeader`.
- [x] AC-3: Dashboard renders `overview.metrics` as metric cards.
- [x] AC-4: Dashboard renders recent audit rows with time, user, project/env, risk, decision, sqlHash, and a detail
  action.
- [x] AC-5: Dashboard renders attention items and an empty state when no items exist.
- [x] AC-6: Environment selector is visible but disabled or otherwise non-deceptive until filtering is wired.
- [x] AC-7: Loading skeleton and empty states are present.
- [x] AC-8: `pnpm --dir dbflow-admin build` passes.

## Implementation Steps

### Step 1: Add overview client contract

**Files:** `dbflow-admin/src/api/overview.ts`, `dbflow-admin/src/types/overview.ts`
**Verification:** focused API test proves the client calls `/overview` and unwraps typed data.

Status: ✅ Done
Evidence: RED test failed because `./overview` did not exist; GREEN targeted test passed with `fetchOverview()` calling
`/overview`.
Deviations:

### Step 2: Build dashboard sections

**Files:** `dbflow-admin/src/features/dashboard/index.tsx`, `dbflow-admin/src/features/dashboard/components/*`
**Verification:** focused browser tests cover loading, data, table, attention, and empty states.

Status: ✅ Done
Evidence: Dashboard browser test passed for overview data, metrics, recent audit table, attention items, disabled
environment selector, loading skeleton, and empty states.
Deviations:

### Step 3: Verify route build and control plane

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** React build, targeted tests, Harness validator, and diff hygiene pass.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin build`, `pnpm --dir dbflow-admin test`, and `./mvnw test` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                                  | Notes                                         |
|------|--------|-----------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| 1    | ✅      | `pnpm --dir dbflow-admin test src/api/overview.test.ts src/features/dashboard/dashboard.test.tsx` passed. | API contract added.                           |
| 2    | ✅      | Browser tests covered data, loading, and empty render states.                                             | Dashboard split into small components.        |
| 3    | ✅      | Build, full frontend tests, and Maven tests passed.                                                       | `routeTree.gen.ts` formatting noise reverted. |

## Decision Log

| Decision                                          | Context                                                                 | Alternatives Considered                      | Rationale                                                |
|---------------------------------------------------|-------------------------------------------------------------------------|----------------------------------------------|----------------------------------------------------------|
| Keep environment selector disabled for this task. | Backend overview API currently exposes options but no filter parameter. | Add client-only filtering or fake selection. | Disabled UI is honest and avoids misleading operators.   |
| Use existing DBFlow component primitives.         | Previous step centralized status and layout primitives.                 | Inline badge/tone classes in the page.       | Reuse prevents status styling drift across future pages. |

## Completion Summary

Migrated the React admin default Dashboard from the temporary shell page to real `/admin/api/overview` data. The page
now renders a DBFlow overview header, metrics, recent audit events, attention items, a disabled environment selector
for future filtering, loading skeletons, empty states, and error feedback.

Verification passed:

- `pnpm --dir dbflow-admin test src/api/overview.test.ts src/features/dashboard/dashboard.test.tsx` with 2 files and 4
  tests passed
- `pnpm --dir dbflow-admin build`
- `pnpm --dir dbflow-admin test` with 25 files and 130 tests passed
- `./mvnw test` with 228 tests, 0 failures, 0 errors, and 10 skipped
- `./mvnw -Preact-admin -DskipTests package`, copying 107 React admin resources into
  `target/classes/static/admin-next`
