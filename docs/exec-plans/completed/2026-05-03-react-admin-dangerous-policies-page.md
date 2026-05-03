# Execution Plan: React Admin Dangerous Policies Page

Created: 2026-05-03
Status: Completed
Author: agent

## Objective

Implement a read-only React `/policies/dangerous` page that renders the sanitized dangerous DDL policy API for DBFlow
administrators.

## Scope

**In scope:**

- `dbflow-admin/src/api/policies.ts`
- `dbflow-admin/src/types/policy.ts`
- `dbflow-admin/src/features/policies/dangerous/**`
- `dbflow-admin/src/routes/_authenticated/policies/dangerous.tsx`
- Focused frontend API/page tests and generated TanStack Router route tree.

**Out of scope:**

- Backend API changes.
- Audit page implementation.
- Policy mutation or whitelist editing.
- Thymeleaf admin pages.

## Constraints

- Follow the existing React admin patterns for API clients, TanStack Query, shadcn/ui, layout header, loading, error,
  and empty states.
- Use the existing sanitized backend view fields; do not introduce password, Token, full JDBC URL, or datasource
  connection details in frontend policy types.
- Keep the page read-only.
- Use Harness verification before claiming completion.

## Assumptions

- `/admin/api/policies/dangerous` returns `DangerousPolicyPageView` with `defaults`, `whitelist`, `rules`, and
  `emptyHint`.
- `/audit?decision=POLICY_DENIED` may be wired later; this task only needs to provide the navigation target.
- `allowProd` is represented by the backend as a display string such as `YES` or `NO`.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/src/api/policies.ts` fetches `/policies/dangerous` through the shared API client and typed
  policy page model.
- [x] AC-2: `/policies/dangerous` renders default high-risk policies with operation, risk, decision, and requirement.
- [x] AC-3: `/policies/dangerous` renders DROP whitelist rows with operation, risk, project, env, schema, table,
  allowProd, and prodRule.
- [x] AC-4: The page renders fixed strengthened rules for DROP whitelist, TRUNCATE confirmation, and prod strengthening.
- [x] AC-5: The `查看被拒绝审计` action points to `/audit?decision=POLICY_DENIED`.
- [x] AC-6: Empty whitelist data shows a clear policy guidance message.
- [x] AC-7: `pnpm --dir dbflow-admin build` passes.

## Risk Notes

| Risk                                                              | Likelihood | Mitigation                                                                 |
|-------------------------------------------------------------------|------------|----------------------------------------------------------------------------|
| Frontend field names drift from backend view records              | Low        | Inspect Java view records and cover API/page fixtures with exact fields.   |
| Generated route tree changes unrelated formatting                 | Medium     | Keep generated file only after adding the semantic route and verify build. |
| Sensitive fields accidentally introduced in frontend policy types | Low        | Limit types to backend sanitized fields and run targeted grep.             |

## Implementation Steps

### Step 1: Add focused frontend tests

**Files:** `dbflow-admin/src/api/policies.test.ts`,
`dbflow-admin/src/features/policies/dangerous/dangerous-policies-page.test.tsx`
**Verification:** Targeted Vitest run fails for missing modules before implementation.

Status: ✅ Done
Evidence:
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/policies.test.ts src/features/policies/dangerous/dangerous-policies-page.test.tsx`
initially failed for missing `./policies` and `./index` modules before implementation.
Deviations:

### Step 2: Implement policy API and types

**Files:** `dbflow-admin/src/api/policies.ts`, `dbflow-admin/src/types/policy.ts`
**Verification:** API test passes and confirms no sensitive policy fields.

Status: ✅ Done
Evidence:
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/policies.test.ts src/features/policies/dangerous/dangerous-policies-page.test.tsx`
passed 2 files / 4 tests after implementation.
Deviations:

### Step 3: Implement read-only dangerous policy page

**Files:** `dbflow-admin/src/features/policies/dangerous/**`
**Verification:** Page test passes for policy defaults, whitelist, rules, denied-audit link, empty state, and reason
sheet.

Status: ✅ Done
Evidence: Page test covers default policy rows, whitelist rows, strengthened rules, denied-audit link, empty whitelist
state, and reason Sheet.
Deviations:

### Step 4: Register authenticated route

**Files:** `dbflow-admin/src/routes/_authenticated/policies/dangerous.tsx`, `dbflow-admin/src/routeTree.gen.ts`
**Verification:** `pnpm --dir dbflow-admin build` includes the route and passes.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin build` passed and `dbflow-admin/src/routeTree.gen.ts` contains
`/_authenticated/policies/dangerous`.
Deviations:

### Step 5: Verify and archive

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** Run targeted tests, frontend build, Harness validator, and sensitive-field grep; then archive plan.

Status: ✅ Done
Evidence: Targeted Vitest, frontend build, `./mvnw test`, `python3 scripts/check_harness.py`, `git diff --check`, and
sensitive-field grep completed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                             | Notes               |
|------|--------|----------------------------------------------------------------------|---------------------|
| 1    | ✅      | Red test failed for missing modules before implementation.           | Expected RED state. |
| 2    | ✅      | Targeted Vitest passed after API/types implementation.               |                     |
| 3    | ✅      | Page test passed for required render states.                         |                     |
| 4    | ✅      | Frontend build passed and route tree includes `/policies/dangerous`. |                     |
| 5    | ✅      | Full verification gates passed.                                      |                     |

## Decision Log

| Decision                                       | Context                                    | Alternatives Considered  | Rationale                                                |
|------------------------------------------------|--------------------------------------------|--------------------------|----------------------------------------------------------|
| Use a plain anchor for denied-audit navigation | React `/audit` route is not in this scope. | Add a placeholder route. | Preserves requested target without adding fake audit UI. |

## Completion Summary

Completed: 2026-05-03
Duration: 5 steps
All acceptance criteria: PASS

Summary:

- Added typed policy API client and React read-only dangerous policy page.
- Rendered default policy rows, DROP whitelist rows, fixed strengthened rules, empty whitelist guidance, and reason
  detail Sheet.
- Registered authenticated TanStack Router route `/policies/dangerous`.
- Verified with targeted frontend tests, frontend build, backend Maven tests, Harness validator, diff whitespace check,
  and sensitive-field grep.
