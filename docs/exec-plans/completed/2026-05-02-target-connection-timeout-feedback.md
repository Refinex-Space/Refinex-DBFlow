# Fix Plan: Target Connection Timeout Feedback

Created: 2026-05-02
Completed: 2026-05-02
Status: Completed
Author: agent
Type: fix

## Bug Brief

**Symptom**: Codex 调用 `dbflow_inspect_schema` 查询表元数据时，已通过 project/env 授权，但目标库不可达，MCP 响应等待约 30
秒后返回 `08S01` / `Connection is not available, request timed out after 30001ms`；后端日志出现
`dbflow-target-...:connection-adder` 的 `Communications link failure`。
**Expected**: 首次真实访问不可达目标库时必须失败，但不应让 Codex 交互等待 Hikari 默认 30 秒；dev
模板和代码默认值应提供更短、更可诊断的失败窗口，并明确这是运行期目标库连接失败，不代表 schema/table 为空。
**Severity**: Degraded UX / operational feedback
**Type**: Runtime configuration default bug

### Reproduction

1. 使用 dev Nacos 模板中的 `dbflow.datasource-defaults.hikari.connection-timeout=30s`。
2. 配置一个 project/env 指向当前不可达的 MySQL。
3. 授权 MCP 用户访问该 project/env。
4. 调用 `dbflow_inspect_schema`。
5. 观察 MCP 响应约 30 秒后失败，错误消息包含 `request timed out after 30001ms`。

Evidence:

- 用户实际 Codex 测试返回 `project/env 可访问：dbflow_dev / dev`，但 `dbflow_inspect_schema` 在目标库连接阶段失败。
- 用户日志显示 `Connection refused`，说明根因是目标 MySQL 不可达；30 秒等待来自 Hikari `connection-timeout`。
- 当前 dev Nacos 模板显式配置 `connection-timeout: 30s`。

## Root Cause

`validate-on-startup=false` 已避免应用启动阶段主动连接目标库，但真实 `inspect_schema` 请求仍必须从 Hikari pool
获取目标库连接。dev 模板和代码默认值沿用 Hikari 30 秒获取连接等待时间，导致不可达目标库在 MCP 交互中表现为长时间卡顿后失败。

## Fix

**Strategy**: 将 DBFlow target Hikari `connection-timeout` 代码默认值和 dev Nacos 模板默认值收敛到 5 秒，并更新
runbook/observability 文档说明 30001ms 表示仍在使用旧 30 秒配置或运行时配置未刷新。
**Files**:

- `src/main/java/com/refinex/dbflow/config/properties/DbflowProperties.java`
- `src/test/java/com/refinex/dbflow/config/DbflowPropertiesTests.java`
- `docs/deployment/nacos/dev/application-dbflow.yml`
- `docs/OBSERVABILITY.md`
- `docs/runbooks/troubleshooting.md`
- `docs/PLANS.md`

**Risk**: 非 dev 环境如果确实需要更长的跨网段连接等待，仍可在外部配置中显式覆盖
`dbflow.datasource-defaults.hikari.connection-timeout`。

## Steps

### Step 1: Add regression coverage

**Files:** `DbflowPropertiesTests.java`
**Verification:** 无显式配置时，target Hikari connection timeout 默认应为 5 秒。

Status: ✅ Done
Evidence: 修复前运行 `./mvnw -Dtest=DbflowPropertiesTests#shouldProvideInteractiveTargetConnectionTimeoutDefault test`
失败，实际默认值为 `null`，会落到 Hikari 30 秒默认等待。
Deviations: None.

### Step 2: Update defaults and docs

**Files:** `DbflowProperties.java`, dev Nacos YAML, troubleshooting/observability docs
**Verification:** 配置绑定测试和 Hikari registry 相关测试通过。

Status: ✅ Done
Evidence: `DbflowProperties` 默认 `connectionTimeout=5s`；dev Nacos YAML、Observability 和 troubleshooting runbook 已同步说明。
Deviations: None.

### Step 3: Verify and archive

**Verification:** targeted tests, full test suite, Harness validation, diff check.

Status: ✅ Done
Evidence: `./mvnw -Dtest=DbflowPropertiesTests,HikariDataSourceRegistryTests,SchemaInspectServiceTests test` 和
`./mvnw test` 通过。
Deviations: None.

## Verification

- [x] Reproduction test now passes
- [x] Regression test added and passes
- [x] Full test suite passes
- [x] Harness validation passes
- [x] Diff reviewed — only fix-related changes present

## Completion Summary

Completed: 2026-05-02
Root cause: `validate-on-startup=false` 只避免启动期目标库连接；运行期 `inspect_schema` 仍需要真实获取目标库连接，而
DBFlow 没有配置自己的连接获取默认等待时间，dev 模板还显式沿用 30 秒。
Fix: DBFlow target Hikari `connectionTimeout` 默认改为 5 秒，dev Nacos 模板同步为 `5s`，runbook 说明 `30001ms` 代表旧 30
秒配置或运行配置未生效。
Regression test: `DbflowPropertiesTests#shouldProvideInteractiveTargetConnectionTimeoutDefault`
Verification: `./mvnw clean -Dtest=DbflowPropertiesTests#shouldProvideInteractiveTargetConnectionTimeoutDefault test`;
`./mvnw -Dtest=DbflowPropertiesTests,HikariDataSourceRegistryTests,SchemaInspectServiceTests test`; `./mvnw test`;
`python3 scripts/check_harness.py`; `git diff --check`
