# 2026-04-30 Nacos Config Comments

## Objective

为 `docs/deployment/nacos-config.md` 中可复制的 Nacos YAML 模板补全注释，让部署者能直接理解每个配置块的职责和敏感值注入边界。

## Scope

- 只修改 `docs/deployment/nacos-config.md` 的配置说明和 YAML 示例注释。
- 更新 `docs/PLANS.md` 登记和归档本次文档增强。

## Non-Scope

- 不修改 `src/main/resources/application-nacos.yml`。
- 不修改运行时配置绑定或 Spring/Nacos 行为。
- 不写入真实数据库密码、Nacos 密码、Token pepper 或管理员密码。

## Acceptance Criteria

- [x] `refinex-dbflow.yml` 示例包含关键配置块注释。
- [x] `refinex-dbflow-nacos.yml` 示例包含 metadata datasource、server、target project/env、policy whitelist 注释。
- [x] 注释不破坏 YAML 可复制性，示例仍只使用占位符。
- [x] `git diff --check`、`python3 scripts/check_harness.py` 和 `./mvnw test` 通过。

## Evidence Log

- 2026-04-30: Added comments to `refinex-dbflow.yml` shared Nacos YAML for JPA/Flyway, Actuator, target datasource
  defaults, dangerous SQL defaults, initial admin, MCP token pepper, Origin, request size, and rate limit.
- 2026-04-30: Added comments to `refinex-dbflow-nacos.yml` profile YAML for metadata datasource, server forwarding,
  target project/env, and DROP whitelist.
- 2026-04-30: Verified comment coverage with `rg` over `docs/deployment/nacos-config.md`.
- 2026-04-30: `git diff --check` passed.
- 2026-04-30: `python3 scripts/check_harness.py` passed.
- 2026-04-30: `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
