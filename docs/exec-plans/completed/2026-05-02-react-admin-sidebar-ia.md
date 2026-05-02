# Execution Plan: React Admin Sidebar IA

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Replace the imported template sidebar data with the DBFlow Admin information architecture so the React shell no longer
shows demo navigation.

## Scope

**In scope:**

- Update `dbflow-admin/src/components/layout/data/sidebar-data.ts`.
- Replace the placeholder user, team list, and navigation groups with DBFlow Admin labels and lucide-react icons.
- Verify the React admin build and absence of demo navigation strings.
- Update `docs/PLANS.md` and this execution plan.

**Out of scope:**

- Creating the target pages for `/users`, `/grants`, `/tokens`, `/config`, `/policies/dangerous`, `/audit`, or
  `/health`.
- Wiring the user placeholder to a session API.
- Changing Spring Boot backend code, Maven configuration, or Thymeleaf templates.

## Constraints

- Preserve reusable shell components and the existing temporary dashboard.
- Keep future-route sidebar entries as plain path strings; the existing sidebar data type allows string URLs.
- Do not change `pom.xml`, `src/main/java/**`, or `src/main/resources/templates/**`.

## Acceptance Criteria

- [x] AC-1: Sidebar user placeholder is `admin` / `DBFlow Administrator`.
- [x] AC-2: Sidebar teams contain only `DBFlow Admin` with `MCP SQL Gateway`.
- [x] AC-3: Sidebar navigation groups are 工作台、身份与访问、配置与策略、审计、运维、设置 with the requested entries.
- [x] AC-4: Sidebar data no longer contains demo navigation labels such as General, Pages, Other, Auth, Errors, Clerk,
  or demo settings children.
- [x] AC-5: `pnpm --dir dbflow-admin build` completes successfully.
- [x] AC-6: `pom.xml`, `src/main/java/**`, and `src/main/resources/templates/**` are not modified.

## Implementation Steps

### Step 1: Replace sidebar data

**Files:** `dbflow-admin/src/components/layout/data/sidebar-data.ts`
**Verification:** source inspection for expected DBFlow labels and absence of template labels

Status: ✅ Completed
Evidence: `dbflow-admin/src/components/layout/data/sidebar-data.ts` now contains the requested DBFlow user, single
team, navigation groups, routes, and lucide-react icons; demo navigation keyword search returned no matches.

### Step 2: Verify build and scope

**Files:** repository state
**Verification:** frontend build, Harness check, diff hygiene, protected-path diff checks

Status: ✅ Completed
Evidence: `pnpm --dir dbflow-admin build`; sidebar expected-string search; sidebar demo-string absence search;
restricted backend path diff checks.

## Progress Log

| Step      | Status | Evidence                                                                           | Notes                                                                                                         |
|-----------|--------|------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Preflight | ✅      | `python3 scripts/check_harness.py`; `pnpm --dir dbflow-admin build`; `./mvnw test` | Harness and frontend build baseline passed; Maven baseline passed 191 tests, 0 failures, 0 errors, 10 skipped |
| 1         | ✅      | Sidebar expected-string search and demo-string absence search                      | DBFlow Admin navigation data replaced the imported template sidebar groups                                    |
| 2         | ✅      | `pnpm --dir dbflow-admin build`; restricted backend path diff checks               | Frontend build passed; protected backend and Thymeleaf paths were not modified                                |

## Decision Log

| Decision                      | Context                                                               | Alternatives Considered                     | Rationale                                                                |
|-------------------------------|-----------------------------------------------------------------------|---------------------------------------------|--------------------------------------------------------------------------|
| Keep target pages for later   | User requested replacing sidebar data, not building each page         | Add placeholder routes for every nav target | Avoid expanding this prompt beyond information architecture replacement  |
| Use DBFlow product navigation | The imported demo sidebar still exposed template pages and auth demos | Retain error/auth demo links in sidebar     | Sidebar should describe DBFlow Admin work areas, while routes can remain |

## Completion Summary

Replaced the imported template sidebar data with DBFlow Admin information architecture: placeholder admin user, single
DBFlow Admin team, requested DBFlow work-area groups, and lucide-react icons. Verified the React build and confirmed the
sidebar data no longer contains the prior demo navigation labels.
