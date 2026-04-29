# Execution Plan: Unified Audit Event Writer

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement a unified audit event writer for SQL execution, EXPLAIN, and confirmation flows so core audit fields are
persisted consistently across received, denied, confirmation, executed, failed, and expired paths.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/audit`
- `src/main/java/com/refinex/dbflow/executor`
- `src/main/java/com/refinex/dbflow/sqlpolicy`
- `src/main/java/com/refinex/dbflow/mcp/DbflowMcpTools.java`
- `src/main/resources/db/migration/V1__create_metadata_schema.sql`
- `src/test/java/com/refinex/dbflow/audit`
- Existing SQL execution, EXPLAIN, and TRUNCATE confirmation tests when constructor wiring changes.
- `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`

**Out of scope:**

- Audit search/reporting APIs or management UI.
- Persisting full SQL result sets.
- Persisting MCP Token plaintext.
- Changing business decisions for SQL policy or confirmation.

## Constraints

- Follow root `AGENTS.md` and Java development standards.
- Every changed audit path must preserve existing authorization/policy/execution order.
- Token plaintext must not be accepted or persisted by the writer.
- `result_summary` must be bounded and must not contain complete query result sets.
- Exception paths must write audit evidence before returning or throwing where the service has enough request context.

## Assumptions

- `status` remains compatible with existing values, while the new `decision` field carries finer event names such as
  `REQUEST_RECEIVED`, `POLICY_DENIED`, and `EXECUTED`.
- `McpAuthenticationContext.clientInfo()` is the current source for client name/version; when it is not parseable,
  the writer stores the client info as the name and `unknown` as the version.
- The initial Flyway migration can still be edited because this repository has not introduced versioned production
  migration compatibility yet.

## Acceptance Criteria

- [x] AC-1: `AuditEventWriter` persists request received, policy denied, requires confirmation, executed, failed, and
  confirmation expired events.
- [x] AC-2: Audit rows include non-empty core fields: request id, user id, token id, client name, client version,
  user agent, source IP, project/env, tool, operation, risk, decision, SQL hash, and bounded result summary where
  applicable.
- [x] AC-3: SQL execution success, denial, and exception paths write through the unified writer.
- [x] AC-4: EXPLAIN success, denial, and exception paths write through the unified writer.
- [x] AC-5: TRUNCATE confirmation creation, confirmation success, and expiration paths write through the unified
  writer.
- [x] AC-6: Token plaintext is not stored; only token id and display prefix are persisted.
- [x] AC-7: `result_summary` is capped and does not persist full result rows.
- [x] AC-8: `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

## Risk Notes

| Risk                                        | Likelihood | Mitigation                                                               |
|---------------------------------------------|------------|--------------------------------------------------------------------------|
| Audit schema change breaks existing tests   | Medium     | Keep existing status values and add fields without removing old columns. |
| Request-received event changes audit counts | Medium     | Update tests to assert required statuses rather than exact event counts. |
| Result summary accidentally stores row data | Low        | Centralize summary trimming in `AuditEventWriter`.                       |
| Client info format is inconsistent          | Medium     | Store stable fallback values when parsing cannot split name/version.     |

## Implementation Steps

### Step 1: Add failing audit writer tests

**Files:** `src/test/java/com/refinex/dbflow/audit/AuditEventWriterTests.java`
**Verification:** `./mvnw -Dtest=AuditEventWriterTests test` fails for missing writer/model fields.

Status: ✅ Completed
Evidence:

- 2026-04-29: `./mvnw -Dtest=AuditEventWriterTests test` failed at test compilation because
  `AuditEventWriter`, `AuditEventWriteRequest`, and `AuditRequestContext` do not exist yet.
  Deviations:

### Step 2: Implement writer, schema fields, and entity accessors

**Files:** `src/main/java/com/refinex/dbflow/audit/*`,
`src/main/resources/db/migration/V1__create_metadata_schema.sql`
**Verification:** `./mvnw -Dtest=AuditEventWriterTests,MetadataSchemaMigrationTests test` passes.

Status: ✅ Completed
Evidence:

- 2026-04-29: Added `AuditEventWriter`, `AuditEventWriteRequest`, `AuditRequestContext`, entity fields/getters, and
  audit migration columns for token id, user agent, tool, and decision.
- 2026-04-29: `./mvnw -Dtest=AuditEventWriterTests,MetadataSchemaMigrationTests test` passed with 8 tests.
  Deviations:

### Step 3: Wire execution, EXPLAIN, and confirmation services

**Files:** `src/main/java/com/refinex/dbflow/executor/*`, `src/main/java/com/refinex/dbflow/sqlpolicy/*`,
`src/main/java/com/refinex/dbflow/mcp/DbflowMcpTools.java`, affected tests
**Verification:** targeted SQL/audit tests pass.

Status: ✅ Completed
Evidence:

- 2026-04-29: Wired SQL execution, EXPLAIN, TRUNCATE confirmation create/confirm paths through
  `AuditEventWriter`; MCP tools now pass client/user-agent/source-ip/tool audit context.
- 2026-04-29:
  `./mvnw -Dtest=AuditEventWriterTests,MetadataSchemaMigrationTests,SqlExplainServiceTests,TruncateConfirmationServiceJpaTests test`
  passed with 17 tests.
- 2026-04-29: `./mvnw test` passed with 101 tests, 0 failures, 0 errors, 10 Docker-skipped Testcontainers methods.
  Deviations:

### Step 4: Update control-plane docs and verify

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

Status: ✅ Completed
Evidence:

- 2026-04-29: Updated architecture, observability, and plan indexes for the unified audit writer and its persisted
  metadata surface.
- 2026-04-29: `./mvnw test` passed with 101 tests, 0 failures, 0 errors, and 10 Docker-skipped Testcontainers
  methods.
- 2026-04-29: `python3 scripts/check_harness.py` passed.
- 2026-04-29: `git diff --check` passed with no whitespace errors.
  Deviations:

## Progress Log

| Step | Status | Evidence                                  | Notes                                                          |
|------|--------|-------------------------------------------|----------------------------------------------------------------|
| 1    | ✅      | RED test compilation failure              | Missing unified writer/model confirms the implementation gap.  |
| 2    | ✅      | Writer and metadata tests pass            | Summary capping and core-field persistence are covered.        |
| 3    | ✅      | Targeted and full Maven tests pass        | Execution, EXPLAIN, and confirmation paths now use the writer. |
| 4    | ✅      | Full Maven, Harness, and diff checks pass | Control-plane docs updated and plan archived.                  |

## Decision Log

| Decision                                 | Context                                                                      | Alternatives Considered                         | Rationale                                                                                      |
|------------------------------------------|------------------------------------------------------------------------------|-------------------------------------------------|------------------------------------------------------------------------------------------------|
| Keep `status` and add `decision`         | Existing audit consumers already rely on coarse status values.               | Replace status with a new enum-only model.      | Preserves compatibility while exposing stable event-level decisions to MCP clients and tests.  |
| Bound summaries in the writer            | Multiple services produce different result summaries.                        | Require every caller to trim summaries.         | Centralizing the cap prevents full result-set persistence from leaking through new call sites. |
| Persist token id and token prefix only   | Audit rows need token traceability without storing secrets.                  | Store raw bearer token or omit token metadata.  | Token id/prefix provide correlation while satisfying the no-token-plaintext constraint.        |
| Parse MCP client info into audit context | SQL tool calls already receive `Mcp-Client-Info`, user-agent, and source IP. | Leave client metadata outside service requests. | Passing a service-level context keeps audit writer independent from MCP transport details.     |

## Completion Summary

Implemented `AuditEventWriter` with request, denial, confirmation, execution, failure, confirmation-confirmed, and
confirmation-expired event methods. SQL execution, EXPLAIN, and TRUNCATE confirmation flows now route audit writes
through the unified writer, include token id/prefix and client/tool metadata, and cap `result_summary` before
persistence. Metadata schema/entity/tests now cover token id, user agent, tool, and decision fields, and control-plane
docs were updated with the new audit surface.
