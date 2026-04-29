# Execution Plan: SQL Classifier

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement an auditable SQL parsing and risk classification service for DBFlow using JSQLParser.

## Sprint Contract

**In scope:**

- Add the JSQLParser dependency.
- Implement `SqlClassifier` under `com.refinex.dbflow.sqlpolicy`.
- Return classification fields: `statementType`, `operation`, `riskLevel`, `targetSchema`, `targetTable`, `isDdl`,
  `isDml`, and `parseStatus`.
- Reject multi-statement inputs by classification result.
- Fail closed for DDL/DML parse failures.
- Explicitly classify readable statements such as `SELECT`, `SHOW`, `DESCRIBE`, and `EXPLAIN`.
- Add tests for MySQL 8 and MySQL 5.7 common syntax, including required operation coverage.

**Out of scope:**

- SQL execution, JDBC calls, EXPLAIN execution, and result-set handling.
- Authorization, whitelist policy decisions, confirmation challenge generation, and audit persistence writes.
- Full SQL firewall completeness beyond this first classification boundary.

**Constraints:**

- Dangerous SQL must not be allowed because parsing failed.
- Classification result must be suitable for later audit records.
- Multi-statement execution is denied by default.
- Keep implementation inside the existing single-module Maven project and `sqlpolicy` package.

**Assumptions:**

- JSQLParser 5.3 is acceptable because Maven Central reports it as current release/latest, and Context7 confirms the
  `CCJSqlParserUtil.parse()` / `parseStatements()` API shape.
- For parse failures, the classifier may use a conservative first-token fallback only to decide whether the input is a
  known readable command or an unsafe DDL/DML/administrative command.

## Acceptance Criteria

- [x] AC-1: `pom.xml` includes JSQLParser with a documented version property/dependency.
- [x] AC-2: `SqlClassifier` returns auditable immutable classification data with all required fields.
- [x] AC-3: Multi-statement input is rejected by default.
- [x] AC-4: DDL/DML parse failures fail closed.
- [x] AC-5: `SELECT`, `SHOW`, `DESCRIBE`, and `EXPLAIN` are explicitly classified.
- [x] AC-6: Tests cover `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `CREATE`, `ALTER`, `DROP`, `TRUNCATE`, `GRANT`,
  and `LOAD DATA`.
- [x] AC-7: MySQL 8 and MySQL 5.7 common syntax examples are represented in tests.
- [x] AC-8: `docs/ARCHITECTURE.md` and `docs/OBSERVABILITY.md` reflect the implemented SQL policy baseline.
- [x] AC-9: `./mvnw test` and `python3 scripts/check_harness.py` pass.

## Risk Notes

| Risk                                               | Likelihood | Mitigation                                                                                       |
|----------------------------------------------------|------------|--------------------------------------------------------------------------------------------------|
| Parser grammar gaps classify dangerous SQL as safe | Medium     | Fail closed for unparsed DDL/DML/admin commands and keep parse status explicit.                  |
| Multi-statement bypass through semicolon handling  | Medium     | Use `parseStatements()` before single-statement classification and reject size greater than one. |
| Classification leaks raw SQL in logs or docs       | Low        | Result includes operation, targets, status, and reason, not raw SQL text.                        |

## Implementation Steps

### Step 1: Add classification tests

**Files:** `src/test/java/com/refinex/dbflow/sqlpolicy/SqlClassifierTests.java`
**Verification:** Targeted test fails before classifier and dependency exist.

Status: ✅ Completed
Evidence:

- RED: `./mvnw -Dtest=SqlClassifierTests test` failed because `SqlClassifier`, `SqlOperation`, and `SqlRiskLevel`
  did not exist.
  Deviations:

### Step 2: Add dependency and classifier model

**Files:** `pom.xml`, `src/main/java/com/refinex/dbflow/sqlpolicy/`
**Verification:** Tests compile and required result fields are available.

Status: ✅ Completed
Evidence:

- Added JSQLParser 5.3 as a documented Maven dependency.
- Added immutable classification model records/enums under `com.refinex.dbflow.sqlpolicy`.
- `./mvnw -Dtest=SqlClassifierTests test` passed: 7 tests, 0 failures, 0 errors.
  Deviations:

### Step 3: Implement parsing and risk classification

**Files:** `src/main/java/com/refinex/dbflow/sqlpolicy/`
**Verification:** Targeted classifier tests pass.

Status: ✅ Completed
Evidence:

- Implemented `SqlClassifier` with single-statement parsing, conservative fallback classification, target extraction,
  and default rejection for multi-statement and failed DDL/DML/admin parsing.
- `./mvnw -Dtest=SqlClassifierTests test` passed: 7 tests, 0 failures, 0 errors.
  Deviations:

### Step 4: Update docs and archive

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, `docs/exec-plans/`
**Verification:** Full test suite and Harness validation pass.

Status: ✅ Completed
Evidence:

- Updated `docs/ARCHITECTURE.md` with real `sqlpolicy` source paths, dependency direction, and classification behavior.
- Updated `docs/OBSERVABILITY.md` with JSQLParser baseline and `SqlClassifierTests` coverage.
- `./mvnw test` passed: 71 tests, 0 failures, 0 errors.
- `python3 scripts/check_harness.py` passed: 14 manifest artifacts, all checks passed.
- `git diff --check` passed.
  Deviations:

## Progress Log

| Step | Status | Evidence                                                  | Notes |
|------|--------|-----------------------------------------------------------|-------|
| 1    | ✅      | RED compile failure for missing classifier/model classes. |       |
| 2    | ✅      | Targeted classifier tests passed.                         |       |
| 3    | ✅      | Targeted classifier tests passed.                         |       |
| 4    | ✅      | Full test suite and Harness validation passed.            |       |

## Decision Log

| Decision                     | Context                                                          | Alternatives Considered                             | Rationale                                                   |
|------------------------------|------------------------------------------------------------------|-----------------------------------------------------|-------------------------------------------------------------|
| Fail-closed parse fallback   | Parser coverage is useful but not a security boundary by itself. | Treat parser failure as unknown/low risk.           | DBFlow must not allow dangerous SQL because parsing failed. |
| Classification-only boundary | This stage precedes policy enforcement and execution.            | Implement whitelist and confirmation decisions now. | Keeps the phase small and auditable.                        |

## Completion Summary

Implemented JSQLParser-backed SQL parsing and risk classification under `com.refinex.dbflow.sqlpolicy`. The classifier
returns auditable immutable results with statement type, operation, risk level, target schema/table, DDL/DML flags,
parse status, default rejection state, and audit reason. Multi-statement input is rejected. Failed DDL/DML/admin parsing
fails closed, while readable commands remain explicitly classified with parse status retained for audit.

Final verification:

- `./mvnw test` passed: 71 tests, 0 failures, 0 errors.
- `python3 scripts/check_harness.py` passed: 14 manifest artifacts, all checks passed.
- `git diff --check` passed.
