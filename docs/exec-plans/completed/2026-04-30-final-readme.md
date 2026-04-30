# 2026-04-30 Final README

## Objective

将根目录 `README.md` 从占位标题整理为项目最终入口文档，明确 Refinex-DBFlow 的定位、能力边界、快速启动、部署入口、
管理端/MCP 使用入口和关键文档索引。

## Scope

- 重写根目录 `README.md`。
- 在 README 中提供高频命令、核心能力、安全边界、运行入口、文档导航和开发维护入口。
- 更新 `docs/PLANS.md` 登记本次 README 收口工作。

## Non-Scope

- 不修改生产代码、配置绑定或运行时行为。
- 不新增部署步骤；README 只做入口摘要，详细步骤链接到既有文档。
- 不写入真实 Token、数据库密码、Nacos 密码或其它密钥。

## Acceptance Criteria

- [x] `README.md` 能让新读者在第一屏理解项目是什么、面向谁、解决什么问题。
- [x] `README.md` 包含本地启动、测试、构建、管理端和 MCP endpoint 的最小入口。
- [x] `README.md` 在合适位置索引部署、Nacos、元数据库、MCP 客户端、管理员、员工、审计安全、排障、架构、观测和 Harness 文档。
- [x] `README.md` 明确敏感信息边界和高危 SQL 策略，不误导用户把密码或 Token 写入仓库。
- [x] `git diff --check`、`python3 scripts/check_harness.py` 和 `./mvnw test` 通过。

## Evidence Log

- 2026-04-30: Read current `README.md`, `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, deployment guide, MCP client
  guide, and repository document inventory.
- 2026-04-30: Rewrote `README.md` with project positioning, fit/non-fit cases, core capabilities, technology stack,
  quick start, common commands, configuration/secret boundaries, MCP tool surface, documentation navigation, repository
  map, and development conventions.
- 2026-04-30: Verified README local links with `perl` extraction plus filesystem existence check.
- 2026-04-30: `git diff --check` passed.
- 2026-04-30: `python3 scripts/check_harness.py` passed.
- 2026-04-30: `./mvnw test` passed with `Tests run: 136, Failures: 0, Errors: 0, Skipped: 11`.
