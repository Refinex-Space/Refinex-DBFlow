# Execution Plan: React Admin Demo Prune

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Remove DBFlow-irrelevant imported demo pages from the React admin shell while preserving reusable infrastructure
components, error pages, auth/settings scaffolding, and a minimal DBFlow temporary dashboard at the authenticated index
route.

## Scope

**In scope:**

- Delete imported demo feature modules for apps, chats, tasks, help center, and users.
- Delete corresponding authenticated routes for apps, chats, tasks, help center, and demo users.
- Replace the imported analytics dashboard with a minimal DBFlow Admin dashboard.
- Remove deleted demo navigation links from sidebar data.
- Regenerate generated route tree through the Vite build.
- Update `docs/PLANS.md` and this execution plan.

**Out of scope:**

- Removing Clerk/auth demo routes or dependencies.
- Removing settings pages.
- Implementing real DBFlow users, projects, tokens, audits, policies, or SQL operation views.
- Changing Spring Boot backend code, Maven configuration, or Thymeleaf templates.

## Constraints

- Preserve reusable component directories listed by the user: `components/ui`, `components/data-table`,
  `components/layout`, `search.tsx`, `theme-switch.tsx`, `routes/(errors)`, `context`, `hooks/use-table-url-state.ts`,
  and `lib`.
- Keep the temporary dashboard utility-first and restrained: internal admin tool, dense enough to orient, no marketing
  hero.
- Do not change `pom.xml`, `src/main/java/**`, or `src/main/resources/templates/**`.
- TDD does not fit this deletion/scaffold task; verification is build, route/search inspection, and diff-scope based.

## Assumptions

- The user intends the whole imported demo users feature to be removed, not only its seed data, because the demo users
  route is removed and DBFlow users will be rebuilt later.
- Settings and auth pages remain because they were not listed for deletion and are useful shell scaffolding for later
  stages.
- `routeTree.gen.ts` may change as a generated artifact when deleted routes are removed.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/src/features/apps/**`, `chats/**`, `help-center/**`, `tasks/**`, and demo `users/**` no longer
  exist.
- [x] AC-2: Authenticated routes for `apps`, `chats`, `help-center`, `tasks`, and demo `users` no longer exist.
- [x] AC-3: Preserved reusable directories/files still exist.
- [x] AC-4: `dbflow-admin/src/routes/_authenticated/index.tsx` still routes to `Dashboard`.
- [x] AC-5: `dbflow-admin/src/features/dashboard/index.tsx` displays `DBFlow Admin`, `MCP SQL Gateway`, and
  `React shell is ready`.
- [x] AC-6: Sidebar no longer links to deleted demo pages.
- [x] AC-7: `pnpm --dir dbflow-admin build` completes successfully.
- [x] AC-8: `pom.xml`, `src/main/java/**`, and `src/main/resources/templates/**` are not modified.

## Implementation Steps

### Step 1: Remove demo feature and route files

**Files:** demo feature directories and matching route files
**Verification:** `test ! -e` checks and `rg --files` inspection

Status: ✅ Completed
Evidence: `test ! -e ...` checks confirmed the listed demo feature and authenticated route paths are absent;
`rg --files dbflow-admin/src/features dbflow-admin/src/routes/_authenticated` shows only auth, dashboard, errors,
settings, and preserved route shell files.
Deviations: Removed `dbflow-admin/src/routes/clerk/_authenticated/**` as well because the Clerk user-management demo
depended on the deleted demo users feature.

### Step 2: Rebuild temporary DBFlow dashboard and navigation

**Files:** `dbflow-admin/src/features/dashboard/index.tsx`, `dbflow-admin/src/routes/_authenticated/index.tsx`,
`dbflow-admin/src/components/layout/data/sidebar-data.ts`
**Verification:** source inspection for required strings and absence of deleted navigation targets

Status: ✅ Completed
Evidence:
`rg 'DBFlow Admin|MCP SQL Gateway|React shell is ready' dbflow-admin/src/features/dashboard/index.tsx dbflow-admin/src/routes/_authenticated/index.tsx`;
sidebar data no longer contains `/apps`, `/chats`, `/help-center`, `/tasks`, or `/users`.
Deviations: Clerk sign-in/sign-up demo routes remain, but the deleted user-management fallback now redirects to `/`.

### Step 3: Build and scope verification

**Files:** repository state
**Verification:** `pnpm --dir dbflow-admin build`, `python3 scripts/check_harness.py`, `git diff --check`, restricted
backend path diff checks, Maven regression check

Status: ✅ Completed
Evidence: `pnpm --dir dbflow-admin build`; `python3 scripts/check_harness.py`; `./mvnw test`; Playwright text assertion
against `http://127.0.0.1:5173/`; `git diff --check`; restricted backend path diff checks.
Deviations: Ran `pnpm --dir dbflow-admin exec vite build` once before the formal package build to regenerate TanStack
`routeTree.gen.ts` after deleting file-based routes.

## Frontend Working Model

- **Surface type:** admin tool.
- **Audience:** internal DBFlow administrators and operators.
- **Visual thesis:** restrained, utility-first internal console that immediately confirms the React shell is ready for
  DBFlow work.
- **Content plan:** header controls remain; main surface shows product name, gateway subtitle, ready state, and a short
  shell status list.
- **Interaction thesis:** static first screen, no decorative motion, preserve existing shell controls for
  theme/search/profile.

## Progress Log

| Step      | Status | Evidence                                                                                       | Notes                                                                                                         |
|-----------|--------|------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Preflight | ✅      | `python3 scripts/check_harness.py`; `pnpm --dir dbflow-admin build`; `./mvnw test`             | Harness and frontend build baseline passed; Maven baseline passed 191 tests, 0 failures, 0 errors, 10 skipped |
| 1         | ✅      | `test ! -e ...`; `rg --files dbflow-admin/src/features dbflow-admin/src/routes/_authenticated` | Demo feature and route paths removed                                                                          |
| 2         | ✅      | Source search for required dashboard strings; sidebar route search                             | Temporary DBFlow dashboard and navigation cleanup completed                                                   |
| 3         | ✅      | `pnpm --dir dbflow-admin build`; Playwright text assertion; Harness/Maven/diff checks          | `/` renders the temporary Dashboard text through Vite                                                         |

## Decision Log

| Decision                                | Context                                                                                     | Alternatives Considered                           | Rationale                                                                                       |
|-----------------------------------------|---------------------------------------------------------------------------------------------|---------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Remove the whole demo users feature     | User asked to remove demo users route and data, and said DBFlow users will be rebuilt later | Keep unused users components after deleting route | Avoid retaining page-specific demo forms, fixtures, and tests that no longer have a route       |
| Keep auth/settings scaffolding          | User did not list these for deletion                                                        | Delete all non-dashboard pages                    | Keep potentially reusable shell/auth/settings structure for later migration stages              |
| Remove Clerk user-management demo route | That route imported the deleted demo users feature and duplicated the users demo page       | Keep users feature solely for Clerk demo          | Preserves Clerk sign-in/sign-up demo while removing the DBFlow-irrelevant users demo dependency |

## Completion Summary

Removed imported apps, chats, help-center, tasks, and users demo pages/routes, rebuilt the authenticated index as a
minimal DBFlow Admin temporary dashboard, cleaned sidebar/search test references to deleted routes, regenerated the
TanStack route tree, and verified build, runtime text, Harness, Maven, and diff-scope checks.
