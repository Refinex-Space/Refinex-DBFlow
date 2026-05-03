# 2026-05-03 Remove Legacy Thymeleaf Admin

## Objective

Remove the short-lived legacy Thymeleaf admin after the React `/admin` cutover so DBFlow has one maintained
management UI.

## Scope

- Delete the legacy Thymeleaf admin controller and templates.
- Delete `/admin-assets/**` files that were only used by the Thymeleaf admin.
- Serve `GET /login` from the React SPA while keeping Spring Security `POST /login` and `/logout` behavior.
- Remove or rewrite tests that only verified legacy SSR pages and forms.
- Update current README, admin guide, deployment guide, observability notes, and architecture inventory.

## Non-Scope

- Do not change `/admin/api/**`, MCP, Actuator, SQL policy, audit, or React page behavior.
- Do not delete historical completed execution plans or prototype/spec documents.
- Do not remove service/view model classes still used by JSON APIs or session shell metadata.

## Assumptions

- React `/admin` is accepted and is now the only maintained management UI.
- React has a `/login` route and can render from the same SPA `index.html`.
- Legacy Thymeleaf route removal should make `/admin-legacy/**` unavailable instead of protected.

## Acceptance Criteria

- [x] `src/main/resources/templates/admin/**` is deleted.
- [x] `src/main/resources/static/admin-assets/**` is deleted.
- [x] `AdminHomeController` is deleted.
- [x] `GET /login` serves the React SPA index and still issues the CSRF cookie.
- [x] `POST /login` JSON/form security behavior still passes existing tests.
- [x] `/admin-legacy/**` no longer exposes the old后台.
- [x] `/admin/api/**`, MCP, and Actuator tests are not regressed.
- [x] `./mvnw test` passes or records accepted Docker/Testcontainers skips.
- [x] `pnpm --dir dbflow-admin build` passes.
- [x] `./mvnw -Preact-admin -DskipTests package` passes.

## Plan

- [x] Step 1: Remove legacy controller, templates, and static assets.
- [x] Step 2: Route React login SPA shell and tighten admin security matchers.
- [x] Step 3: Delete or rewrite SSR-only tests.
- [x] Step 4: Update current documentation and Harness plan index.
- [x] Step 5: Run targeted and full verification, then archive the plan.

## Evidence Log

- 2026-05-03: Preflight `python3 scripts/check_harness.py` passed with 13 manifest artifacts.
- 2026-05-03: Deleted `AdminHomeController`, `src/main/resources/templates/admin/**`, and
  `src/main/resources/static/admin-assets/**`.
- 2026-05-03: Added `GET /login` handling in `AdminSpaController` to serve React `index.html`, removed
  `/admin-legacy/**` and `/admin-assets/**` from the admin security chain, and kept `POST /login`/`/logout` under
  Spring Security.
- 2026-05-03: Deleted SSR-only page controller tests, rewrote SPA/security/CSRF/health assertions around React
  `/login`, unavailable `/admin-legacy`, and `/admin/api/health`.
- 2026-05-03: Updated current README, admin guide, deployment guide, observability notes, architecture inventory, and
  troubleshooting command references to remove `/admin-legacy`, `templates/admin`, and `/admin-assets/**` as active
  surfaces.
- 2026-05-03: Removed the unused `spring-boot-starter-thymeleaf` dependency and updated admin view package docs to
  describe React JSON API projections.
- 2026-05-03:
  `./mvnw -Dtest=AdminSpaControllerTests,AdminSecurityTests,AdminJsonLoginTests,AdminCsrfSpaTests,OperationalHealthAndMetricsTests test`
  passed with 23 tests, 0 failures, 0 errors, 0 skipped.
- 2026-05-03: `pnpm --dir dbflow-admin build` passed; Vite reported only the existing large chunk warning.
- 2026-05-03: `./mvnw -Preact-admin -DskipTests package` passed and copied React assets to `static/admin/`.
- 2026-05-03: `./mvnw test` passed with 204 tests, 0 failures, 0 errors, and 10 accepted skips.
- 2026-05-03: Legacy reference scan found no current references to `templates/admin`, `admin-assets`,
  `AdminHomeController`, SSR-only tests, or `spring-boot-starter-thymeleaf`.
- 2026-05-03: Legacy resource directory scan found no files under `src/main/resources/templates` or
  `src/main/resources/static/admin-assets`.
- 2026-05-03: Jar content scan confirmed `BOOT-INF/classes/static/admin/index.html` and
  `BOOT-INF/classes/static/admin/assets/**` exist, with no `templates/admin` or `static/admin-assets` matches.
