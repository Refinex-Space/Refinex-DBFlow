# Execution Plan: Schema Inspect Resource

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement `dbflow_inspect_schema` and the schema resource as authorized, read-only MySQL metadata inspection surfaces
backed by `information_schema`.

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/executor`
- `src/main/java/com/refinex/dbflow/mcp/DbflowMcpTools.java`
- `src/main/java/com/refinex/dbflow/mcp/DbflowMcpResources.java`
- `src/test/java/com/refinex/dbflow/executor`
- `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`

**Out of scope:**

- Cross-database diffing or dependency graphing.
- Returning target datasource configuration, JDBC URLs, usernames, or passwords.
- Complex cursor protocol beyond a bounded `maxItems`/`truncated` response for this stage.
- Parsing object definitions or routine bodies.

## Constraints

- Follow root `AGENTS.md` and Java development standards.
- `inspect_schema` and schema resource must authorize project/environment before target datasource access.
- Metadata must come from `information_schema`, not from direct application table scans.
- Response fields must be stable and MCP-friendly.
- Sensitive connection information must not be returned.

## Assumptions

- Schema resource template has only project/env path variables, so it returns a bounded project-environment overview
  using default inspect options.
- The tool supports explicit `schema` and optional `table` filters.
- Large schema control uses deterministic `maxItems` and `truncated` fields rather than opaque server-side cursors.
- MySQL 8/5.7 Testcontainers tests may skip locally when Docker is unavailable but must compile and run on
  Docker-enabled hosts.

## Acceptance Criteria

- [x] AC-1: `dbflow_inspect_schema` delegates to a real inspect service and returns stable fields.
- [x] AC-2: Schema resource delegates to the same service after authorization.
- [x] AC-3: Inspection reads schemas, tables, columns, indexes, views, routines, procedures, and functions from
  `information_schema`.
- [x] AC-4: Tool supports schema and table filters.
- [x] AC-5: Column output includes type, nullable, default, comment, ordinal, and key/default metadata.
- [x] AC-6: Index output includes uniqueness, sequence, column, index type, cardinality, and nullable metadata.
- [x] AC-7: Large responses are bounded and mark `truncated=true` when the cap is exceeded.
- [x] AC-8: Unauthorized inspect requests do not access target datasources and do not expose connection secrets.
- [x] AC-9: Tests cover table, column, index, view, stored procedure, stored function, filtering, and unauthorized
  environment behavior.
- [x] AC-10: `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

## Risk Notes

| Risk                                            | Likelihood | Mitigation                                                                   |
|-------------------------------------------------|------------|------------------------------------------------------------------------------|
| MySQL 5.7 and 8.0 metadata columns drift        | Medium     | Use long-standing `information_schema` columns shared by both versions.      |
| Resource accidentally returns datasource config | Low        | Service result DTO contains only metadata fields, no config/source fields.   |
| Large schema responses get too big              | Medium     | Apply max-items cap to each metadata category and return `truncated`.        |
| Docker unavailable locally                      | Medium     | Use `disabledWithoutDocker=true` and keep non-container authorization tests. |

## Implementation Steps

### Step 1: Add failing inspect tests

**Files:** `src/test/java/com/refinex/dbflow/executor/SchemaInspectServiceTests.java`,
`src/test/java/com/refinex/dbflow/executor/SchemaInspectServiceMysqlTests.java`
**Verification:** `./mvnw -Dtest=SchemaInspectServiceTests,SchemaInspectServiceMysqlTests test` fails for missing
service/model.

Status: ✅ Completed
Evidence:

- 2026-04-29: `./mvnw -Dtest=SchemaInspectServiceTests,SchemaInspectServiceMysqlTests test` failed at test
  compilation because `SchemaInspectService` and `SchemaInspectRequest` do not exist yet.
  Deviations:

### Step 2: Implement schema inspect service and DTOs

**Files:** `src/main/java/com/refinex/dbflow/executor/*`
**Verification:** `./mvnw -Dtest=SchemaInspectServiceTests,SchemaInspectServiceMysqlTests test` passes or skips only
Docker-backed container methods when Docker is unavailable.

Status: ✅ Completed
Evidence:

- 2026-04-29: Added `SchemaInspectService` and metadata DTO records for schemas, tables, columns, indexes, views,
  and routines.
- 2026-04-29: `./mvnw -Dtest=SchemaInspectServiceTests,SchemaInspectServiceMysqlTests test` passed; MySQL
  Testcontainers methods skipped locally because Docker is unavailable.
  Deviations:

### Step 3: Wire MCP tool and resource

**Files:** `src/main/java/com/refinex/dbflow/mcp/DbflowMcpTools.java`,
`src/main/java/com/refinex/dbflow/mcp/DbflowMcpResources.java`
**Verification:** `./mvnw -Dtest=DbflowMcpServerTests,DbflowMcpDiscoveryTests test` passes.

Status: ✅ Completed
Evidence:

- 2026-04-29: Wired `dbflow_inspect_schema` in `DbflowMcpTools` and schema resource in `DbflowMcpResources` to the
  shared inspect service.
- 2026-04-29:
  `./mvnw -Dtest=SchemaInspectServiceTests,SchemaInspectServiceMysqlTests,DbflowMcpServerTests,DbflowMcpDiscoveryTests test`
  passed with 9 tests, 0 failures, 0 errors, 4 Docker-skipped Testcontainers methods.
  Deviations:

### Step 4: Update control-plane docs and verify

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

Status: ✅ Completed
Evidence:

- 2026-04-29: Updated architecture, observability, and plan index documentation for schema inspect implementation.
- 2026-04-29: `./mvnw test` passed with 98 tests, 0 failures, 0 errors, 10 Docker-skipped Testcontainers methods.
- 2026-04-29: `python3 scripts/check_harness.py` passed.
- 2026-04-29: `git diff --check` passed.
  Deviations:
- Local Docker is unavailable, so MySQL 8/5.7 container-backed schema inspect tests compiled and were skipped locally.

## Progress Log

| Step | Status | Evidence                                             | Notes                                                                                 |
|------|--------|------------------------------------------------------|---------------------------------------------------------------------------------------|
| 1    | ✅      | RED test compilation failure                         | Missing inspect service/model confirms the implementation gap.                        |
| 2    | ✅      | Targeted inspect tests pass/skip Docker-only methods | Service authorizes before target datasource access and returns bounded metadata DTOs. |
| 3    | ✅      | MCP targeted tests pass                              | Tool and resource both delegate to the inspect service.                               |
| 4    | ✅      | Full verification commands pass                      | Plan archived and docs updated.                                                       |

## Decision Log

| Decision                                         | Context                                                                           | Alternatives Considered                        | Rationale                                                                                              |
|--------------------------------------------------|-----------------------------------------------------------------------------------|------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| Use per-category `maxItems` cap plus `truncated` | Large schemas can produce many metadata rows across tables, columns, and indexes. | Cursor protocol, global row budget.            | Stable bounded fields are simpler for MCP clients at this stage.                                       |
| Read only `information_schema`                   | The feature is metadata inspection, not application data browsing.                | JDBC metadata APIs, direct target table reads. | MySQL `information_schema` provides stable MySQL 5.7/8.0 metadata columns and avoids user table scans. |
| Schema resource uses default inspect options     | Resource URI has only project/env path variables.                                 | Add query string parsing to the resource URI.  | Keeps resource fields predictable while the tool handles explicit filters.                             |

## Completion Summary

Implemented `SchemaInspectService`, stable schema metadata DTOs, `dbflow_inspect_schema`, and the
`dbflow://projects/{project}/envs/{env}/schema` resource. The service authorizes project/environment access before
target datasource lookup, reads MySQL `information_schema`, supports schema/table filters, caps each metadata category,
returns `truncated`, and avoids returning target JDBC URLs or passwords. Tests cover denial-before-target-access,
filtering, truncation, and MySQL 8/5.7 table/column/index/view/procedure/function metadata through Docker-optional
Testcontainers.
