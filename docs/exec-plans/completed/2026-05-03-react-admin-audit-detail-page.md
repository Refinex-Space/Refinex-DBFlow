# Execution Plan: React Admin Audit Detail Page

Created: 2026-05-03
Status: Completed
Author: agent

## Objective

Implement a React `/audit/:eventId` audit detail page that reads `/admin/api/audit-events/{id}` and presents a
read-only, sanitized audit investigation view.

## Scope

**In scope:**

- `dbflow-admin/src/features/audit/detail/index.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-identity-panel.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-sql-panel.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-failure-panel.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-timeline.tsx`
- `dbflow-admin/src/components/dbflow/sql-code-viewer.tsx`
- Extend `dbflow-admin/src/api/audit.ts` and `dbflow-admin/src/types/audit.ts` for detail fetch/type.
- Replace the existing minimal `/audit/$eventId` placeholder route with the real detail page.
- Focused frontend API/page tests.

**Out of scope:**

- Backend audit API changes.
- Monaco editor integration.
- Audit mutation, replay, remediation, or export actions.
- Changes to Thymeleaf `/admin/audit/{id}`.

## Constraints

- Use the existing shared Axios API client and TanStack Query v5 object syntax.
- Use TanStack Router dynamic params through `$eventId` and `Route.useParams()`.
- Use shadcn Card/Button layout and DBFlow badges; keep the page dense and read-only.
- Use a lightweight `<pre>` SQL viewer in this task; Monaco can be a separate enhancement.
- Do not define or render Token plaintext, Token hash, database passwords, complete JDBC URLs, password hash, or Token
  prefixes.

## Assumptions

- Backend `AuditEventDetail` returns sanitized fields only: identity/context metadata, SQL hash/text, decision/status,
  error metadata, result summary, and created time.
- The requested timeline is a UI reconstruction from persisted detail fields, not a separate backend timeline API.
- `failureReason` maps to backend `errorMessage` because the current JSON DTO names the field `errorMessage`.

## Acceptance Criteria

- [x] AC-1: `GET /admin/api/audit-events/{id}` is exposed through typed React API helpers and query keys.
- [x] AC-2: `/audit/:eventId` loads detail by route param and shows loading, error, and success states.
- [x] AC-3: Header shows `Audit #id`, decision badge, return-list action, and SQL Hash copy action.
- [x] AC-4: Identity panel shows created time, user, project/env, tool, operation, risk, sqlHash, requestId, client,
  sourceIp, affectedRows, and confirmationId.
- [x] AC-5: SQL text is shown in a read-only lightweight code viewer.
- [x] AC-6: Failure panel shows status, errorCode, failureReason/errorMessage, and resultSummary.
- [x] AC-7: Timeline reconstructs request received, authorization, classification, policy decision, and audit persisted.
- [x] AC-8: Production audit detail frontend code does not define or render Token plaintext/hash, database password,
  complete JDBC URL, password hash, or Token prefix fields.
- [x] AC-9: `pnpm --dir dbflow-admin build` passes.

## Risk Notes

| Risk                                                           | Likelihood | Mitigation                                                                                   |
|----------------------------------------------------------------|------------|----------------------------------------------------------------------------------------------|
| Timeline implies backend events that are not separately stored | Medium     | Label timeline as reconstructed from persisted audit detail fields and keep wording factual. |
| Detail page accidentally expands sensitive fields              | Low        | Type only `AuditEventDetail` sanitized DTO fields and run targeted sensitive-field grep.     |
| Monaco integration increases build/config risk                 | Medium     | Use lightweight `<pre>` viewer now and leave Monaco for a separate task.                     |

## Implementation Steps

### Step 1: Add focused frontend tests

**Files:** `dbflow-admin/src/api/audit.test.ts`, `dbflow-admin/src/features/audit/detail/audit-detail-page.test.tsx`
**Verification:** Targeted Vitest run fails for missing detail API/page modules before implementation.

Status: ✅ Completed
Evidence:

- Red run:
  `pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/audit.test.ts src/features/audit/detail/audit-detail-page.test.tsx`
  failed before implementation because `fetchAuditEventDetail` and `./index` were missing.
  Deviations:

### Step 2: Add detail API and type contract

**Files:** `dbflow-admin/src/api/audit.ts`, `dbflow-admin/src/types/audit.ts`
**Verification:** API test passes for `/audit-events/{id}` and sensitive-field absence.

Status: ✅ Completed
Evidence:

- Added `AuditEventDetail`, `auditEventDetailQueryKey`, and `fetchAuditEventDetail`.
- Green run:
  `pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/audit.test.ts src/features/audit/detail/audit-detail-page.test.tsx`
  passed 2 files / 5 tests.
  Deviations:

### Step 3: Implement shared SQL viewer and detail panels

**Files:** SQL viewer and audit detail components
**Verification:** Component/page tests pass for header, identity fields, SQL viewer, failure panel, timeline, copy
action, and sensitive-field absence.

Status: ✅ Completed
Evidence:

- Added `SqlCodeViewer`, identity, SQL, failure, and timeline panels.
- Green run:
  `pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/audit.test.ts src/features/audit/detail/audit-detail-page.test.tsx`
  passed 2 files / 5 tests.
  Deviations:

### Step 4: Wire route to real detail page

**Files:** `dbflow-admin/src/routes/_authenticated/audit.$eventId.tsx`, `dbflow-admin/src/routeTree.gen.ts`
**Verification:** `pnpm --dir dbflow-admin build` passes and route tree remains valid.

Status: ✅ Completed
Evidence:

- Replaced the existing `/audit/$eventId` placeholder route with `AuditDetailPage` using `Route.useParams()`.
- `pnpm --dir dbflow-admin build` passed, including generated route tree validation.
  Deviations:

### Step 5: Verify and archive

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** Run targeted frontend tests, frontend build, Maven tests, Harness validator, `git diff --check`, and
sensitive-field grep; then archive plan.

Status: ✅ Completed
Evidence:

-
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/api/audit.test.ts src/features/audit/detail/audit-detail-page.test.tsx`
passed 2 files / 5 tests.
- `pnpm --dir dbflow-admin build` passed.
- `./mvnw test` passed 228 tests with 0 failures, 0 errors, and 10 skipped.
- Sensitive-field grep over production audit detail frontend files returned no matches.
  Deviations:

## Progress Log

| Step | Status | Evidence                                                     | Notes                                                                                            |
|------|--------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| 1    | ✅      | Red Vitest failed on missing detail API/page modules         | Test-first coverage established for the new detail contract.                                     |
| 2    | ✅      | Green Vitest passed 2 files / 5 tests                        | Detail API helper and sanitized type contract added.                                             |
| 3    | ✅      | Green Vitest passed 2 files / 5 tests                        | Panels render header, identity, SQL, failure, timeline, copy action, and sensitive-text absence. |
| 4    | ✅      | `pnpm --dir dbflow-admin build` passed                       | `/audit/$eventId` route wired to the real page.                                                  |
| 5    | ✅      | Frontend tests/build, Maven tests, and sensitive grep passed | Plan ready for archive and final Harness verify.                                                 |

## Decision Log

| Decision                             | Context                                                                   | Alternatives Considered       | Rationale                                                                                       |
|--------------------------------------|---------------------------------------------------------------------------|-------------------------------|-------------------------------------------------------------------------------------------------|
| Use lightweight `<pre>` SQL viewer   | User allowed lightweight viewer if Monaco adds bundle/config complexity.  | Add Monaco now.               | Keeps this feature focused and avoids adding editor build complexity.                           |
| Map failure reason to `errorMessage` | Backend detail DTO has `errorMessage`, not `failureReason`.               | Add frontend-only fake field. | Preserves API truth and displays the user-requested concept using the existing sanitized field. |
| Reconstruct timeline in UI           | Backend detail endpoint returns one persisted event, not a step timeline. | Add backend timeline API.     | Avoids backend scope change while satisfying investigation-oriented display.                    |

## Completion Summary

Implemented the React `/audit/:eventId` audit detail page backed by `/admin/api/audit-events/{id}`. The page now
provides a read-only investigation view with `Audit #id`, decision badge, return-list action, SQL Hash copy,
identity/context metadata, lightweight SQL code viewer, failure/rejection details, and a reconstructed timeline.
Production frontend detail files define only sanitized audit DTO fields and do not include Token plaintext/hash,
database password, full JDBC URL, password hash, or Token prefix fields.
