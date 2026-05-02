# Execution Plan: React Admin Session Shell Layout

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Make the React admin shell render DBFlow session identity, shell status, logout, and DBFlow navigation instead of
imported template layout data.

## Scope

**In scope:**

- `dbflow-admin/src/components/layout/**`
- `dbflow-admin/src/components/profile-dropdown.tsx`
- `dbflow-admin/src/components/sign-out-dialog.tsx`
- `dbflow-admin/src/components/command-menu.tsx`
- `dbflow-admin/src/components/search.tsx`
- shared DBFlow route metadata under `dbflow-admin/src/lib/`

**Out of scope:**

- Implementing real DBFlow data pages beyond current placeholder routes.
- Changing Spring Boot backend security/API behavior.
- Removing upstream attribution from README/NOTICE.

## Constraints

- Preserve light/dark theme support and existing shadcn/ui layout primitives.
- Session state must come from the server-backed session store, not localStorage or static template users.
- Logout must use the existing JSON `/logout` client with CSRF support.
- Visible template brand text must not remain in React source UI.

## Acceptance Criteria

- [ ] AC-1: Sidebar user and profile menu render the current session display name/username with DBFlow fallback text.
- [ ] AC-2: Header area shows MCP status, config source, ThemeSwitch, and ProfileDropdown.
- [ ] AC-3: Command palette lists DBFlow pages: overview, users, grants, tokens, config, dangerous policy, audit,
  health.
- [ ] AC-4: Team switcher no longer exposes demo team/add-team UI and acts as DBFlow brand selector.
- [ ] AC-5: `pnpm --dir dbflow-admin build` passes.
- [ ] AC-6: Visible React source no longer contains `satnaing`, `Acme`, or `Shadcn Admin` outside README/NOTICE.

## Risk Notes

| Risk                                                     | Likelihood | Mitigation                                                                                        |
|----------------------------------------------------------|------------|---------------------------------------------------------------------------------------------------|
| Router path typing rejects shared string route metadata. | Medium     | Keep route URLs aligned with existing `LinkProps['to']` shape and validate with TypeScript build. |
| Logout dialog async behavior races with navigation.      | Low        | Await JSON logout, clear session store, then replace-route to `/login`.                           |
| Header status is duplicated or too visually noisy.       | Low        | Add compact shell metadata inside the existing search/action cluster.                             |

## Implementation Steps

### Step 1: Centralize DBFlow Route Metadata

**Files:** `dbflow-admin/src/lib/routes.ts`, `dbflow-admin/src/components/layout/data/sidebar-data.ts`,
`dbflow-admin/src/components/command-menu.tsx`
**Verification:** DBFlow route names are reused by sidebar and command palette tests/build.

Status: ✅ Done
Evidence: Added shared `dbflowRouteGroups` / `dbflowRoutes` metadata and wired sidebar data plus command palette to it.
Deviations:

### Step 2: Bind Layout Identity And Logout To Session

**Files:** `dbflow-admin/src/components/layout/app-sidebar.tsx`, `dbflow-admin/src/components/layout/nav-user.tsx`,
`dbflow-admin/src/components/layout/team-switcher.tsx`, `dbflow-admin/src/components/profile-dropdown.tsx`,
`dbflow-admin/src/components/sign-out-dialog.tsx`
**Verification:** TypeScript build and source grep confirm no static template identity is visible.

Status: ✅ Done
Evidence: Sidebar user now derives from `useSessionStore`; team switcher and sidebar/profile menus no longer expose demo
team/pro/billing text; logout calls JSON `/logout`.
Deviations:

### Step 3: Show Shell Status In Header/Search

**Files:** `dbflow-admin/src/components/search.tsx`
**Verification:** Build passes and status/config source render from session shell with safe fallbacks.

Status: ✅ Done
Evidence: Search/header action cluster now renders compact MCP status badge and config source from session shell.
Deviations:

## Progress Log

| Step | Status | Evidence                                                              | Notes                                   |
|------|--------|-----------------------------------------------------------------------|-----------------------------------------|
| 1    | ✅      | Shared route metadata drives sidebar and command palette.             | Targeted command palette test passed.   |
| 2    | ✅      | Session-derived sidebar/profile identity and JSON logout added.       | Targeted sign-out test passed.          |
| 3    | ✅      | MCP status/config source added to the existing header action cluster. | `pnpm --dir dbflow-admin build` passed. |

## Decision Log

| Decision                                                            | Context                                            | Alternatives Considered | Rationale                                                    |
|---------------------------------------------------------------------|----------------------------------------------------|-------------------------|--------------------------------------------------------------|
| Use one shared `dbflowRoutes` list for sidebar and command palette. | Sidebar and search have identical route semantics. | Keep duplicated arrays. | Single metadata source reduces drift across layout surfaces. |

## Completion Summary

Completed: 2026-05-02
Duration: 3 implementation steps
All acceptance criteria: PASS

Summary: React admin layout now reads session identity from `useSessionStore`, renders DBFlow shell status/config source
in the header action cluster, uses DBFlow-only sidebar/profile/team UI, calls JSON `/logout` for sign-out, centralizes
DBFlow route metadata for sidebar and command palette, and passes frontend/backend/Harness verification.
