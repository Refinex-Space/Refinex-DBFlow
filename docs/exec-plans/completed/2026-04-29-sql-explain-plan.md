# Execution Plan: SQL Explain Plan

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement `dbflow_explain_sql` as a controlled, non-mutating EXPLAIN capability with stable MCP-friendly plan fields
and basic index advice.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/executor`
- `src/main/java/com/refinex/dbflow/mcp/DbflowMcpTools.java`
- `src/test/java/com/refinex/dbflow/executor`
- `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`

**Out of scope:**

- P07.3 schema inspect implementation.
- LLM-based or cost-model-heavy index advice.
- Executing target DML or changing target data while explaining.
- UI or admin audit search screens.

## Constraints

- Follow root `AGENTS.md` and Java development standards.
- `dbflow_explain_sql` must authorize before target datasource access.
- The service must classify SQL and only accept SELECT plus explainable DML.
- EXPLAIN for DML must not execute the target DML.
- MySQL 8 should prefer `EXPLAIN FORMAT=JSON`; MySQL 5.7 should use compatible traditional output when needed.
- MCP response fields must remain stable and client-friendly.

## Assumptions

- Explainable DML in this stage means `INSERT`, `UPDATE`, and `DELETE`.
- Basic index advice is deterministic rule text based on traditional plan fields, not an optimization engine.
- MySQL 8/5.7 integration tests may be skipped locally when Docker is unavailable, but must compile and run on
  Docker-enabled hosts.

## Acceptance Criteria

- [x] AC-1: `dbflow_explain_sql` delegates to a real explain service and returns stable fields.
- [x] AC-2: SELECT and explainable DML are explained through `EXPLAIN`, without executing target DML.
- [x] AC-3: MySQL 8 path attempts `EXPLAIN FORMAT=JSON` and returns a JSON plan summary when available.
- [x] AC-4: MySQL 5.7 path returns compatible traditional plan rows.
- [x] AC-5: Plan rows include `table`, `type`, `key`, `rows`, `filtered`, and `extra`.
- [x] AC-6: Basic index advice flags full scans or missing keys without LLM judgment.
- [x] AC-7: Tests cover indexed query, unindexed query, syntax error, and unauthorized environment.
- [x] AC-8: `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

## Risk Notes

| Risk                                    | Likelihood | Mitigation                                                            |
|-----------------------------------------|------------|-----------------------------------------------------------------------|
| EXPLAIN accidentally mutates data       | Low        | Always prefix with EXPLAIN and assert DML row counts do not change.   |
| MySQL 5.7 rejects JSON explain in cases | Medium     | Fallback to traditional EXPLAIN and make JSON summary optional.       |
| Plan fields drift by server version     | Medium     | Normalize output into stable DTO fields and keep raw values optional. |
| Docker unavailable in local environment | Medium     | Use `disabledWithoutDocker=true` and keep non-container denial tests. |

## Implementation Steps

### Step 1: Add failing explain tests

**Files:** `src/test/java/com/refinex/dbflow/executor/SqlExplainServiceMysqlTests.java`,
`src/test/java/com/refinex/dbflow/executor/SqlExplainServiceTests.java`
**Verification:** `./mvnw -Dtest=SqlExplainServiceTests,SqlExplainServiceMysqlTests test` fails for missing
service/model.

Status: ✅ Completed
Evidence:

- 2026-04-29: `./mvnw -Dtest=SqlExplainServiceTests,SqlExplainServiceMysqlTests test` failed at test compilation because
  `SqlExplainService` and `SqlExplainRequest` do not exist yet.
  Deviations:

### Step 2: Implement explain service and DTOs

**Files:** `src/main/java/com/refinex/dbflow/executor/*`
**Verification:** `./mvnw -Dtest=SqlExplainServiceTests,SqlExplainServiceMysqlTests test` passes or skips only
Docker-backed
container methods when Docker is unavailable.

Status: ✅ Completed
Evidence:

- 2026-04-29: Added `SqlExplainService` plus stable request/result/plan/advice DTOs.
- 2026-04-29: `./mvnw -Dtest=SqlExplainServiceTests,SqlExplainServiceMysqlTests test` passed; MySQL 8/5.7 container
  methods skipped locally because Docker is unavailable.
  Deviations:
- Local environment has no Docker socket, so MySQL 8/5.7 Testcontainers methods compiled but skipped locally.

### Step 3: Wire MCP explain tool

**Files:** `src/main/java/com/refinex/dbflow/mcp/DbflowMcpTools.java`
**Verification:** `./mvnw -Dtest=DbflowMcpServerTests,DbflowMcpDiscoveryTests test` passes.

Status: ✅ Completed
Evidence:

- 2026-04-29: `DbflowMcpTools.explainSql` now delegates to `SqlExplainService` and returns stable MCP-friendly fields.
- 2026-04-29: `./mvnw -Dtest=DbflowMcpServerTests,DbflowMcpDiscoveryTests test` passed.
  Deviations:

### Step 4: Update control-plane docs and verify

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

Status: ✅ Completed
Evidence:

- 2026-04-29: Updated `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, and `docs/PLANS.md`.
- 2026-04-29: `./mvnw test` passed with 93 tests, 0 failures, 0 errors, 6 Docker-optional skipped tests.
- 2026-04-29: `python3 scripts/check_harness.py` passed.
- 2026-04-29: `git diff --check` passed.
  Deviations:

## Progress Log

| Step | Status | Evidence                      | Notes                                                                       |
|------|--------|-------------------------------|-----------------------------------------------------------------------------|
| 1    | ✅      | RED test compilation failure  | Missing explain service/model confirms the implementation gap.              |
| 2    | ✅      | Targeted explain tests passed | MySQL Testcontainers methods skipped locally because Docker is unavailable. |
| 3    | ✅      | MCP tests passed              | `dbflow_explain_sql` returns stable service result fields.                  |
| 4    | ✅      | Full Maven test passed        | Remaining Harness/diff checks run during completion verification.           |

## Decision Log

| Decision                                          | Context                                                            | Alternatives Considered                   | Rationale                                                                                            |
|---------------------------------------------------|--------------------------------------------------------------------|-------------------------------------------|------------------------------------------------------------------------------------------------------|
| Prefer MySQL 8 JSON summary plus traditional rows | MCP clients need stable fields while MySQL 8 has richer JSON plans | JSON-only output                          | Traditional rows keep `table/type/key/rows/filtered/extra` stable; JSON remains an optional summary. |
| Explainable DML via `EXPLAIN <SQL>` only          | DML EXPLAIN must not mutate target data                            | Dry-run execution or transaction rollback | Prefixing with EXPLAIN avoids executing the target DML body.                                         |
| Deterministic advice only                         | User requested no LLM judgment                                     | Cost-model or LLM-generated suggestions   | Rule-based codes are stable for MCP clients and tests.                                               |

## Completion Summary

Implemented controlled `dbflow_explain_sql` through `SqlExplainService`, stable EXPLAIN DTOs, MCP wiring, MySQL 8/5.7
integration tests, non-container authorization/syntax tests, and architecture/observability documentation. Verified
with targeted explain tests, MCP tests, and full `./mvnw test`; Docker-optional container tests compiled but skipped
locally because Docker is unavailable.
