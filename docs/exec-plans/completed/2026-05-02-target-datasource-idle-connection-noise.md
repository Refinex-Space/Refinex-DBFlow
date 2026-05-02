# Fix Plan: Target Datasource Idle Connection Noise

Created: 2026-05-02
Completed: 2026-05-02
Status: Completed
Author: agent
Type: fix

## Bug Brief

**Symptom**: 应用启动后，目标库 Hikari pool 在后台 `connection-adder` 线程持续尝试连接不可达 MySQL，并输出 `Pool is empty, failed to create/setup connection` / `Communications link failure` / `Connection refused`。
**Expected**: 当 `dbflow.datasource-defaults.validate-on-startup=false` 时，本地或离线启动不应主动触碰目标业务库；只有首次 SQL/EXPLAIN/schema inspect 或显式 reload 预热时才连接目标库。
**Severity**: Degraded
**Type**: New bug / Environment-specific

### Reproduction

1. 配置至少一个 `dbflow.projects[*].environments[*]` 指向当前不可达的 MySQL。
2. 保持 `dbflow.datasource-defaults.validate-on-startup=false`。
3. 设置 `dbflow.datasource-defaults.hikari.minimum-idle=1`。
4. 启动应用，观察 Hikari 后台建连线程日志。

Reproduction evidence:

- 用户提供的启动后日志显示 `dbflow-target-dbflow_dev-dev:connection-adder` 在非请求线程中尝试连接目标库并失败。
- 修复前新增回归测试失败：`./mvnw -Dtest=HikariDataSourceRegistryTests#shouldNotOpenBackgroundConnectionsWhenStartupValidationDisabled test`，断言期望连接尝试次数为 `0`，实际为 `6`。

## Root Cause

**Mechanism**: Hikari 的负数 `initializationFailTimeout` 只跳过启动线程里的初始连接尝试，但 pool 仍会按 `minimumIdle` 在后台尝试补足空闲连接。DBFlow 在 `validate-on-startup=false` 时仍把配置的 `minimum-idle=1` 原样写入 Hikari，导致启动后异步连接不可达目标库。
**Introduced by**: Hikari 目标库注册表实现把“是否阻断启动”和“是否建立后台 idle 连接”混为一个开关处理。
**Why it wasn't caught**: 现有测试只断言关闭启动校验时 Spring context 不失败，没有断言启动后不能发生后台目标库连接尝试。

## Hypothesis Log

### Hypothesis #1: Nacos 或 metadata datasource 触发了 MySQL 连接

Prediction: 如果是 Nacos 或 metadata datasource，日志 pool name 不会是 `dbflow-target-<project>-<env>`。
Experiment: 对照日志 pool name 与 `HikariDataSourceRegistry.poolName` 生成规则，并搜索配置文档。
Result: 用户日志 pool name 为 `dbflow-target-dbflow_dev-dev`，与目标库 registry 命名规则一致。
Conclusion: REFUTED

### Hypothesis #2: `validate-on-startup=false` 只避免启动失败，不能避免 Hikari 后台补 idle 连接

Prediction: Hikari 文档会说明负数 `initializationFailTimeout` 跳过初始连接但仍后台获取连接；代码里若 `minimumIdle > 0`，仍会触发后台建连。
Experiment: 使用 Context7 查询 HikariCP 文档并检查 `HikariDataSourceRegistry.buildConfig`。
Result: HikariCP 文档确认负数 `initializationFailTimeout` 会立即启动 pool，同时后台尝试获取连接；代码在关闭启动校验时仍写入配置的 `minimumIdle`。
Conclusion: CONFIRMED

## Fix

**Strategy**: 当启动期连接校验未启用时，把目标库 Hikari 的 effective `minimumIdle` 强制设为 `0`，使 pool 懒连接；当启动校验或候选 reload 预热启用时保留配置的 `minimumIdle` 和强制连接验证语义。
**Files**:

- `src/main/java/com/refinex/dbflow/executor/datasource/HikariDataSourceRegistry.java`
- `src/test/java/com/refinex/dbflow/executor/HikariDataSourceRegistryTests.java`
- `docs/deployment/nacos/dev/application-dbflow.yml`
- `docs/OBSERVABILITY.md`
- `docs/runbooks/troubleshooting.md`
- `docs/PLANS.md`

**Risk**: 配置展示中的 `minimum-idle` 仍是用户配置值，而实际 Hikari pool 在本地懒连接模式下为 0；文档已明确 effective 行为。

### Steps

#### Step 1: Add regression reproduction

**Files:** `src/test/java/com/refinex/dbflow/executor/HikariDataSourceRegistryTests.java`
**Verification:** 新测试在当前实现下能暴露关闭启动校验时仍发生后台连接尝试。

Status: ✅ Done
Evidence: 修复前运行 `./mvnw -Dtest=HikariDataSourceRegistryTests#shouldNotOpenBackgroundConnectionsWhenStartupValidationDisabled test` 失败，实际连接尝试次数为 `6`。
Deviations: None.

#### Step 2: Apply minimal Hikari configuration fix

**Files:** `src/main/java/com/refinex/dbflow/executor/datasource/HikariDataSourceRegistry.java`
**Verification:** 回归测试通过；开启启动校验时原测试仍保持 fail-fast。

Status: ✅ Done
Evidence: `./mvnw -Dtest=HikariDataSourceRegistryTests test` 通过，7 tests / 0 failures / 0 errors / 0 skipped。
Deviations: None.

#### Step 3: Sync operator documentation

**Files:** `docs/deployment/nacos/dev/application-dbflow.yml`, `docs/OBSERVABILITY.md`, `docs/runbooks/troubleshooting.md`
**Verification:** 文档说明 `validate-on-startup=false` 时 effective `minimumIdle=0`。

Status: ✅ Done
Evidence: 文档同步说明关闭启动校验时 DBFlow 会将目标 Hikari pool 的实际 `minimumIdle` 降为 `0`。
Deviations: None.

## Verification

- [x] Reproduction test now passes
- [x] Regression test added and passes
- [x] Full test suite passes
- [x] Harness validation passes
- [x] Diff reviewed — only fix-related changes present
- [x] Pre-existing failures unchanged

## Progress Log

| Step       | Status | Evidence | Notes |
| ---------- | ------ | -------- | ----- |
| Reproduce  | ✅     | 修复前回归测试失败：expected `0`, actual `6` | 用户日志对应同一机制 |
| Root cause | ✅     | Context7 HikariCP docs + local code trace | `initializationFailTimeout=-1` 仍会后台建连 |
| Fix        | ✅     | `effectiveMinimumIdle(...)=0` when validation disabled | reload/validation enabled path unchanged |
| Verify     | ✅     | `./mvnw test`; Harness; `git diff --check` | 159 tests passed |
| Regression | ✅     | `shouldNotOpenBackgroundConnectionsWhenStartupValidationDisabled` | 覆盖异步连接尝试 |

## Completion Summary

Completed: 2026-05-02
Root cause: `validate-on-startup=false` 只关闭 Hikari 初始化失败阻断，但 `minimumIdle=1` 仍驱动后台 idle 连接创建。
Fix: 启动校验关闭时将目标 Hikari pool 实际 `minimumIdle` 设为 `0`；启动校验或 reload 预热时保留配置值。
Regression test: `HikariDataSourceRegistryTests#shouldNotOpenBackgroundConnectionsWhenStartupValidationDisabled`
All verification criteria: PASS

Summary: 已修复启动后不可达目标 MySQL 反复刷 `connection-adder` 告警的问题，同时保留生产 fail-fast 和 reload 候选预热语义。
