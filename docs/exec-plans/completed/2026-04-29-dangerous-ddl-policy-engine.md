# Execution Plan: Dangerous DDL Policy Engine

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement a YAML-backed `DangerousDdlPolicyEngine` that denies `DROP DATABASE` and `DROP TABLE` by default, supports
wildcard whitelist matching, and keeps each decision auditable.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/sqlpolicy`
- `src/main/java/com/refinex/dbflow/config/DbflowProperties.java`
- `src/test/java/com/refinex/dbflow/sqlpolicy`
- `docs/ARCHITECTURE.md`
- `docs/OBSERVABILITY.md`

**Out of scope:**

- SQL execution.
- Confirmation workflow implementation.
- Audit persistence integration; this stage only returns audit-ready decision data.

## Constraints

- Follow root `AGENTS.md` and Java development standards.
- Keep high-risk SQL policy server-side.
- `DROP DATABASE` and `DROP TABLE` default to deny.
- Prod environments require an explicit `allow-prod-dangerous-ddl=true` whitelist flag even when other whitelist fields
  match.
- Rejection decisions must include a machine-readable reason code and a human-readable reason.

## Acceptance Criteria

- [x] AC-1: `DangerousDdlPolicyEngine` denies `DROP DATABASE` and `DROP TABLE` when no whitelist entry matches.
- [x] AC-2: Matching YAML whitelist entries allow non-prod `DROP DATABASE` and `DROP_TABLE`.
- [x] AC-3: Prod whitelist matches are denied unless `allow-prod-dangerous-ddl=true` is configured.
- [x] AC-4: Project, environment, schema, and table wildcard matching is covered by tests.
- [x] AC-5: Rejection decisions expose machine-readable reason codes and human-readable reasons.
- [x] AC-6: `./mvnw test` and `python3 scripts/check_harness.py` pass.

## Risk Notes

| Risk                                                | Likelihood | Mitigation                                                                                              |
|-----------------------------------------------------|------------|---------------------------------------------------------------------------------------------------------|
| Whitelist matching accidentally becomes too broad   | Medium     | Require operation match and test wildcard behavior separately from exact match behavior.                |
| Prod protection can be bypassed by wildcard entries | Medium     | Enforce prod override after whitelist match and test wildcard prod denial.                              |
| Policy decisions are not audit-ready                | Low        | Include reason code, reason, matched whitelist flag, and audit requirement flag in the decision record. |

## Implementation Steps

### Step 1: Add failing policy tests

**Files:** `src/test/java/com/refinex/dbflow/sqlpolicy/DangerousDdlPolicyEngineTests.java`
**Verification:** `./mvnw -Dtest=DangerousDdlPolicyEngineTests test` fails for missing engine/model symbols.

Status: ✅ Done
Evidence: `./mvnw -Dtest=DangerousDdlPolicyEngineTests test` failed as expected with missing `DangerousDdlPolicyEngine`,
`DangerousDdlPolicyDecision`, `DangerousDdlPolicyReasonCode`, and `setAllowProdDangerousDdl`.
Deviations:

### Step 2: Implement policy engine and decision model

**Files:** `src/main/java/com/refinex/dbflow/sqlpolicy/*`,
`src/main/java/com/refinex/dbflow/config/DbflowProperties.java`
**Verification:** `./mvnw -Dtest=DangerousDdlPolicyEngineTests,DbflowPropertiesTests test` passes.

Status: ✅ Done
Evidence: `./mvnw -Dtest=DangerousDdlPolicyEngineTests,DbflowPropertiesTests test` passed: 16 tests, 0 failures, 0
errors.
Deviations:

### Step 3: Update docs and archive plan

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

Status: ✅ Done
Evidence: `./mvnw test` passed: 78 tests, 0 failures, 0 errors. `python3 scripts/check_harness.py` passed.
`git diff --check` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                | Notes |
|------|--------|-------------------------------------------------------------------------|-------|
| 1    | ✅      | RED test compile failure confirmed missing policy engine/model symbols. |       |
| 2    | ✅      | Targeted policy/config tests passed: 16 tests, 0 failures, 0 errors.    |       |
| 3    | ✅      | Full Maven suite, Harness validator, and diff whitespace check passed.  |       |

## Decision Log

| Decision                                                 | Context                                                    | Alternatives Considered                  | Rationale                                                        |
|----------------------------------------------------------|------------------------------------------------------------|------------------------------------------|------------------------------------------------------------------|
| Model `allow-prod-dangerous-ddl` on each whitelist entry | User required an explicit flag when prod matches whitelist | Global flag under dangerous-ddl defaults | Per-entry flag keeps production exceptions narrow and auditable. |

## Completion Summary

Completed: 2026-04-29
Duration: 3 steps
All acceptance criteria: PASS

Summary:

- Added `DangerousDdlPolicyEngine` with YAML whitelist matching for `DROP_TABLE` and `DROP_DATABASE`.
- Added audit-ready `DangerousDdlPolicyDecision` and machine-readable `DangerousDdlPolicyReasonCode`.
- Added per-whitelist-entry `allow-prod-dangerous-ddl` binding.
- Added tests for default deny, exact whitelist allow, prod explicit allow, wildcard matching, and classification
  rejection precedence.
- Updated architecture and observability docs.
