# Execution Plan: React Admin Integration Verification

Created: 2026-05-03
Status: Completed
Author: agent

## Objective

完整运行 React admin 集成验证命令矩阵，修复真实失败项，并证明 packaged jar 同时包含 `/admin-next` 资产且保留 `/admin`
Thymeleaf 管理端。

## Scope

**In scope:**

- `dbflow-admin/` dependency install, lint, build, and any required lint/style fixes.
- Spring Boot test and React admin Maven profile package verification.
- Jar content inspection for `static/admin-next/index.html` and built assets.
- Existing backend tests or package evidence that proves `/admin` Thymeleaf remains reachable.
- Harness plan/index evidence updates.

**Out of scope:**

- E2E browser testing against a live server unless command evidence reveals it is necessary.
- Broad React admin refactors unrelated to failing lint/build/package checks.
- Disabling lint rules to bypass template or style failures.
- Changing backend security, session, CSRF, or admin route semantics without a real failure.

## Constraints

- Prefer fixing code over disabling lint rules.
- Docker/Testcontainers skip in `./mvnw test` is acceptable only when reported as skip, not failure.
- Preserve existing uncommitted React admin test/documentation work in the worktree.
- Use `harness-verify` before claiming completion.

## Acceptance Criteria

- [x] AC-1: `pnpm --dir dbflow-admin install --frozen-lockfile` exits 0.
- [x] AC-2: `pnpm --dir dbflow-admin lint` exits 0, with code fixes for any real lint failures.
- [x] AC-3: `pnpm --dir dbflow-admin build` exits 0.
- [x] AC-4: `./mvnw test` exits 0; Docker/Testcontainers skips, if present, are recorded as acceptable.
- [x] AC-5: `./mvnw -Preact-admin -DskipTests package` exits 0.
- [x] AC-6: Packaged jar contains `BOOT-INF/classes/static/admin-next/index.html` and at least one
  `BOOT-INF/classes/static/admin-next/assets/` entry.
- [x] AC-7: Current `/admin` Thymeleaf reachability remains covered by backend regression evidence.
- [x] AC-8: `python3 scripts/check_harness.py` and `git diff --check` exit 0.

## Risk Notes

| Risk                                                             | Likelihood | Mitigation                                                                             |
|------------------------------------------------------------------|------------|----------------------------------------------------------------------------------------|
| Lint exposes many template-era style issues                      | Medium     | Fix focused failures in touched/relevant files first; avoid disabling rules.           |
| React admin package profile mutates built assets under `target/` | High       | Treat `target/` as generated verification output; do not include it in source summary. |
| Maven tests skip Docker-backed Testcontainers                    | Medium     | Record skip count and distinguish from failures/errors.                                |

## Implementation Steps

### Step 1: Run frontend dependency and lint/build checks

**Files:** `dbflow-admin/**` if fixes are required.
**Verification:** install, lint, and build commands exit 0.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin install --frozen-lockfile` exited 0; `pnpm --dir dbflow-admin lint` exited 0 after
fixing 7 lint errors; `pnpm --dir dbflow-admin build` exited 0; `pnpm --dir dbflow-admin test` passed 44 files / 194
tests.
Deviations: Lint still reports 7 existing Fast Refresh warnings for route/template export shape, but exit code is 0 and
no rule was disabled.

### Step 2: Run backend and packaging checks

**Files:** backend or packaging files only if failures require fixes.
**Verification:** `./mvnw test` and `./mvnw -Preact-admin -DskipTests package` exit 0.

Status: ✅ Done
Evidence: `./mvnw test` exited 0 with 228 tests, 0 failures, 0 errors, 10 skipped;
`./mvnw -Preact-admin -DskipTests package` exited 0 and copied 135 React admin resources into
`target/classes/static/admin-next`.
Deviations: The 10 skipped tests include Docker/Testcontainers-dependent coverage and match the documented acceptable
behavior when Docker-backed tests are unavailable.

### Step 3: Inspect packaged jar and `/admin` regression evidence

**Files:** none expected.
**Verification:** jar listing includes React admin static assets; backend tests include `/admin` Thymeleaf regression
coverage.

Status: ✅ Done
Evidence:
`jar tf target/refinex-dbflow-0.1.0-SNAPSHOT.jar | rg "BOOT-INF/classes/static/admin-next/(index\\.html|assets/.+)"`
found `BOOT-INF/classes/static/admin-next/index.html` and multiple `BOOT-INF/classes/static/admin-next/assets/...`
entries; `./mvnw -Dtest=AdminUiControllerTests test` passed 6 tests, 0 failures, 0 errors, 0 skipped.
Deviations:

### Step 4: Run control-plane and diff hygiene checks

**Files:** `docs/exec-plans/active/2026-05-03-react-admin-integration-verification.md`, `docs/PLANS.md`.
**Verification:** `python3 scripts/check_harness.py` and `git diff --check` exit 0; plan is archived when complete.

Status: ✅ Done
Evidence: `python3 scripts/check_harness.py` exited 0; `git diff --check` exited 0 before archiving, and will be rerun
after archiving.
Deviations:

## Progress Log

| Step | Status | Evidence                                        | Notes                                                             |
|------|--------|-------------------------------------------------|-------------------------------------------------------------------|
| 1    | ✅      | install/lint/build/test passed.                 | Fixed React lint errors instead of disabling rules.               |
| 2    | ✅      | Maven test/package passed.                      | Testcontainers skip recorded as acceptable.                       |
| 3    | ✅      | Jar listing and AdminUiControllerTests passed.  | `/admin-next` packaged and `/admin` Thymeleaf regression covered. |
| 4    | ✅      | Harness and diff hygiene passed before archive. | Final rerun pending after archive.                                |

## Decision Log

| Decision                 | Context                                                                                   | Alternatives Considered                          | Rationale                                                                                                            |
|--------------------------|-------------------------------------------------------------------------------------------|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| 修复 lint error，不关闭规则      | React Hooks lint exposed setState-in-effect/static-components/no-duplicate-imports errors | Disable rules or ignore generated template style | User explicitly required code fixes first; the fixes remove synchronous prop-to-state effects and duplicate imports. |
| 保留 Fast Refresh warnings | Remaining lint output is warning-only and from route/template export shape                | Broadly split route files now                    | Not acceptance-blocking and would expand scope beyond integration failure repair.                                    |

## Completion Summary

Completed: 2026-05-03
Duration: 4 steps
All acceptance criteria: PASS

Summary: Ran the full React admin integration command matrix, fixed real lint failures, verified frontend build/tests,
backend tests, React admin profile packaging, jar static asset inclusion, `/admin` Thymeleaf regression coverage,
Harness validation, and diff hygiene.
