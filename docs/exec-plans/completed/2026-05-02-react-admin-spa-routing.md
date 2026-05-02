# Execution Plan: React Admin SPA Routing

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Serve the React admin SPA routes under `/admin-next/**` from the Spring Boot production jar while preserving the
existing `/admin/**` Thymeleaf management UI and keeping `/admin/api/**` protected by the admin security chain.

## Scope

**In scope:**

- Add `AdminSpaController` for GET `/admin-next` and `/admin-next/**` SPA fallback.
- Avoid falling back to SPA `index.html` for actual static resource paths such as `.js`, `.css`, `.svg`, and `.png`.
- Update `AdminSecurityConfiguration` to include `/admin-next/**` in the management security chain.
- Permit anonymous access to React static assets and SPA shell routes so `/admin-next/login` can render in later stages.
- Keep `/admin/api/**` under `ROLE_ADMIN`.
- Add `AdminSpaControllerTests`.
- Update Harness plan/docs evidence.

**Out of scope:**

- Changing existing `/admin/**` Thymeleaf controllers or templates.
- Implementing React session APIs or login page logic.
- Changing the Maven React build profile.
- Adding production reverse-proxy configuration.

## Constraints

- Backend API security is authoritative: `/admin/api/**` must require `ROLE_ADMIN`.
- Existing `/admin` Thymeleaf overview must continue to render for admins.
- Static resources must never be converted into SPA fallback HTML; packaged classpath resources may be returned
  directly by the SPA controller when the catch-all route owns the request.

## Assumptions

- The React SPA shell should be anonymously fetchable; protected UI actions will rely on backend session/API checks.
- Static resource detection can be path-extension based for the packaged Vite output.

## Acceptance Criteria

- [x] AC-1: `AdminSpaController` handles GET `/admin-next` and `/admin-next/**`.
- [x] AC-2: Non-resource SPA paths forward to `/admin-next/index.html`.
- [x] AC-3: Resource-looking paths such as `.js`, `.css`, `.svg`, and `.png` do not fallback to the SPA index.
- [x] AC-4: `/admin-next/**` participates in the admin security filter chain.
- [x] AC-5: `/admin/api/**` still requires `ROLE_ADMIN`.
- [x] AC-6: Existing `/admin` Thymeleaf overview still renders for admins.
- [x] AC-7: `./mvnw -Dtest=AdminSpaControllerTests,AdminUiControllerTests test` passes.

## Implementation Steps

### Step 1: Add red SPA routing tests

**Files:** `src/test/java/com/refinex/dbflow/admin/AdminSpaControllerTests.java`
**Verification:** targeted test fails before implementation

Status: ✅ Completed
Evidence: `./mvnw -Dtest=AdminSpaControllerTests,AdminUiControllerTests test` initially failed because
`/admin-next` had no forward and `/admin-next/users` returned 404.

### Step 2: Implement SPA fallback controller and security rules

**Files:** `src/main/java/com/refinex/dbflow/admin/controller/AdminSpaController.java`,
`src/main/java/com/refinex/dbflow/security/configuration/AdminSecurityConfiguration.java`
**Verification:** targeted test passes

Status: ✅ Completed
Evidence: Added `AdminSpaController`; updated `AdminSecurityConfiguration` to match `/admin-next/**`, permit React
static assets and SPA shell, and keep `/admin/api/**` administrator-only.

### Step 3: Verify scope and archive

**Files:** docs and repository state
**Verification:** targeted tests, Harness check, diff hygiene, optional full Maven test if needed

Status: ✅ Completed
Evidence: Targeted Maven test passed: 11 tests, 0 failures, 0 errors, 0 skipped. `python3 scripts/check_harness.py`
passed. `git diff --check` passed.

## Progress Log

| Step      | Status | Evidence                                                                                    | Notes                                                                                                        |
|-----------|--------|---------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| Preflight | ✅      | Context7 Spring Security/Spring MVC docs; `python3 scripts/check_harness.py`; `./mvnw test` | Harness passed; baseline Maven test passed 191 tests, 0 failures, 0 errors, 10 skipped                       |
| 1         | ✅      | Red targeted test failed before implementation                                              | `/admin-next/users` returned 404; `/admin-next` had no SPA forward                                           |
| 2         | ✅      | Added controller/security updates                                                           | `/admin-next/**` is in the admin chain; `/admin/api/**` remains `ROLE_ADMIN`; SPA shell routes are anonymous |
| 3         | ✅      | Targeted Maven test; `python3 scripts/check_harness.py`; `git diff --check`                 | 11 tests, 0 failures, 0 errors, 0 skipped; Harness passed; diff hygiene passed                               |

## Decision Log

| Decision                             | Context                                                                  | Alternatives Considered                        | Rationale                                                                                                |
|--------------------------------------|--------------------------------------------------------------------------|------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| Permit SPA shell routes              | `/admin-next/login` may need anonymous access and React handles shell UX | Require server auth for every `/admin-next/**` | Lets the SPA boot while `/admin/api/**` remains the authoritative protected surface                      |
| Keep static resources in security    | Spring Security docs recommend `permitAll` over ignoring                 | `web.ignoring()` for assets                    | Retains security headers while avoiding auth prompts for assets                                          |
| Serve extension-based resource paths | The controller catch-all owns `/admin-next/**` requests                  | Let resource handler own all assets            | Prevents asset URLs from falling to SPA HTML; existing packaged assets are still returned from classpath |
