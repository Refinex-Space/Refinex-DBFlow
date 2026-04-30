# 2026-04-30 Deployment Config Hardening

## Objective

补齐 DBFlow 部署配置的可复制性：管理员初始密码 hash 能快速生成，`.env.example` 覆盖当前文档支持的环境变量，默认
`application.yml` 不再承载已外部化或 Nacos 管理的 `dbflow.*` 示例配置。

## Scope

- 增加一个默认跳过、按需启用的测试辅助入口，用于生成 `DBFLOW_ADMIN_INITIAL_PASSWORD_HASH`。
- 补全 `.env.example`，覆盖 metadata database、Nacos、target datasource、admin bootstrap、MCP endpoint 安全和示例
  project/env。
- 清理 `src/main/resources/application.yml` 中已由外部配置/Nacos 管理的 `dbflow.*` 注释块。
- 更新部署文档、Nacos 配置文档和观测文档中的相关命令与配置边界。

## Non-Scope

- 不改变初始化管理员、Nacos、MCP endpoint 或 datasource 的运行时语义。
- 不引入 dotenv loader，也不要求 Spring Boot 自动读取 `.env`。
- 不写入真实密码、Token、Token pepper、Nacos 密码或数据库连接密钥。

## Acceptance Criteria

- [x] 可通过一条 Maven 测试命令生成 `DBFLOW_ADMIN_INITIAL_PASSWORD_HASH`，且默认 `./mvnw test` 不强制生成。
- [x] `.env.example` 覆盖部署文档和 Nacos 模板引用的主要环境变量，所有敏感值均为占位符。
- [x] `src/main/resources/application.yml` 只保留本地默认启动配置和外部化配置指引，不再保留大段 `dbflow.*` 示例。
- [x] `docs/deployment/README.md` 和 `docs/deployment/nacos-config.md` 明确 hash 生成和 shell 引号注意事项。
- [x] `git diff --check`、`python3 scripts/check_harness.py` 和 `./mvnw test` 通过。

## Evidence Log

- 2026-04-30: Baseline `python3 scripts/check_harness.py` passed.
- 2026-04-30: Baseline `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
- 2026-04-30: Added `AdminPasswordHashGeneratorTests`, gated by `-Ddbflow.generate-admin-password-hash=true`.
- 2026-04-30: Expanded `.env.example` to shell-safe quoted values for JDBC URLs and display names, plus metadata,
  Nacos, target datasource, initial admin, MCP endpoint, and smoke-token placeholders.
- 2026-04-30: Removed the large commented `dbflow.*` example from `src/main/resources/application.yml` and replaced it
  with an external configuration pointer.
- 2026-04-30: Updated deployment and Nacos docs with the hash generation command, `DBFLOW_ADMIN_INITIAL_PASSWORD_HASH`
  quoting guidance, and `.env.example` loading caveat.
- 2026-04-30: Verified `.env.example` can be sourced by zsh without JDBC URL shell parsing errors.
- 2026-04-30: Verified hash generation command passed and printed `DBFLOW_ADMIN_INITIAL_PASSWORD_HASH='...'` using a
  fake local password.
- 2026-04-30: Verified default `./mvnw -Dtest=AdminPasswordHashGeneratorTests test` skips the generator test.
- 2026-04-30: `git diff --check` passed.
- 2026-04-30: `python3 scripts/check_harness.py` passed.
- 2026-04-30: `./mvnw test` passed with `Tests run: 136, Failures: 0, Errors: 0, Skipped: 11`.
