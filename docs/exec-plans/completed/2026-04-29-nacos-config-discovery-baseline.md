# Execution Plan: Nacos Config Discovery Baseline

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

Add Spring Cloud Alibaba Nacos Config and Discovery baseline wiring without requiring a real Nacos server for local
default startup and tests.

## Scope

**In scope:**

- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/resources/application-nacos.yml`
- `src/test/java/com/refinex/dbflow/config/`
- `docs/OBSERVABILITY.md`
- `docs/ARCHITECTURE.md`
- `docs/PLANS.md`

**Out of scope:**

- Real Nacos integration tests that require a running Nacos server.
- Dynamic Hikari pool hot replacement.
- Credential management implementation beyond documenting environment-variable boundaries.
- SQL execution or MCP tool behavior.

## Constraints

- Nacos credentials must not be committed.
- Default local profile must not require a real Nacos server.
- Nacos refresh must not directly destroy the currently usable Hikari target pools; future hot replacement must validate
  candidate configuration first.
- Dependency and configuration choices are verified against Context7 Spring Cloud Alibaba docs.

## Acceptance Criteria

- [x] AC-1: Maven declares Nacos Config and Discovery starters managed by the existing Spring Cloud Alibaba BOM.
- [x] AC-2: Default local configuration disables Nacos Config and Discovery.
- [x] AC-3: `nacos` profile contains `spring.config.import` examples for data id, group, namespace, and
  `refreshEnabled`.
- [x] AC-4: Nacos credentials are represented only as external placeholders and no secret defaults are committed.
- [x] AC-5: Configuration loading test verifies default local and `nacos` profile properties.
- [x] AC-6: `docs/OBSERVABILITY.md` documents local YAML startup and Nacos profile startup.
- [x] AC-7: `./mvnw test` and `python3 scripts/check_harness.py` pass without a real Nacos server.

## Risk Notes

| Risk                                                                 | Likelihood | Mitigation                                                                                                     |
|----------------------------------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------|
| Nacos starter auto-configuration tries to connect during local tests | Medium     | Disable Nacos in default `application.yml`; enable only in `nacos` profile.                                    |
| Optional Nacos import syntax drifts                                  | Medium     | Keep examples close to Context7 verified starter/import syntax and test the local resource values.             |
| Refresh invalidates live target pools                                | Medium     | Document current policy as no direct hot replacement; future refresh path must validate candidate pools first. |

## Implementation Steps

### Step 1: Add configuration loading tests

**Files:** `src/test/java/com/refinex/dbflow/config/NacosProfileConfigurationTests.java`
**Verification:** Targeted test initially fails before profile resource and default Nacos properties exist, then passes
after implementation.

Status: ✅ Done
Evidence: `./mvnw -Dtest=NacosProfileConfigurationTests test` initially failed because default Nacos disablement and
`application-nacos.yml` were absent.
Deviations:

### Step 2: Add dependencies and YAML profiles

**Files:** `pom.xml`, `src/main/resources/application.yml`, `src/main/resources/application-nacos.yml`
**Verification:** Nacos profile configuration test passes and default `./mvnw test` does not require Nacos.

Status: ✅ Done
Evidence: `./mvnw -Dtest=NacosProfileConfigurationTests test` passed with 3 tests after adding dependencies and YAML
resources.
Deviations:

### Step 3: Update docs

**Files:** `docs/OBSERVABILITY.md`, `docs/ARCHITECTURE.md`, `docs/PLANS.md`
**Verification:** Docs include local YAML and Nacos profile startup commands plus refresh safety boundary.

Status: ✅ Done
Evidence: `docs/OBSERVABILITY.md` documents default startup, Nacos profile startup, external credentials, and refresh
safety boundary; `docs/ARCHITECTURE.md` records the Nacos baseline.
Deviations:

### Step 4: Verify and archive

**Files:** `docs/exec-plans/active/2026-04-29-nacos-config-discovery-baseline.md`,
`docs/exec-plans/completed/2026-04-29-nacos-config-discovery-baseline.md`
**Verification:** `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` pass.

Status: ✅ Done
Evidence: `./mvnw test` passed with 61 tests; `python3 scripts/check_harness.py` passed with 14 manifest artifacts;
`git diff --check` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                          | Notes |
|------|--------|-----------------------------------------------------------------------------------|-------|
| 1    | ✅      | RED test failed on missing Nacos local/profile config.                            |       |
| 2    | ✅      | Targeted Nacos configuration test passed.                                         |       |
| 3    | ✅      | Observability and architecture docs updated.                                      |       |
| 4    | ✅      | `./mvnw test`, `python3 scripts/check_harness.py`, and `git diff --check` passed. |       |

## Decision Log

| Decision                                      | Context                                             | Alternatives Considered                        | Rationale                                                   |
|-----------------------------------------------|-----------------------------------------------------|------------------------------------------------|-------------------------------------------------------------|
| Enable Nacos only through the `nacos` profile | Local tests and development must not require Nacos. | Put optional Nacos imports in default profile. | Keeps default startup deterministic and makes Nacos opt-in. |

## Completion Summary

Completed: 2026-04-29
Duration: 4 steps
All acceptance criteria: PASS

Summary:

- Added Spring Cloud Alibaba Nacos Config and Discovery starters.
- Disabled Nacos Config, Discovery, and service auto-registration in default local configuration.
- Added opt-in `application-nacos.yml` with optional Nacos Config Data imports, group/namespace examples, and external
  credential placeholders.
- Added configuration resource tests and documented local/Nacos startup plus refresh safety constraints.
