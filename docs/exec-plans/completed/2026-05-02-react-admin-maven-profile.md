# Execution Plan: React Admin Maven Profile

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Add an optional Maven profile named `react-admin` that builds the React admin app and packages its Vite `dist` output
under Spring Boot static resources at `/static/admin-next/`, without requiring Node or pnpm for the default Maven test
path.

## Scope

**In scope:**

- Add a root Maven `react-admin` profile.
- Run `pnpm --dir dbflow-admin install --frozen-lockfile` when the profile is active.
- Run `pnpm --dir dbflow-admin build` when the profile is active.
- Copy `dbflow-admin/dist/**` into `target/classes/static/admin-next/` before jar packaging.
- Update observability/build documentation for the optional package path.
- Verify default `./mvnw test` remains backend-only.
- Verify `./mvnw -Preact-admin -DskipTests package` includes React admin assets in the jar.

**Out of scope:**

- Installing Node or pnpm through Maven for machines that do not activate the profile.
- Writing React build output into `src/main/resources`.
- Changing backend controller, Spring Security, or static routing behavior.
- Changing the React app itself.

## Constraints

- Default Maven lifecycle must not fail on machines without Node or pnpm.
- The profile may require local pnpm because the user requested explicit `pnpm --dir ...` commands.
- Keep generated frontend assets under `target/` so source directories remain clean.
- Keep the existing single-module Maven layout.

## Assumptions

- The profile is intended for release/package builds in environments where pnpm is available.
- Copying into `${project.build.outputDirectory}/static/admin-next` during `prepare-package` is acceptable because the
  jar goal packages `target/classes` afterward.

## Acceptance Criteria

- [x] AC-1: Root `pom.xml` defines a Maven profile with id `react-admin`.
- [x] AC-2: The profile invokes `pnpm --dir dbflow-admin install --frozen-lockfile`.
- [x] AC-3: The profile invokes `pnpm --dir dbflow-admin build`.
- [x] AC-4: React admin `dist` files are copied into `target/classes/static/admin-next/`.
- [x] AC-5: Default `./mvnw test` does not execute frontend build and passes.
- [x] AC-6: `./mvnw -Preact-admin -DskipTests package` builds frontend assets and packages them into the jar.
- [x] AC-7: Source static resources are not polluted with generated React build output.

## Implementation Steps

### Step 1: Add optional Maven profile

**Files:** `pom.xml`
**Verification:** `./mvnw help:active-profiles`; source inspection for profile executions

Status: ✅ Completed
Evidence: `pom.xml` now contains `react-admin`, `react-admin-pnpm-install`, `react-admin-pnpm-build`, and
`copy-react-admin-dist` executions.

### Step 2: Package generated React admin assets

**Files:** `pom.xml`
**Verification:** `./mvnw -Preact-admin -DskipTests package`; `jar tf target/refinex-dbflow-0.1.0-SNAPSHOT.jar`

Status: ✅ Completed
Evidence: `./mvnw -Preact-admin -DskipTests package` executed the pnpm install/build commands, copied 95 resources from
`dbflow-admin/dist` to `target/classes/static/admin-next`, and built the Spring Boot jar successfully.

### Step 3: Update control-plane documentation and verify default path

**Files:** `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `./mvnw test`; Harness check; diff hygiene

Status: ✅ Completed
Evidence: `./mvnw test`; `python3 scripts/check_harness.py`; `git diff --check`; `test ! -e
src/main/resources/static/admin-next`.

## Progress Log

| Step      | Status | Evidence                                                                         | Notes                                                                                                             |
|-----------|--------|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Preflight | ✅      | `python3 scripts/check_harness.py`; `./mvnw test`; Context7 frontend plugin docs | Harness passed; default Maven baseline passed 191 tests, 0 failures, 0 errors, 10 skipped; profile remains opt-in |
| 1         | ✅      | `pom.xml` source inspection                                                      | Added opt-in `react-admin` profile with pnpm install/build executions                                             |
| 2         | ✅      | `./mvnw -Preact-admin -DskipTests package`; `jar tf ...`                         | Jar contains `BOOT-INF/classes/static/admin-next/index.html` and built asset files                                |
| 3         | ✅      | `./mvnw test`; `python3 scripts/check_harness.py`; source static absence check   | Default Maven test path remains backend-only; generated React assets stay under `target/`                         |

## Decision Log

| Decision                           | Context                                                      | Alternatives Considered                          | Rationale                                                                           |
|------------------------------------|--------------------------------------------------------------|--------------------------------------------------|-------------------------------------------------------------------------------------|
| Use `exec-maven-plugin` for pnpm   | User requested exact `pnpm --dir dbflow-admin ...` commands  | `frontend-maven-plugin` with working directory   | Preserves the requested command shape and keeps the profile simple                  |
| Copy into `target/classes`         | User preferred generated target output over source resources | Copy into `src/main/resources/static/admin-next` | Avoids generated asset churn in source-controlled resource directories              |
| Bind frontend work only in profile | Default `./mvnw test` must not require Node or pnpm          | Add frontend build to default package lifecycle  | Keeps local backend tests reliable on machines without frontend toolchain installed |

## Completion Summary

Added an optional Maven `react-admin` profile that runs the requested pnpm install/build commands, copies the Vite
output into `target/classes/static/admin-next`, and packages those assets into the Spring Boot jar. The default Maven
test path remains backend-only and does not require Node or pnpm.
