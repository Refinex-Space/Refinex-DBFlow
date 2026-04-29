# Execution Plan: MCP Token Lifecycle

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Implement the unique per-user MCP Token lifecycle with one-time plaintext display, peppered hashing, revocation,
reissue, validation, and last-used updates.

## Scope

**In scope:**

- MCP Token pepper configuration from Spring external configuration.
- Token generation with cryptographically strong random bytes.
- HMAC-based token hash using configured pepper; no plaintext persistence.
- Token prefix persistence for display/audit correlation only.
- Service methods for issue, revoke active token, reissue after revoke, validate, and update `last_used_at`.
- Tests for issue success, duplicate issue failure, revoke, reissue, validation, revoked-token rejection, and last-used
  update.
- Documentation updates for token secret and plaintext boundaries.

**Out of scope:**

- HTTP/MCP Bearer Token filter implementation.
- Admin UI for issuing or displaying tokens.
- Audit event emission for token lifecycle operations.
- Token rotation scheduler or expiration cleanup.

## Constraints

- Token plaintext must not be stored in database, logs, or audit records.
- Token validation must use hash comparison.
- Pepper must not have a hard-coded default and must come from environment variables or secure external configuration.
- Preserve one active token per user using the existing nullable `active_flag` unique constraint.

## Acceptance Criteria

- [x] AC-1: `./mvnw test` passes.
- [x] AC-2: Token issue returns plaintext once and persists only hash/prefix metadata.
- [x] AC-3: Duplicate issue fails while an active token exists.
- [x] AC-4: Revoked token cannot validate, and reissue after revoke succeeds.
- [x] AC-5: Successful validation uses hash lookup/comparison and updates `last_used_at`.
- [x] AC-6: Documentation records token plaintext, hash, pepper, and audit boundaries.

## Risk Notes

| Risk                                               | Likelihood | Mitigation                                                                                            |
|----------------------------------------------------|------------|-------------------------------------------------------------------------------------------------------|
| Plaintext token leaks through persistence or logs. | Medium     | Return plaintext only in a service response object, never store it, and avoid logging in the service. |
| Pepper absence breaks unrelated local startup.     | Medium     | Validate pepper only when token lifecycle service is used, not during generic app startup.            |
| Token prefix is mistaken for a secret.             | Low        | Treat prefix as display/audit correlation metadata and keep it short.                                 |

## Implementation Steps

### Step 1: Extend token metadata accessors and repository queries

**Files:** `src/main/java/com/refinex/dbflow/access/entity/DbfApiToken.java`,
`src/main/java/com/refinex/dbflow/access/repository/DbfApiTokenRepository.java`
**Verification:** Token lifecycle tests

Status: ✅ Completed
Evidence: `McpTokenServiceJpaTests.shouldIssueTokenWithoutPersistingPlaintext` and
`shouldValidateTokenAndUpdateLastUsedAt` cover entity getters plus `findByTokenHash`.
Deviations:

### Step 2: Add MCP token pepper configuration and lifecycle service

**Files:** `src/main/java/com/refinex/dbflow/security/*`
**Verification:** Token lifecycle tests

Status: ✅ Completed
Evidence: `McpTokenService`, `McpTokenProperties`, `McpTokenIssueResult`, and
`McpTokenValidationResult` added under `com.refinex.dbflow.security`.
Deviations:

### Step 3: Add token lifecycle tests

**Files:** `src/test/java/com/refinex/dbflow/security/*`
**Verification:** `./mvnw test`

Status: ✅ Completed
Evidence: `./mvnw test -Dtest=McpTokenServiceJpaTests` exited 0 with 5 tests passing.
Deviations:

### Step 4: Update control-plane documentation

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, `src/main/resources/application.yml`
**Verification:** `python3 scripts/check_harness.py`

Status: ✅ Completed
Evidence: `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, and `src/main/resources/application.yml`
record the MCP Token pepper, one-time plaintext, hash, and audit/log boundary.
Deviations:

### Step 5: Final verification

**Files:** Repository root
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, `git diff --check`

Status: ✅ Completed
Evidence: `./mvnw test` exited 0 with 33 tests passing. Final Harness and diff checks are recorded
in the handoff summary.
Deviations:

## Progress Log

| Step | Status | Evidence                                     | Notes                                                                       |
|------|--------|----------------------------------------------|-----------------------------------------------------------------------------|
| 1    | ✅      | `./mvnw test -Dtest=McpTokenServiceJpaTests` | Token metadata accessors and hash lookup covered.                           |
| 2    | ✅      | `McpTokenService` service tests              | Peppered HMAC issue/validate/revoke flow implemented.                       |
| 3    | ✅      | `McpTokenServiceJpaTests`: 5 tests passing   | Success, duplicate, invalid, revoked, reissue, and last-used paths covered. |
| 4    | ✅      | Docs and `application.yml` updated           | Token plaintext/hash/pepper boundaries recorded.                            |
| 5    | ✅      | `./mvnw test`: 33 tests passing              | Final verification recorded before handoff.                                 |

## Decision Log

| Decision                                          | Context                                                                         | Alternatives Considered                           | Rationale                                                                               |
|---------------------------------------------------|---------------------------------------------------------------------------------|---------------------------------------------------|-----------------------------------------------------------------------------------------|
| Use HMAC-SHA-256 for token hashes.                | Token validation needs deterministic hash comparison with a server-side pepper. | Plain SHA-256 with concatenated pepper or BCrypt. | HMAC gives keyed deterministic hashes suitable for lookup without storing plaintext.    |
| Keep token service separate from MCP HTTP filter. | Current phase is lifecycle and metadata, not endpoint authentication.           | Implement Bearer filter now.                      | Separation keeps behavior testable and avoids mixing lifecycle with transport security. |

## Completion Summary

Implemented service-level MCP Token lifecycle:

- Added pepper-backed HMAC-SHA-256 token hashing with no plaintext persistence.
- Added one-time issue result, validation result, active-token revocation, reissue after revocation, and last-used
  update behavior.
- Preserved one active token per user through service checks and the existing metadata unique constraint.
- Added JPA slice tests for issue, duplicate issue, invalid token, revoke, reissue, validation, and `last_used_at`.
- Updated architecture and observability docs with token secret boundaries and real source paths.
