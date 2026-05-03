# 2026-05-03 React Admin `/admin` Cutover

## Objective

Promote the accepted React admin SPA from `/admin-next` to the canonical `/admin` entry while keeping the existing
Thymeleaf admin available for short-term troubleshooting under `/admin-legacy`.

## Scope

- Change the Vite production base path from `/admin-next/` to `/admin/`.
- Copy the React build output into Spring Boot static resources under `static/admin/`.
- Serve React SPA fallback for `/admin` and `/admin/**`, excluding `/admin/api/**` and static resource misses.
- Move legacy Thymeleaf page and form routes from `/admin/**` to `/admin-legacy/**`.
- Keep `/admin/api/**` as authenticated JSON API routes and permit `/admin/assets/**` static resources.
- Update tests and docs that describe the active admin entry and legacy transition path.

## Non-Scope

- Do not delete the legacy Thymeleaf templates or `/admin-assets/**`.
- Do not change MCP, Actuator, SQL policy, or admin JSON API behavior.
- Do not redesign React admin pages or add new user-facing features.

## Assumptions

- `/admin-next` React admin has already passed product acceptance.
- Scheme A is selected: preserve the legacy Thymeleaf admin temporarily under `/admin-legacy`.
- Login success redirect remains `/admin` and should now land in the React SPA.

## Acceptance Criteria

- [x] `/admin` forwards to React `index.html`.
- [x] `/admin/users` forwards to React `index.html`.
- [x] `/admin/api/session` remains a JSON API and is not captured by SPA fallback.
- [x] `/admin-legacy` remains accessible to `ROLE_ADMIN` users.
- [x] `/admin/assets/**` is permitted for packaged React static assets.
- [x] `/admin` is no longer mapped by Thymeleaf page controllers.
- [x] `./mvnw -Dtest=AdminSpaControllerTests,AdminSecurityTests test` passes.
- [x] `pnpm --dir dbflow-admin build` passes.
- [x] `./mvnw -Preact-admin -DskipTests package` passes.
- [x] `./mvnw test` passes or records an accepted Testcontainers/Docker skip.

## Plan

- [x] Step 1: Update the React build path contract in Vite and Maven.
- [x] Step 2: Switch Spring MVC SPA fallback and admin security routing to `/admin`.
- [x] Step 3: Move legacy Thymeleaf controller routes, templates, and page tests to `/admin-legacy`.
- [x] Step 4: Update admin cutover documentation.
- [x] Step 5: Run targeted, frontend, package, full backend, Harness, and whitespace verification.

## Evidence Log

- 2026-05-03: Preflight `python3 scripts/check_harness.py` passed before implementation.
- 2026-05-03: Context7 Vite docs confirmed `base` rewrites built JS, CSS, and HTML asset references for nested public
  paths.
- 2026-05-03: Updated `dbflow-admin/vite.config.ts` base to `/admin/`, Maven `react-admin` copy output to
  `target/classes/static/admin`, and the MockMvc fixture asset to `static/admin/assets/admin-spa-test.js`.
- 2026-05-03: Updated `AdminSpaController` to serve `/admin` and `/admin/**` from `static/admin`, reject accidental
  `/admin/api/**` fallback capture, and updated the admin security chain to keep `/admin/api/**`, `/admin/**`, and
  `/admin-legacy/**` admin-only while permitting `/admin/assets/**`.
- 2026-05-03: Moved `AdminHomeController` page/form mappings, legacy Thymeleaf links/actions, and page/controller
  regression tests to `/admin-legacy/**` while leaving `/admin/api/**` tests unchanged.
- 2026-05-03: Updated README, admin guide, deployment guide, and observability notes so `/admin` is the React entry and
  `/admin-legacy/**` is documented as a short-term troubleshooting route.
- 2026-05-03: `./mvnw -Dtest=AdminSpaControllerTests,AdminSecurityTests test` passed with 11 tests, 0 failures, 0
  errors, 0 skipped.
- 2026-05-03: `pnpm --dir dbflow-admin build` passed. Vite emitted only the existing large chunk warning.
- 2026-05-03: `./mvnw -Preact-admin -DskipTests package` passed and copied 135 React resources to
  `target/classes/static/admin`.
- 2026-05-03: Jar inspection confirmed `BOOT-INF/classes/static/admin/index.html` and
  `BOOT-INF/classes/static/admin/assets/...` are present.
- 2026-05-03: `./mvnw test` passed with 229 tests, 0 failures, 0 errors, 10 Docker/Testcontainers skips.
- 2026-05-03: `python3 scripts/check_harness.py` passed with 13 manifest artifacts.
- 2026-05-03: `git diff --check` passed.
