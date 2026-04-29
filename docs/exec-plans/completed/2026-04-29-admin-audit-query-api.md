# Execution Plan: Admin Audit Query API

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement the management-side audit query backend so administrators can filter, page, sort, and inspect audit events
without exposing sensitive Token metadata or database passwords.

## Scope

**In scope:**

- Audit query repository/service DTOs under `src/main/java/com/refinex/dbflow/audit`.
- Management API controller under `src/main/java/com/refinex/dbflow/admin`.
- Repository/service/controller tests for filtering, pagination, detail, and permission behavior.
- Control-plane docs and plan index updates.

**Out of scope:**

- Management UI pages.
- Export/download APIs.
- User self-service audit history API; ordinary users are blocked from the admin API for now.
- New audit write behavior.

## Constraints

- Follow root `AGENTS.md` and Java development standards.
- Only administrators may query the full audit dataset through this management API.
- Ordinary users must not access full audit data; the later user-facing scope must be limited to the caller's own rows.
- API responses must not expose Token plaintext, token id, token prefix, database passwords, or connection strings.
- Pagination must use bounded page size and stable sort field mapping.

## Assumptions

- The first management API path is `/admin/api/audit-events`.
- Existing admin Spring Security `ROLE_ADMIN` protection on `/admin/**` is the authorization boundary for full audit
  queries.
- `createdAt desc` is the default sort because audit review normally starts with recent events.
- Sensitive text redaction should be deterministic and conservative, without attempting LLM classification.

## Acceptance Criteria

- [x] AC-1: Audit query service supports time range, user id, project, environment, risk, decision, SQL hash, and tool
  filters.
- [x] AC-2: Audit query service supports bounded pagination and stable sorting.
- [x] AC-3: Audit detail lookup returns a single sanitized audit detail by id.
- [x] AC-4: Management controller exposes list and detail endpoints under `/admin/api/audit-events`.
- [x] AC-5: Full audit query endpoints require `ROLE_ADMIN`; non-admin users are rejected.
- [x] AC-6: Audit API DTOs do not expose Token plaintext, token id, token prefix, database passwords, or connection
  strings.
- [x] AC-7: Repository/service/controller tests cover filtering, pagination, detail, and permission.
- [x] AC-8: `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

## Risk Notes

| Risk                                    | Likelihood | Mitigation                                                                    |
|-----------------------------------------|------------|-------------------------------------------------------------------------------|
| Dynamic filters become hard to maintain | Medium     | Use Spring Data JPA `Specification` with small named predicates.              |
| API leaks token metadata from entity    | Medium     | Return dedicated DTOs instead of entities.                                    |
| Password-like SQL text leaks in detail  | Medium     | Apply deterministic text redaction before returning SQL/error/summary text.   |
| Sort parameters expose arbitrary fields | Low        | Map allowed API sort names to entity fields and default unknown names safely. |

## Implementation Steps

### Step 1: Add RED repository/service/controller tests

**Files:** `src/test/java/com/refinex/dbflow/audit/AuditQueryServiceTests.java`,
`src/test/java/com/refinex/dbflow/admin/AdminAuditEventControllerTests.java`
**Verification:** `./mvnw -Dtest=AuditQueryServiceTests,AdminAuditEventControllerTests test` fails for missing query
service/controller types.

Status: ✅ Completed
Evidence:

- 2026-04-29: `./mvnw -Dtest=AuditQueryServiceTests,AdminAuditEventControllerTests test` failed at test
  compilation because `AuditQueryService` does not exist yet.
  Deviations:

### Step 2: Implement query model, repository specification support, and service

**Files:** `src/main/java/com/refinex/dbflow/audit/repository/DbfAuditEventRepository.java`,
`src/main/java/com/refinex/dbflow/audit/service/*`
**Verification:** `./mvnw -Dtest=AuditQueryServiceTests test` passes.

Status: ✅ Completed
Evidence:

- 2026-04-29: Added `JpaSpecificationExecutor` support, query criteria/page/detail DTOs, deterministic text
  sanitizer, and `AuditQueryService`.
- 2026-04-29: `./mvnw -Dtest=AuditQueryServiceTests test` passed with 3 tests.
  Deviations:

### Step 3: Implement admin REST controller and permission coverage

**Files:** `src/main/java/com/refinex/dbflow/admin/*`, controller tests
**Verification:** `./mvnw -Dtest=AuditQueryServiceTests,AdminAuditEventControllerTests test` passes.

Status: ✅ Completed
Evidence:

- 2026-04-29: Added `/admin/api/audit-events` list and detail endpoints backed by `AuditQueryService`.
- 2026-04-29: `./mvnw -Dtest=AuditQueryServiceTests,AdminAuditEventControllerTests test` passed with 7 tests.
  Deviations:

### Step 4: Update docs, archive, and verify

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, this plan
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

Status: ✅ Completed
Evidence:

- 2026-04-29: Updated architecture, observability, package docs, and plan index for the admin audit query API.
- 2026-04-29: `./mvnw test` passed with 108 tests, 0 failures, 0 errors, and 10 Docker-skipped Testcontainers
  methods.
- 2026-04-29: `python3 scripts/check_harness.py` passed.
- 2026-04-29: `git diff --check` passed with no whitespace errors.
  Deviations:

## Progress Log

| Step | Status | Evidence                                  | Notes                                                         |
|------|--------|-------------------------------------------|---------------------------------------------------------------|
| 1    | ✅      | RED test compilation failure              | Missing audit query service confirms the implementation gap.  |
| 2    | ✅      | Service tests pass                        | Filtering, pagination, sorting, and sanitized detail covered. |
| 3    | ✅      | Targeted service/controller tests pass    | ADMIN access and USER rejection are covered.                  |
| 4    | ✅      | Full Maven, Harness, and diff checks pass | Control-plane docs updated and plan archived.                 |

## Decision Log

| Decision                           | Context                                                                      | Alternatives Considered                        | Rationale                                                                                      |
|------------------------------------|------------------------------------------------------------------------------|------------------------------------------------|------------------------------------------------------------------------------------------------|
| Use `/admin/api/audit-events`      | The feature is management-side backend API, not MCP surface.                 | Add MCP tools or UI pages.                     | Keeps full audit access inside the existing admin session security chain.                      |
| Deny non-admin access for now      | The hard constraint says ordinary users may later only query their own rows. | Add a user-facing query mode immediately.      | Avoids accidental full audit exposure while leaving user self-query as a later scoped feature. |
| Return dedicated DTOs              | `DbfAuditEvent` stores token id/prefix for traceability.                     | Return entity objects directly.                | DTO mapping prevents token metadata and future entity fields from leaking through JSON.        |
| Redact deterministic text patterns | SQL and summaries may contain passwords or JDBC URLs.                        | Rely only on upstream audit writer discipline. | Defense in depth for management detail responses without LLM judgement.                        |
| Whitelist sort fields              | Sort parameters come from query string.                                      | Pass raw sort fields to Spring Data.           | Prevents accidental sorting by sensitive or unstable entity fields.                            |

## Completion Summary

Implemented the management-side audit query backend with `AuditQueryService`, specification-based filtering, bounded
pagination, stable sort mapping, sanitized summary/detail DTOs, and `/admin/api/audit-events` list/detail endpoints.
The admin API is protected by the existing `/admin/**` `ROLE_ADMIN` session chain; non-admin users receive 403. Tests
cover filter combinations, pagination/sorting, sanitized detail output, and permission behavior.
