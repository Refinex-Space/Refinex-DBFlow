# 2026-04-30 Deployment Doc Simplification

## Objective

把部署文档从多文件、多环境变量、多示例占位的形态收敛为低阅读成本路径：`.env.example` 只保留启动 Nacos profile 所需变量，
部署目录直接提供可复制的 dev YAML，主部署 README 一页讲清启动、Nacos 配置、初始账号和安全边界。

## Scope

- 简化 `.env.example`。
- 删除 `docs/deployment/application-dbflow-example.yml`、`metadata-database.md`、`nacos-config.md`。
- 新增 `docs/deployment/nacos/dev/application-dbflow.yml` 和 `docs/deployment/nacos/dev/refinex-dbflow-nacos.yml`。
- 重写 `docs/deployment/README.md`，移除散落式外部配置章节和冗余 hash 引导。
- 更新根 `README.md`、`docs/ARCHITECTURE.md`、`docs/OBSERVABILITY.md` 和 `docs/PLANS.md` 的文档索引。

## Non-Scope

- 不改变 MCP、SQL 执行、审计、授权和策略运行时逻辑。
- 不把真实业务数据库连接信息、Token 或 Nacos 密码写入仓库。
- 不要求用户提前配置示例 project/env；目标库由用户按需在 YAML 中添加。

## Acceptance Criteria

- [x] `.env.example` 不再包含 demo project/env、target datasource、管理员 hash 生成相关变量。
- [x] `docs/deployment/README.md` 不再要求用户阅读多个 deployment 子文档才能启动。
- [x] `docs/deployment/nacos/dev/` 下存在可复制的 `application-dbflow.yml` 和 `refinex-dbflow-nacos.yml`。
- [x] dev 配置默认初始化 `admin/admin`，并提示首次登录后修改密码。
- [x] 已删除冗余 deployment 子文档和旧 example YAML，并修正所有引用。
- [x] `git diff --check`、`python3 scripts/check_harness.py` 和 `./mvnw test` 通过。

## Evidence Log

- 2026-04-30: Confirmed current deployment docs reference `application-dbflow-example.yml`, `metadata-database.md`,
  `nacos-config.md`, demo project/env variables, and admin password hash variables in multiple places.
- 2026-04-30: Replaced `.env.example` with Nacos profile startup variables only.
- 2026-04-30: Added `docs/deployment/nacos/dev/application-dbflow.yml` and
  `docs/deployment/nacos/dev/refinex-dbflow-nacos.yml` with `admin/admin`, empty `projects: []`, safe default
  dangerous DDL policy, and dev-only MCP pepper.
- 2026-04-30: Rewrote `docs/deployment/README.md` into one deployment entrypoint and removed separate metadata/Nacos
  child-doc requirements.
- 2026-04-30: Deleted `docs/deployment/application-dbflow-example.yml`, `docs/deployment/metadata-database.md`,
  `docs/deployment/nacos-config.md`, and the no-longer-documented admin hash generator test.
- 2026-04-30: Updated root README, architecture, observability, and test placeholders to remove current references to
  deleted deployment docs and demo environment variables.
- 2026-04-30: Verified current documentation links with a Markdown link existence script.
- 2026-04-30: `git diff --check` passed.
- 2026-04-30: `python3 scripts/check_harness.py` passed.
- 2026-04-30: `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
