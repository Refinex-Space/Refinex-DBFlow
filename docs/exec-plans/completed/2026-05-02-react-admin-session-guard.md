# Execution Plan: React Admin Session Guard

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

React SPA uses `/admin/api/session` as the source of truth for login state and protects DBFlow authenticated routes
without storing login state in localStorage.

## Scope

**In scope:**

- `dbflow-admin/src/api/`
- `dbflow-admin/src/types/`
- `dbflow-admin/src/stores/`
- `dbflow-admin/src/components/auth/`
- `dbflow-admin/src/routes/`

**Out of scope:**

- Spring Boot backend endpoint changes.
- Thymeleaf `/admin/**` behavior.
- Full DBFlow users/grants/tokens page implementation.
- Replacing the JSON login form behavior beyond route-level login-state handling.

## Constraints

- Session state must come from the server cookie and `/admin/api/session`.
- 401 responses must clear the React session store without a global hard redirect.
- Authenticated pages should show a loading skeleton while the session check is pending.
- `/admin-next/**` production subpath behavior must remain compatible with existing Vite/TanStack Router setup.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/src/api/session.ts`, `dbflow-admin/src/types/session.ts`, and
  `dbflow-admin/src/stores/session-store.ts` exist and model `username`, `displayName`, `roles`, and `shell`.
- [x] AC-2: Authenticated routes call `/admin/api/session`, clear session on 401, and redirect anonymous users to
  `/login?redirect=<current>`.
- [x] AC-3: Visiting `/login` while authenticated redirects to `/`.
- [x] AC-4: Protected route pending state renders a loading skeleton before page content or route errors.
- [x] AC-5: `pnpm --dir dbflow-admin build` passes.

## Risk Notes

| Risk                                                                               | Likelihood | Mitigation                                                                           |
|------------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------|
| Existing template `/sign-in` route differs from required `/login` path.            | Medium     | Add `/login` as a DBFlow route while leaving `/sign-in` untouched for compatibility. |
| `/users` was previously pruned, so unauthenticated `/users` could 404 before auth. | Medium     | Add a minimal protected `/users` placeholder route only to exercise the guard.       |
| Session checks could duplicate requests during concurrent route loads.             | Low        | Deduplicate in-flight `loadSession()` requests inside the store.                     |

## Implementation Steps

### Step 1: Add session API types and store

**Files:** `dbflow-admin/src/types/session.ts`, `dbflow-admin/src/api/session.ts`,
`dbflow-admin/src/stores/session-store.ts`
**Verification:** Focused store tests cover authenticated load and 401 clearing.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin exec vitest run src/stores/session-store.test.ts --browser.headless` passed 3 tests.
Deviations:

### Step 2: Add route guard and login handling

**Files:** `dbflow-admin/src/components/auth/require-auth.tsx`, `dbflow-admin/src/routes/_authenticated/route.tsx`,
`dbflow-admin/src/routes/(auth)/login.tsx`, `dbflow-admin/src/routes/_authenticated/users.tsx`
**Verification:** Build route generation succeeds and route tree contains `/login` and `/users`.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin exec vite build` regenerated `routeTree.gen.ts`; route tree contains `/login` and
`/users`.
Deviations:

### Step 3: Verify and archive

**Files:** `docs/PLANS.md`, active/completed plan files
**Verification:** `pnpm --dir dbflow-admin build`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin build` passed; Playwright smoke with mocked 401 session redirected
`/admin-next/users` to `/admin-next/login?redirect=%2Fusers`; `python3 scripts/check_harness.py`, `git diff --check`,
and `./mvnw test` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                              | Notes                                                           |
|------|--------|-----------------------------------------------------------------------|-----------------------------------------------------------------|
| 1    | ✅      | Store test passed 3/3.                                                | 401 clears session; concurrent load deduped.                    |
| 2    | ✅      | Route tree grep shows `/login` and `/users`; build produced chunks.   | Added minimal protected users placeholder.                      |
| 3    | ✅      | Frontend build, browser smoke, Harness, diff, and Maven tests passed. | Maven test result: 228 tests, 0 failures, 0 errors, 10 skipped. |

## Decision Log

| Decision                                                   | Context                                                                                                      | Alternatives Considered                                           | Rationale                                                                                                          |
|------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| Add `/login` while preserving `/sign-in`.                  | Requirement names `/login`; template currently exposes `/sign-in`.                                           | Rename `/sign-in`; add redirect; add alias route.                 | Alias route is smallest and avoids breaking existing template links during migration.                              |
| Add minimal protected `/users` placeholder.                | Acceptance checks unauthenticated `/admin-next/users`.                                                       | Leave missing route and rely on 404; rebuild full users page now. | A placeholder proves auth guard behavior without expanding into the future user-management feature.                |
| Configure TanStack Router `basepath` from Vite `BASE_URL`. | Browser smoke showed `/admin-next/users` was interpreted as an unknown route before router basepath was set. | Keep Vite-only `base`; hard-code `/admin-next`.                   | Context7 documents `createRouter({ basepath })` for subpath routing; deriving it from Vite keeps dev/prod aligned. |

## Completion Summary

Completed: 2026-05-02
Duration: 3 steps
All acceptance criteria: PASS

Summary: Added server-session-backed React admin auth state, protected `_authenticated` routes with TanStack Router
`beforeLoad`, introduced `/login` and a minimal protected `/users` placeholder for guard verification, configured router
`basepath` for `/admin-next/`, and verified with frontend tests, build, browser smoke, Harness, diff, and Maven tests.
