# 2026-04-30 Metadata And Nacos Deployment Clarity

## Objective

Clarify where the Flyway metadata migration runs and provide copy-ready Nacos configuration documents for the current
`application-nacos.yml` imports.

## Scope

- Add deployment documentation for the DBFlow metadata database and `V1__create_metadata_schema.sql` execution boundary.
- Add deployment documentation for the two Nacos config dataIds imported by `src/main/resources/application-nacos.yml`.
- Link the new documents from the deployment guide and control-plane indexes.

## Non-Scope

- No Java, SQL migration, or runtime configuration behavior changes.
- No real credentials, database passwords, Token pepper values, or Nacos passwords.
- No change to current `application-nacos.yml` import names.

## Acceptance Criteria

- [x] Documentation states that `V1__create_metadata_schema.sql` runs on the Spring Boot primary metadata `DataSource`,
  not target project databases.
- [x] Documentation provides copy-ready metadata database bootstrap commands.
- [x] Documentation maps `refinex-dbflow.yml` and `refinex-dbflow-${spring.profiles.active}.yml` to concrete content
  responsibilities.
- [x] Documentation provides copy-ready Nacos YAML snippets with only placeholders or fake values.
- [x] `git diff --check`, `python3 scripts/check_harness.py`, and `./mvnw test` pass.

## Evidence Log

- 2026-04-30: Added `docs/deployment/metadata-database.md` explaining that Flyway V1 runs against `spring.datasource`
  metadata database only, and that database/user/bootstrap SQL should stay outside Flyway migration.
- 2026-04-30: Added `docs/deployment/nacos-config.md` mapping current `application-nacos.yml` imports to
  `refinex-dbflow.yml` and `refinex-dbflow-nacos.yml` with copy-ready placeholder YAML.
- 2026-04-30: Linked both guides from `docs/deployment/README.md`, `docs/ARCHITECTURE.md`, and
  `docs/OBSERVABILITY.md`.
- 2026-04-30: Verified key terms with `rg` across the new deployment docs.
- 2026-04-30: `git diff --check` passed.
- 2026-04-30: `python3 scripts/check_harness.py` passed.
- 2026-04-30: `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
