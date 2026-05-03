# Execution Plan: React Admin Config Page

Created: 2026-05-03
Completed: 2026-05-03
Status: Completed
Author: agent

## Objective

Implement the DBFlow React admin `/config` read-only configuration page backed by the existing sanitized
`GET /admin/api/config` JSON API.

## Scope

**In scope:**

- `dbflow-admin/src/api/config.ts`
- `dbflow-admin/src/types/config.ts`
- `dbflow-admin/src/features/config/index.tsx`
- `dbflow-admin/src/features/config/components/config-table.tsx`
- `dbflow-admin/src/features/config/components/config-detail-sheet.tsx`
- `dbflow-admin/src/routes/_authenticated/config.tsx`
- Generated TanStack route tree update for `/config`
- Focused frontend tests for API wiring, config table rendering, manual refresh, detail Sheet, empty state, and
  sensitive-field absence.

**Out of scope:**

- Changing Spring Boot config API contracts.
- Changing Thymeleaf `/admin/config`.
- Editing database/Nacos/project configuration.
- Rendering full JDBC URLs or any password fields.

## Constraints

- Use the unified API client and existing `GET /admin/api/config` endpoint.
- Keep the page read-only.
- Use TanStack Query `refetch()` for the refresh button.
- `ConfigRow` frontend type must not define `password` or `jdbcUrl`.
- Detail Sheet must render only sanitized fields already present in `ConfigRow`.
- Empty `rows` must show `当前未配置 dbflow.projects。`.

## Assumptions

- Backend `ConfigRow` is the sanitized source of truth: `project`, `projectName`, `env`, `envName`, `datasource`,
  `type`, `host`, `port`, `schema`, `username`, `limits`, and `syncStatus`.
- `sourceLabel` is always safe display text from backend config source metadata.
- Unknown/blank fields render through existing formatting fallback.

## Acceptance Criteria

- [x] AC-1: `fetchConfigPage()` reads `GET /admin/api/config`.
- [x] AC-2: `/config` displays `sourceLabel`.
- [x] AC-3: Config table renders project, projectName, env, envName, datasource, type, host, port, schema, username,
  limits, and syncStatus.
- [x] AC-4: Refresh button manually refetches the config query and exposes loading state.
- [x] AC-5: Clicking a row opens a detail Sheet for that sanitized row.
- [x] AC-6: Empty rows render `当前未配置 dbflow.projects。`.
- [x] AC-7: Page source and types do not define `password` or `jdbcUrl`.
- [x] AC-8: Page does not display full JDBC URLs.
- [x] AC-9: `pnpm --dir dbflow-admin build` passes.

## Implementation Steps

### Step 1: Add config API and type contract

**Files:** `dbflow-admin/src/api/config.ts`, `dbflow-admin/src/types/config.ts`
**Verification:** focused API test proves `/config` path and safe data shape without `jdbcUrl` or `password`.

Status: ✅ Done
Evidence: RED test failed because `./config` did not exist; GREEN targeted API test passed with `/config` path and
safe-shape assertions.
Deviations:

### Step 2: Build config page, route, table, and detail Sheet

**Files:** `dbflow-admin/src/features/config/**`, `dbflow-admin/src/routes/_authenticated/config.tsx`,
`dbflow-admin/src/routeTree.gen.ts`
**Verification:** focused browser tests cover source label, all table fields, refresh, row-click detail Sheet, empty
state, and sensitive-data absence.

Status: ✅ Done
Evidence: Targeted browser tests passed for source label, table fields, refresh refetch, row-click detail Sheet,
empty state, and sensitive-data absence.
Deviations:

### Step 3: Verify build, sensitive boundary, and control plane

**Files:** `docs/PLANS.md`, this execution plan
**Verification:** targeted frontend tests, full frontend tests/build, Maven tests when relevant, Harness validator,
sensitive grep, and diff hygiene.

Status: ✅ Done
Evidence: React build, full frontend tests, Maven tests, sensitive grep, Harness validator, and diff hygiene passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                                               | Notes                          |
|------|--------|--------------------------------------------------------------------------------------------------------|--------------------------------|
| 1    | ✅      | `pnpm --dir dbflow-admin test src/api/config.test.ts src/features/config/config-page.test.tsx` passed. | Config API client added.       |
| 2    | ✅      | Browser tests covered table, refresh, detail Sheet, empty state, and sensitive absence.                | `/config` route source added.  |
| 3    | ✅      | Build, full frontend tests, Maven tests, sensitive grep, Harness validator, and diff check passed.     | Route tree includes `/config`. |

## Decision Log

| Decision                                | Context                                                                      | Alternatives Considered                | Rationale                                                                                                |
|-----------------------------------------|------------------------------------------------------------------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------|
| Use backend `ConfigRow` shape directly. | Backend API already returns sanitized host/port/schema/datasource summaries. | Parse or transform JDBC data in React. | React should not receive or derive full JDBC URLs; using sanitized fields preserves the safety boundary. |

## Completion Summary

Implemented the React `/config` read-only configuration page for DBFlow Admin. The page loads sanitized configuration
data from `/admin/api/config`, displays the backend `sourceLabel`, renders all required table fields, supports manual
refresh through TanStack Query `refetch()`, opens a detail Sheet from row clicks, and renders the empty state
`当前未配置 dbflow.projects。` when no configured environments exist.

Verification passed:

- `pnpm --dir dbflow-admin test src/api/config.test.ts src/features/config/config-page.test.tsx` with 2 files and 5
  tests passed
- `pnpm --dir dbflow-admin build`
- `pnpm --dir dbflow-admin test` with 33 files and 162 tests passed
- `./mvnw test` with 228 tests, 0 failures, 0 errors, and 10 skipped
- `python3 scripts/check_harness.py`
- production config source sensitive grep for
  `password|Password|jdbcUrl|jdbc-url|jdbc:mysql|jdbc:h2|tokenHash|plaintextToken`
  returned no matches
- `git diff --check`
