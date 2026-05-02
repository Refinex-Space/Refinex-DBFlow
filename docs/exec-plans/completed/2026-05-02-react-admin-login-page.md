# Execution Plan: React Admin Login Page

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Replace the imported template sign-in demo on `/login` with a DBFlow-specific React + shadcn/ui login page that uses the
Spring Security JSON login protocol and server-session state.

## Scope

**In scope:**

- `dbflow-admin/src/api/session.ts`
- `dbflow-admin/src/features/auth/login-page.tsx`
- `dbflow-admin/src/routes/(auth)/login.tsx`
- Focused frontend tests and route/API smoke verification.

**Out of scope:**

- Backend login endpoint changes.
- Removing legacy template `/sign-in` routes.
- Rebuilding non-login authenticated pages.
- Adding new UI dependency families such as Aceternity or Magic UI.

## Frontend Working Model

- Surface type: admin login page for internal DBFlow operators.
- Visual thesis: technical and utilitarian, with warm-neutral control-plane polish and a restrained database-governance
  signal.
- Content plan: left governance story with `MCP SQL Gateway`, `Database Operation Governance`, and `Audit Ready`; right
  shadcn login card.
- Interaction thesis: password visibility toggle, loading button state, toast feedback, and no decorative motion beyond
  subtle hover/focus states.

## Constraints

- Use existing shadcn/ui Card, Input, Button, and Form components.
- Use lucide-react icons: `Database`, `Shield`, `KeyRound`.
- Login request must POST `/login` with `Accept: application/json`, CSRF header, and form-urlencoded
  `username/password`.
- Successful login must store the returned session in memory and navigate to `redirect` or `/`.
- Failed login must show an error toast and keep the user on `/login`.
- Login state must not be persisted in localStorage.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/src/api/session.ts` exposes `login(username, password)` and `logout()` using the correct
  Spring Security JSON login protocol.
- [x] AC-2: `/login` renders a DBFlow-specific login page, not the template sign-in demo or social-login content.
- [x] AC-3: Login page shows DBFlow governance copy and lucide `Database`, `Shield`, and `KeyRound` icons.
- [x] AC-4: Login failure displays a visible error toast.
- [x] AC-5: Login success stores session state and navigates to `redirect` or `/`, so Dashboard becomes accessible.
- [x] AC-6: `pnpm --dir dbflow-admin build` passes.

## Risk Notes

| Risk                                                                             | Likelihood | Mitigation                                                                                           |
|----------------------------------------------------------------------------------|------------|------------------------------------------------------------------------------------------------------|
| Axios `/admin/api` base URL could accidentally send login to `/admin/api/login`. | Medium     | Use a separate root-relative auth client for `/login` and `/logout`.                                 |
| Visual redesign could drift into marketing-page composition.                     | Low        | Keep first viewport as an admin login work surface with one login card and dense governance summary. |
| Toast tests could assert implementation details.                                 | Low        | Test user-visible failure behavior and navigation/session effects, not mock component internals.     |

## Implementation Steps

### Step 1: Extend session API

**Files:** `dbflow-admin/src/api/session.ts`, focused API test
**Verification:** API test proves login sends form-urlencoded body, JSON Accept, and CSRF header; logout posts with
CSRF.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin exec vitest run src/api/session.test.ts --browser.headless` passed 3 tests.
Deviations:

### Step 2: Build DBFlow login page

**Files:** `dbflow-admin/src/features/auth/login-page.tsx`, `dbflow-admin/src/routes/(auth)/login.tsx`
**Verification:** Component test proves validation, failure toast path, and success redirect/session update.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin exec vitest run src/features/auth/login-page.test.tsx --browser.headless` passed 4
tests.
Deviations:

### Step 3: Verify and archive

**Files:** `docs/PLANS.md`, active/completed plan files
**Verification:** Frontend build, browser smoke, Harness validation, diff check, and Maven test pass.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin build` passed; Playwright smoke confirmed failed login toast, CSRF header, successful
login redirect to `/admin-next/`, and Dashboard visibility; `python3 scripts/check_harness.py`, `git diff --check`, and
`./mvnw test` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                              | Notes                                                                                                     |
|------|--------|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| 1    | ✅      | Session API test passed 3/3.                                                          | Login uses root-relative `/login`, form-urlencoded body, JSON Accept, XHR header, and CSRF.               |
| 2    | ✅      | Login page test passed 4/4.                                                           | Replaced `/login` template demo with DBFlow-specific page; fixed password field label wiring during test. |
| 3    | ✅      | Frontend targeted tests, build, browser smoke, Harness, diff, and Maven tests passed. | Maven result: 228 tests, 0 failures, 0 errors, 10 skipped.                                                |

## Decision Log

| Decision                                | Context                                                                                                | Alternatives Considered                          | Rationale                                                                           |
|-----------------------------------------|--------------------------------------------------------------------------------------------------------|--------------------------------------------------|-------------------------------------------------------------------------------------|
| Use form-urlencoded login.              | Backend `AdminJsonLoginTests` covers `username/password` form params with JSON Accept and CSRF header. | JSON body login.                                 | Form login aligns with Spring Security and existing tests.                          |
| Keep `/sign-in` legacy route untouched. | Existing template routes still compile and may be referenced by demo pages.                            | Delete or redirect all template auth routes now. | Current acceptance targets `/login`; broader cleanup belongs to a later prune step. |

## Completion Summary

Completed: 2026-05-02
Duration: 3 steps
All acceptance criteria: PASS

Summary: Replaced `/login` with a DBFlow-specific React login surface, added Spring Security form-urlencoded JSON
login/logout helpers with CSRF support, verified failed-login toast and successful-login Dashboard access, and kept
legacy `/sign-in` routes untouched for later cleanup.
