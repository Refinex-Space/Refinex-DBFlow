# Execution Plan: Admin Operations Pages

Created: 2026-04-29
Status: Completed
Author: agent

## Objective

将管理端审计、危险策略、系统健康页面从演示数据改为真实只读视图，并补齐权限、过滤、分页、脱敏和页面渲染测试。

## Scope

**In scope:**

- `src/main/java/com/refinex/dbflow/admin`
- `src/main/resources/templates/admin`
- `src/test/java/com/refinex/dbflow/admin`
- `docs/PLANS.md`

**Out of scope:**

- 危险策略配置写入能力；MVP 只读展示 YAML/Nacos 生效配置。
- 独立前端 SPA 或 Node/Vite 构建链。
- 对目标数据库执行额外业务 SQL 做深度健康探测。

## Constraints

- 管理端页面必须继续走 Spring MVC + Thymeleaf + Spring Security form login。
- 普通用户不能访问全量审计、危险策略和健康页面。
- 审计详情不能展示敏感 Token、数据库密码或完整结果集。
- 危险策略页面只读，配置来源仍以 YAML/Nacos 为准。

## Acceptance Criteria

- [x] AC-1: `/admin/audit` 支持按 userId、project、env、risk、decision、sqlHash、tool 过滤，支持分页，并渲染真实审计列表。
- [x] AC-2: `/admin/audit/{id}` 渲染真实审计详情，展示拒绝原因/错误摘要且对敏感信息脱敏。
- [x] AC-3: `/admin/policies/dangerous` 只读展示 DROP 白名单、TRUNCATE confirmation 策略和 prod 强化规则。
- [x] AC-4: `/admin/health` 展示元数据库、项目环境连接池、Nacos、MCP endpoint 状态，且不暴露连接串密码。
- [x] AC-5: 测试覆盖权限、分页、过滤和页面渲染，`./mvnw test` 通过。

## Risk Notes

| Risk                    | Likelihood | Mitigation                              |
|-------------------------|------------|-----------------------------------------|
| 健康页面为了探测目标库而触发慢连接       | Medium     | 只读取 Hikari 池状态和配置目录，避免主动执行目标库业务 SQL     |
| 审计详情模板误展示敏感字段           | Medium     | 复用 `AuditQueryService` 已脱敏 DTO，并增加页面级断言 |
| 原有管理端 smoke 测试依赖演示详情 ID | Medium     | 将详情链接改为列表驱动，并在测试中用真实审计事件 ID             |

## Implementation Steps

### Step 1: 建立管理端运维视图服务

**Files:** `src/main/java/com/refinex/dbflow/admin/AdminOperationsViewService.java`
**Verification:** `./mvnw -Dtest=AdminOperationsPageControllerTests test`

Status: ✅ Done
Evidence: Added `AdminOperationsViewService`; `./mvnw -DskipTests compile` passed.
Deviations:

### Step 2: 接线控制器与模板

**Files:** `src/main/java/com/refinex/dbflow/admin/AdminHomeController.java`,
`src/main/resources/templates/admin/*.html`
**Verification:** 管理端页面 smoke 覆盖审计、策略、健康页

Status: ✅ Done
Evidence: `AdminHomeController` delegates audit/policy/health pages to the new view service; Thymeleaf templates render
real page models.
Deviations:

### Step 3: 补充管理端页面测试

**Files:** `src/test/java/com/refinex/dbflow/admin/AdminOperationsPageControllerTests.java`,
`src/test/java/com/refinex/dbflow/admin/AdminUiControllerTests.java`
**Verification:** 权限、分页、过滤、脱敏、页面渲染断言通过

Status: ✅ Done
Evidence: `./mvnw -Dtest=AdminOperationsPageControllerTests,AdminUiControllerTests test` passed, 12 tests.
Deviations:

### Step 4: 全量验证并归档

**Files:** `docs/exec-plans/active/2026-04-29-admin-operations-pages.md`,
`docs/exec-plans/completed/2026-04-29-admin-operations-pages.md`, `docs/PLANS.md`
**Verification:** `python3 scripts/check_harness.py` 和 `./mvnw test`

Status: ✅ Done
Evidence: `python3 scripts/check_harness.py` passed; `./mvnw test` passed with 125 tests, 10 skipped.
Deviations:

## Progress Log

| Step | Status | Evidence                                                                       | Notes                                                            |
|------|--------|--------------------------------------------------------------------------------|------------------------------------------------------------------|
| 1    | ✅      | `./mvnw -DskipTests compile`                                                   | View service compiles.                                           |
| 2    | ✅      | Targeted page smoke tests                                                      | Controller and templates render real audit/policy/health models. |
| 3    | ✅      | `./mvnw -Dtest=AdminOperationsPageControllerTests,AdminUiControllerTests test` | 12 tests passed.                                                 |
| 4    | ✅      | `python3 scripts/check_harness.py`; `./mvnw test`                              | Harness green; Maven suite passed.                               |

## Decision Log

| Decision          | Context              | Alternatives Considered | Rationale                     |
|-------------------|----------------------|-------------------------|-------------------------------|
| 健康页不主动执行目标库业务 SQL | 目标环境可能很多，连接失败可能拖慢管理页 | 每次页面打开都执行连接校验           | MVP 只展示连接池与配置状态，避免后台页面造成目标库压力 |

## Completion Summary

Completed: 2026-04-29
Duration: 4 steps
All acceptance criteria: PASS

Summary:

- Implemented `AdminOperationsViewService` for management-side audit, dangerous policy, and system health page models.
- Wired `/admin/audit`, `/admin/audit/{eventId}`, `/admin/policies/dangerous`, and `/admin/health` to real read-only
  data instead of prototype rows.
- Added smoke/security/redaction tests for audit filtering, pagination, denied details, dangerous policy rendering, and
  health rendering.
- Verified with Harness and full Maven test suite.
