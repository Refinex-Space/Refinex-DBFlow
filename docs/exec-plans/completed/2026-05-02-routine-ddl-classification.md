# Execution Plan: Routine DDL Classification

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Classify `CREATE FUNCTION` and `CREATE PROCEDURE` as supported routine DDL instead of collapsing them into the generic
`CREATE 解析失败，默认拒绝` path when JSQLParser already parses them successfully.

## Sprint Contract

**In scope:**

- Add classifier coverage for JSQLParser routine DDL nodes.
- Preserve fail-closed behavior for unsupported or truly unparsable `CREATE` inputs.
- Add regression tests for routine DDL classification.

**Out of scope:**

- Expanding SQL execution semantics beyond existing DDL handling.
- Introducing new SQL operation enums or policy engines.
- Changing dangerous DDL whitelist behavior.

**Constraints:**

- Existing fail-closed behavior must remain intact for parse failures.
- The smallest change should fix the reported `CREATE FUNCTION` rejection path.
- Keep the change local to `sqlpolicy` and its tests unless a follow-up doc update becomes necessary.

**Assumptions:**

- JSQLParser 5.3 exposes routine DDL as `CreateFunction` and `CreateProcedure` statements.
- The supported behavior only needs to classify the routine name and surface it as DDL metadata.

## Acceptance Criteria

- [ ] AC-1: `SqlClassifier` classifies simple `CREATE FUNCTION` input as `DDL` with `operation=CREATE` and
  `parseStatus=SUCCESS`.
- [ ] AC-2: `SqlClassifier` classifies simple `CREATE PROCEDURE` input the same way.
- [ ] AC-3: Routine DDL target extraction preserves the routine name for audit metadata.
- [ ] AC-4: Existing DDL/DML fail-closed behavior and tests still pass.
- [ ] AC-5: `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass after the change.

## Progress Log

| Step | Status    | Evidence                                                              | Notes                                                        |
|------|-----------|-----------------------------------------------------------------------|--------------------------------------------------------------|
| 1    | completed | `./mvnw -Dtest=SqlClassifierTests test`                               | Added routine DDL classifier branches and helper extraction. |
| 2    | completed | `./mvnw test`, `python3 scripts/check_harness.py`, `git diff --check` | Added regression tests and verified the suite.               |

## Completion Notes

This plan is archived to `docs/exec-plans/completed/` after verification.
