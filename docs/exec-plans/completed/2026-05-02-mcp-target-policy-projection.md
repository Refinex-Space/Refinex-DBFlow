# Fix Plan: MCP Target And Policy Projection

Created: 2026-05-02
Completed: 2026-05-02
Status: Completed
Author: agent
Type: fix

## Bug Brief

**Symptom**: `dbflow_list_targets` 返回空 `targets`，`dbflow_get_effective_policy` 返回固定默认策略和空白名单，导致 MCP
客户端无法发现当前授权用户实际可见的项目环境，也无法看到真实生效的危险 DDL 配置。
**Expected**: MCP 目标列表应基于当前 Bearer Token 认证主体、元数据库 grant 和 YAML/Nacos 项目环境配置计算可见目标；策略视图应返回当前配置中的危险
DDL 默认策略和匹配白名单，且不得向未授权主体泄露目标策略。
**Severity**: Functional bug
**Type**: Skeleton leakage after backend capability completion

### Reproduction

1. 配置 `dbflow.projects[*].environments[*]`。
2. 创建用户、Token，并授予其中一个项目环境。
3. 通过 MCP 调用 `dbflow_list_targets`。
4. 观察响应中的 `targets` 为空。

Additional scan result:

- `dbflow_inspect_schema`, `dbflow_explain_sql`, `dbflow_execute_sql`, `dbflow_confirm_sql` 已调用真实 service。
- `dbflow_get_effective_policy` 仍返回静态默认值和空白名单。
- `dbflow://targets` 与 `dbflow://projects/{project}/envs/{env}/policy` resources 存在同类占位响应。

## Root Cause

MCP surface skeleton 阶段先固定了工具和 resource 名称，后续 schema inspect、explain、execute、confirm 已逐步接入真实能力，但
target catalog 和 policy projection 没有同步替换占位响应。

## Fix

**Strategy**: 新增共享 MCP 投影服务，复用 `ProjectEnvironmentCatalogService`、`McpAccessBoundaryService` 和
`DbflowProperties`，为 tools/resources 统一输出授权后的 target 列表和真实危险 DDL 策略视图。
**Files**:

- `src/main/java/com/refinex/dbflow/mcp/support/McpTargetPolicyProjectionService.java`
- `src/main/java/com/refinex/dbflow/mcp/tool/DbflowMcpTools.java`
- `src/main/java/com/refinex/dbflow/mcp/resource/DbflowMcpResources.java`
- `src/test/java/com/refinex/dbflow/mcp/DbflowMcpTargetPolicyProjectionTests.java`
- `docs/PLANS.md`

**Risk**: 目标列表若直接暴露 JDBC URL 会增加信息泄露面；本修复只返回项目/环境标识、展示名、驱动、用户名和元数据同步状态，不返回密码。

## Steps

### Step 1: Add regression coverage

**Files:** `src/test/java/com/refinex/dbflow/mcp/DbflowMcpTargetPolicyProjectionTests.java`
**Verification:** 授权用户调用 list targets 时必须看到已授权环境，未授权环境不可见；policy 视图必须返回配置白名单。

Status: ✅ Done
Evidence: `./mvnw -Dtest=DbflowMcpTargetPolicyProjectionTests test` 通过，覆盖授权目标列表、真实白名单投影和未授权策略隐藏。
Deviations: None.

### Step 2: Implement shared projection

**Files:** `src/main/java/com/refinex/dbflow/mcp/support/McpTargetPolicyProjectionService.java`
**Verification:** tools 与 resources 复用同一投影输出。

Status: ✅ Done
Evidence: 新增 `McpTargetPolicyProjectionService`，统一输出 visible targets 和 effective dangerous DDL policy。
Deviations: None.

### Step 3: Wire tools/resources and refresh descriptions

**Files:** `DbflowMcpTools.java`, `DbflowMcpResources.java`
**Verification:** 不再出现 target/policy skeleton 空列表占位。

Status: ✅ Done
Evidence: `DbflowMcpTools` 与 `DbflowMcpResources` 已复用共享投影服务，删除原静态空 targets/whitelist 占位。
Deviations: None.

### Step 4: Verify and archive

**Verification:** targeted test, full test suite, Harness validation, diff check.

Status: ✅ Done
Evidence: `./mvnw test` 通过，162 tests / 0 failures / 0 errors / 10 skipped。
Deviations: None.

## Verification

- [x] Reproduction test now passes
- [x] Regression test added and passes
- [x] Full test suite passes
- [x] Harness validation passes
- [x] Diff reviewed — only fix-related changes present

## Completion Summary

Completed: 2026-05-02
Root cause: MCP surface skeleton 完成后，target catalog 与 policy projection 未随 schema/explain/execute/confirm
一起接入真实能力。
Fix: 新增共享投影服务，`dbflow_list_targets` 基于当前 MCP 主体授权返回真实配置目标；`dbflow_get_effective_policy` 返回真实危险
DDL defaults 与匹配 whitelist，并在未授权时隐藏策略详情；同类 resource 输出同步修复。
Regression test: `DbflowMcpTargetPolicyProjectionTests`
Verification: `./mvnw -Dtest=DbflowMcpTargetPolicyProjectionTests test`; `./mvnw test`;
`python3 scripts/check_harness.py`; `git diff --check`
