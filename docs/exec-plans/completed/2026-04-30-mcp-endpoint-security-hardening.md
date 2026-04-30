# 2026-04-30 MCP Endpoint Security Hardening

## Objective

Harden the MCP Streamable HTTP endpoint with origin validation, request size control, basic rate limiting, and stable
client-facing error responses.

## Scope

- Add configurable trusted Origin validation for `/mcp`.
- Add configurable request size limit for `/mcp`.
- Add basic in-memory rate limiting for `/mcp` by source IP.
- Preserve the existing Bearer Token requirement and query-string token rejection.
- Standardize HTTP security error JSON for 401, 403, request-too-large, and rate limit responses.
- Standardize MCP tool data for policy denial, SQL execution failure, confirmation expiry, and result truncation.
- Add security tests for Origin, Token misuse, authorization denial, rate limit, request size, and secret redaction.

## Non-Scope

- Do not add distributed rate limiting, Redis, WAF integration, or gateway-specific configuration.
- Do not redesign the MCP protocol payload shape beyond stable DBFlow error metadata in tool responses.
- Do not relax existing admin session security or Actuator exposure.

## Constraints

- Query string tokens remain rejected.
- Client-facing responses must not include Java stack traces, database passwords, JDBC URLs, or token plaintext.
- Trusted origins must be configurable for LAN deployments.
- Keep the implementation local to the `/mcp` endpoint security boundary.

## Assumptions

- Non-browser MCP clients commonly omit `Origin`; absent Origin is allowed, while present Origin must match the trusted
  origin list when origin validation is enabled.
- A fixed-window in-memory limiter is sufficient for this phase and can later be replaced by gateway/distributed
  limiting without changing the tool API.
- HTTP 429 is the clearest status for rate limiting, while policy and authorization denial remain represented as 403 or
  MCP tool-level denial metadata.

## Plan

- [x] Add `dbflow.security.mcp-endpoint.*` configuration properties.
- [x] Add shared MCP security error response writer and endpoint guard filter.
- [x] Wire the guard filter before Bearer Token authentication in the MCP security chain.
- [x] Add stable MCP tool error metadata for denial, failures, confirmation expiry, and truncation.
- [x] Add/extend security tests for the main misuse and attack paths.
- [x] Update observability documentation, run verification, and archive this plan.

## Verification

- Baseline before changes:
    - `python3 scripts/check_harness.py` passed.
    - `./mvnw test` passed with `Tests run: 130, Failures: 0, Errors: 0, Skipped: 10`.
- Final evidence:
    - `./mvnw -q -DskipTests compile` passed.
    - `./mvnw -q -Dtest=McpSecurityTests,McpEndpointGuardSecurityTests test` passed.
    - `git diff --check` passed.
    - `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
    - `python3 scripts/check_harness.py` passed.
