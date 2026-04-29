# Execution Plan: Metadata Flyway Schema

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Create the P02.1 Flyway-managed metadata database schema and prove it migrates cleanly in tests.

## Scope

**In scope:**

- Flyway, Spring Data JPA, MySQL Flyway support, and H2 local/test database dependencies.
- V1 metadata migration for users, API tokens, projects, environments, grants, confirmation challenges, and audit
  events.
- Constraints and indexes for unique active token, unique user-environment grant, and common audit queries.
- Migration tests covering table existence, sensitive token column absence, uniqueness behavior, and key audit indexes.
- Architecture and observability documentation updates.

**Out of scope:**

- JPA entity/repository implementation.
- Token generation, hashing service, authentication, authorization, audit write service, and MCP tool behavior.
- Testcontainers MySQL; H2 MySQL mode is used for this stage's migration proof.

## Constraints

- Do not store token plaintext; only token hash, prefix, status, expiry, revocation, and last-used metadata may be
  modeled.
- Audit events must not store full result sets; only SQL text/hash, result summary, affected rows, and error fields are
  allowed.
- Keep implementation focused on schema and migration verification.
- Follow Chinese comment and JavaDoc standards from `docs/references/java-development-standards.md`.

## Acceptance Criteria

- [x] AC-1: `./mvnw test` passes.
- [x] AC-2: V1 migration creates all seven required metadata tables.
- [x] AC-3: Migration tests prove token plaintext is absent and active token uniqueness is enforced.
- [x] AC-4: Migration tests prove grant uniqueness is enforced.
- [x] AC-5: Migration tests cover audit query indexes and key schema indexes/constraints.

## Risk Notes

| Risk                                                       | Likelihood | Mitigation                                                                                                      |
|------------------------------------------------------------|------------|-----------------------------------------------------------------------------------------------------------------|
| H2 MySQL mode differs from real MySQL syntax.              | Medium     | Keep DDL conservative and retain `flyway-mysql` for future MySQL/Testcontainers validation.                     |
| Unique active token constraint is awkward in portable SQL. | Medium     | Use nullable `active_flag` plus check constraint and unique `(user_id, active_flag)`; inactive rows use `NULL`. |
| Adding JPA/Flyway changes application startup behavior.    | Medium     | Configure local H2 metadata datasource so the scaffold can still start and tests can run migrations.            |

## Implementation Steps

### Step 1: Add persistence dependencies and datasource baseline

**Files:** `pom.xml`, `src/main/resources/application.yml`
**Verification:** `./mvnw test`

Status: ✅ Done
Evidence: Added `spring-boot-starter-data-jpa`, `flyway-core`, `flyway-mysql`, and H2 runtime dependency; configured
local H2 MySQL mode datasource and Flyway location in `application.yml`.
Deviations:

### Step 2: Create V1 metadata migration

**Files:** `src/main/resources/db/migration/V1__create_metadata_schema.sql`
**Verification:** Flyway migration test

Status: ✅ Done
Evidence: Added `src/main/resources/db/migration/V1__create_metadata_schema.sql` with seven metadata tables, token
constraints, grant constraints, and audit indexes.
Deviations:

### Step 3: Add migration tests

**Files:** `src/test/java/com/refinex/dbflow/config/MetadataSchemaMigrationTests.java`
**Verification:** `./mvnw test`

Status: ✅ Done
Evidence: Added `MetadataSchemaMigrationTests` covering core table existence, token plaintext absence, active token
uniqueness, grant uniqueness, audit indexes, and named constraints.
Deviations:

### Step 4: Update control-plane documentation

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`
**Verification:** `python3 scripts/check_harness.py`

Status: ✅ Done
Evidence: Updated `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, and `docs/PLANS.md` for metadata schema and test
coverage.
Deviations:

### Step 5: Final verification

**Files:** Repository root
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, `git diff --check`

Status: ✅ Done
Evidence: `./mvnw test` exited 0 with Tests run: 11, Failures: 0, Errors: 0, Skipped: 0.
`python3 scripts/check_harness.py` exited 0. `git diff --check` exited 0.
Deviations:

## Progress Log

| Step | Status | Evidence                                                    | Notes |
|------|--------|-------------------------------------------------------------|-------|
| 1    | ✅      | Persistence dependencies and H2/Flyway baseline configured. |       |
| 2    | ✅      | V1 migration added for all required metadata tables.        |       |
| 3    | ✅      | Migration tests cover required schema proof points.         |       |
| 4    | ✅      | Architecture and observability docs updated.                |       |
| 5    | ✅      | Maven test, Harness validator, and diff check passed.       |       |

## Decision Log

| Decision                                        | Context                                      | Alternatives Considered | Rationale                                                                                                               |
|-------------------------------------------------|----------------------------------------------|-------------------------|-------------------------------------------------------------------------------------------------------------------------|
| Use H2 in MySQL mode for P02.1 migration tests. | The task permits H2 or Testcontainers MySQL. | Testcontainers MySQL.   | Keeps this schema phase fast and not dependent on Docker; future compatibility can be strengthened with Testcontainers. |

## Completion Summary

Completed: 2026-04-29
Duration: 5 steps
All acceptance criteria: PASS

Summary: Added the P02.1 Flyway metadata schema, persistence dependencies, local H2 metadata datasource baseline,
migration tests for core tables and constraints, and updated control-plane documentation.
