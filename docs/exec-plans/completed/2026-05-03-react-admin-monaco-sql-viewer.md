# Execution Plan: React Admin Monaco SQL Viewer

Created: 2026-05-03
Status: Completed
Author: agent

## Objective

Upgrade the React audit detail SQL viewer from a lightweight `<pre>` renderer to a read-only Monaco Editor surface for
SQL inspection.

## Scope

**In scope:**

- Add the Monaco React dependency required by `dbflow-admin`.
- Update `dbflow-admin/src/components/dbflow/sql-code-viewer.tsx`.
- Add any Vite/worker wiring required for reliable production builds.
- Add focused frontend coverage for readonly SQL viewer behavior.
- Verify the audit detail build path.

**Out of scope:**

- SQL editing, execution, submit, save, or replay actions.
- Monaco-based formatting, autocomplete, linting, or custom DBFlow SQL language services.
- Backend audit API changes.
- Broad audit detail page layout changes.

## Constraints

- Monaco must be read-only and must not expose any SQL mutation action.
- Language should be `sql`.
- Default height is `220px`; long SQL should scroll inside the editor.
- Theme must follow the DBFlow admin dark/light mode.
- Keep the existing shadcn Card shell and DBFlow visual density.
- Preserve `/admin-next/` build compatibility.

## Assumptions

- `@monaco-editor/react` is acceptable because it is the requested option and Context7 documents direct Vite usage.
- If Vite build succeeds without explicit worker wiring, no extra worker config is required; if it fails or emits
  missing-worker errors, configure Monaco workers explicitly.
- Existing theme state can be read from the existing `useTheme()` context because the app already exposes
  `resolvedTheme`.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin` declares the Monaco dependency and lockfile is updated.
- [x] AC-2: `SqlCodeViewer` renders Monaco Editor with `language='sql'`.
- [x] AC-3: Editor is read-only and disables editing affordances such as minimap and context editing actions where
  appropriate.
- [x] AC-4: Viewer defaults to `220px` height and supports internal scrolling for long SQL.
- [x] AC-5: Viewer follows light/dark theme.
- [x] AC-6: No SQL submit/edit action is introduced.
- [x] AC-7: `pnpm --dir dbflow-admin build` passes.

## Implementation Steps

### Step 1: Add focused tests

**Files:** `dbflow-admin/src/components/dbflow/sql-code-viewer.test.tsx`
**Verification:** Targeted Vitest run fails before Monaco implementation or verifies the current `<pre>` implementation
is missing Monaco props.

Status: ✅ Completed
Evidence:

- Red run: `pnpm --dir dbflow-admin exec vitest run --browser.headless src/components/dbflow/sql-code-viewer.test.tsx`
  failed because the existing `<pre>` implementation did not render a Monaco SQL viewer.
  Deviations:

### Step 2: Add Monaco dependency and bundler support

**Files:** `dbflow-admin/package.json`, `dbflow-admin/pnpm-lock.yaml`, optionally `dbflow-admin/vite.config.ts`
**Verification:** Dependency install succeeds and TypeScript can resolve `@monaco-editor/react`.

Status: ✅ Completed
Evidence:

- `pnpm --dir dbflow-admin add @monaco-editor/react` installed `@monaco-editor/react 4.7.0`.
- `pnpm --dir dbflow-admin add monaco-editor` installed explicit `monaco-editor 0.55.1` required by local ESM worker
  configuration.
- Added local Monaco loader/worker setup in `dbflow-admin/src/lib/monaco.ts` and optimized Vite dependency pre-bundling
  entries.
  Deviations:
- Initial build with `import * as monaco from 'monaco-editor'` worked but pulled unnecessary language workers. The
  implementation was narrowed to `editor.api.js` plus SQL contribution and editor worker.

### Step 3: Upgrade SQL viewer implementation

**Files:** `dbflow-admin/src/components/dbflow/sql-code-viewer.tsx`
**Verification:** Component test verifies SQL language, readonly options, default height, and theme mapping.

Status: ✅ Completed
Evidence:

- `SqlCodeViewer` now renders `@monaco-editor/react` with `language='sql'`, `readOnly`, `domReadOnly`, disabled context
  menu, disabled minimap, automatic layout, word wrap, and default `height='220px'`.
-
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/components/dbflow/sql-code-viewer.test.tsx src/features/audit/detail/audit-detail-page.test.tsx`
passed 2 files / 5 tests.
Deviations:

### Step 4: Build and archive

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** Run targeted tests, `pnpm --dir dbflow-admin build`, Harness validator, `git diff --check`, and
inspect diff.

Status: ✅ Completed
Evidence:

-
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/components/dbflow/sql-code-viewer.test.tsx src/features/audit/detail/audit-detail-page.test.tsx`
passed 2 files / 5 tests.
- `pnpm --dir dbflow-admin build` passed.
- `./mvnw test` passed 228 tests with 0 failures, 0 errors, and 10 skipped.
  Deviations:
- Vite reports a non-failing chunk-size warning for the Monaco-backed audit detail chunk. The build exits 0 and the
  warning is expected for Monaco; future optimization can split/load Monaco separately if this becomes a performance
  target.

## Progress Log

| Step | Status | Evidence                                                | Notes                                                                |
|------|--------|---------------------------------------------------------|----------------------------------------------------------------------|
| 1    | ✅      | Red Vitest failed against `<pre>` implementation        | Locked expected Monaco behavior before implementation.               |
| 2    | ✅      | Dependencies installed and Vite optimized entries added | Local editor worker and SQL contribution are configured.             |
| 3    | ✅      | Component/detail tests passed 2 files / 5 tests         | Viewer is readonly, SQL-language, 220px by default, and theme-aware. |
| 4    | ✅      | Frontend build and Maven tests passed                   | Plan ready for archive and final Harness verify.                     |

## Decision Log

| Decision                   | Context                                                     | Alternatives Considered               | Rationale                                                                    |
|----------------------------|-------------------------------------------------------------|---------------------------------------|------------------------------------------------------------------------------|
| Use `@monaco-editor/react` | User allowed `@monaco-editor/react` or raw `monaco-editor`. | Manual Monaco lifecycle wiring.       | The React wrapper is documented for Vite usage and keeps this upgrade small. |
| Keep Card shell            | Existing DBFlow detail panels use shadcn Card.              | Make Monaco full-bleed or standalone. | The viewer is part of an audit detail panel, not a primary editor workspace. |
| No SQL mutation controls   | Audit detail is read-only.                                  | Add copy/run actions.                 | Preserves the audit review boundary and avoids implying executable SQL.      |

## Completion Summary

<!-- Fill in when archiving the plan -->
Upgraded the audit detail SQL viewer from a lightweight `<pre>` renderer to a read-only Monaco Editor. The viewer uses
SQL language mode, follows DBFlow light/dark theme through `useTheme()`, defaults to `220px`, supports internal
scrolling for long SQL, disables edit-oriented affordances, and introduces no SQL submit/edit action. Vite now has local
Monaco ESM worker support and optimized dependency entries. Focused component/detail tests and production build passed.
