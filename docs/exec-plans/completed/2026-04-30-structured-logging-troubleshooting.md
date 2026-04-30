# 2026-04-30 Structured Logging And Troubleshooting

## Objective

Add request/trace correlation to operational logs and create an executable troubleshooting runbook for DBFlow startup,
configuration, datasource, token, and MCP connectivity failures.

## Scope

- Add `requestId` and `traceId` MDC coverage for HTTP requests and service-level execution paths.
- Add safe operational logs for MCP requests, SQL execution, policy denial, configuration reload, and datasource
  replacement.
- Keep logs free of Token plaintext, database passwords, JDBC URLs, and full result sets.
- Add `docs/runbooks/troubleshooting.md` with at least eight executable failure scenarios.
- Update `docs/OBSERVABILITY.md` with logging and troubleshooting entry points.

## Non-Scope

- Do not introduce a centralized tracing backend, OpenTelemetry collector, ELK stack, or JSON log ingestion pipeline.
- Do not change audit persistence semantics or admin audit query behavior.
- Do not expose additional Actuator endpoints.

## Constraints

- Logs must not output Token plaintext or database passwords.
- Troubleshooting steps must be concrete commands or deterministic checks.
- Keep changes compatible with the current Spring Boot WebMVC + Logback baseline.

## Assumptions

- `X-Request-Id` remains the primary request correlation header.
- `X-Trace-Id` is optional; when absent, DBFlow uses the request id as the trace id so every log line remains
  searchable.
- Text log output with stable key/value fields is sufficient for this phase; JSON log shipping can be added later.

## Acceptance Criteria

- [x] `/mcp` request logs include request/trace correlation without Token plaintext.
- [x] SQL execution and policy-denied logs include project/env/operation/risk/sqlHash but not SQL text or result rows.
- [x] Config refresh and datasource replacement logs include request/trace correlation and sanitized target counts.
- [x] `docs/runbooks/troubleshooting.md` exists and covers at least eight executable failure scenarios.
- [x] `docs/OBSERVABILITY.md` points operators to the logging fields and runbook.
- [x] `./mvnw test` passes.

## Plan

- [x] Add trace context constants/helpers and update Logback pattern.
- [x] Add safe correlated logs to MCP, SQL, config reload, and datasource replacement paths.
- [x] Add/update tests for requestId/traceId MDC behavior.
- [x] Write the troubleshooting runbook and observability entry point.
- [x] Run targeted and full verification, then archive this plan.

## Verification

- Baseline before changes:
    - `python3 scripts/check_harness.py` passed.
    - `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
- Final evidence:
    - `./mvnw -q -DskipTests compile` passed.
    - `./mvnw -q -Dtest=RequestIdFilterTests test` passed.
    -
    `./mvnw -q -Dtest=RequestIdFilterTests,McpSecurityTests,McpEndpointGuardSecurityTests,DataSourceConfigReloaderTests,HikariDataSourceRegistryTests test`
    passed.
    - `git diff --check` passed.
    - `python3 scripts/check_harness.py` passed.
    - `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
