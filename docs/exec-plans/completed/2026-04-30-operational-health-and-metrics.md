# 2026-04-30 Operational Health And Metrics

## Objective

Implement DBFlow operational health checks and Micrometer metrics for the Spring Boot management/runtime surface.

## Scope

- Add Spring Boot Actuator with minimal web exposure.
- Provide custom health indicators for metadata database, target datasource registry, Nacos config/discovery, and MCP
  endpoint readiness.
- Extract the existing management health-page checks into a shared health service so `/admin/health` and Actuator use
  the same source of truth.
- Add Micrometer metrics for MCP calls, SQL risk distribution, rejection count, SQL execution duration, and pending
  confirmation challenges.
- Update `docs/OBSERVABILITY.md` with runtime endpoints, security boundaries, and verification commands.
- Add tests for health indicators, actuator endpoint exposure, metrics registration, and management health reuse.

## Non-Scope

- Do not introduce Prometheus, Grafana, tracing, log aggregation, or alerting rules.
- Do not make the dangerous policy page editable.
- Do not probe target databases with business SQL beyond safe datasource/pool readiness checks.
- Do not expose database passwords, token plaintext, token hashes, JDBC URLs with secrets, or full result sets.

## Constraints

- Actuator exposure must stay minimal; sensitive details remain hidden by default.
- Management health page must reuse the shared health service instead of duplicating health logic.
- Existing MCP `/mcp` bearer-token security and admin form-login boundaries must remain unchanged.
- All metrics must use bounded tags with stable names.

## Assumptions

- `/actuator/health`, `/actuator/metrics`, and `/actuator/metrics/{name}` are sufficient for local operational smoke
  verification at this phase.
- Nacos is still opt-in; when disabled, the Nacos health indicator reports a non-sensitive disabled/unknown state rather
  than failing local startup.
- Target datasource registry health can be derived from configured project environments plus Hikari pool readiness.

## Plan

- [x] Add Actuator dependency/configuration and minimal actuator security.
- [x] Extract a shared `DbflowHealthService` from the existing management health-page logic.
- [x] Add custom health indicators backed by the shared service.
- [x] Add `DbflowMetricsService` and metric hooks in MCP tools, audit writing, SQL execution, and confirmation gauge.
- [x] Add focused tests for actuator security, health indicators, metrics, and admin health reuse.
- [x] Update `docs/OBSERVABILITY.md` and run full verification.

## Verification

- Baseline before changes:
    - `python3 scripts/check_harness.py` passed.
    - `./mvnw test` passed with `Tests run: 125, Failures: 0, Errors: 0, Skipped: 10`.
- Final evidence:
    - `git diff --check` passed.
    - `python3 scripts/check_harness.py` passed.
    - `./mvnw -q -Dtest=OperationalHealthAndMetricsTests test` passed.
    - `./mvnw -q -Dtest=AdminOperationsPageControllerTests,AuditEventWriterTests,DbflowMcpDiscoveryTests test` passed.
    - `./mvnw test` passed with `Tests run: 130, Failures: 0, Errors: 0, Skipped: 10`.
    - `./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=18080"` started locally.
    - `curl -s -i http://localhost:18080/actuator/health` returned HTTP 200 and `{"status":"UP"}`.
    - `curl -s http://localhost:18080/actuator/metrics` listed `dbflow.confirmation.challenges`.
    - `curl -s -o /dev/null -w '%{http_code}\n' http://localhost:18080/actuator/env` returned HTTP 403.
