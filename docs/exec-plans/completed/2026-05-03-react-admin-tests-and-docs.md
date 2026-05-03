# Execution Plan: React Admin Tests And Docs

Created: 2026-05-03
Status: Completed
Author: agent

## Objective

为 React admin 增加基础组件/页面状态测试，并补充新后台开发、部署、验证和 cutover 文档。

## Scope

**In scope:**

- `dbflow-admin/src/test/` 与必要的 `dbflow-admin/src/test-utils/`
- `dbflow-admin/src/components/dbflow/*.test.tsx`
- `dbflow-admin/src/features/auth/login-page.test.tsx`
- `dbflow-admin/src/features/tokens/components/token-reveal-dialog.test.tsx`
- `dbflow-admin/vite.config.ts`
- `docs/user-guide/admin-guide.md`
- `docs/deployment/README.md`
- `docs/OBSERVABILITY.md`
- `README.md`

**Out of scope:**

- 完整 E2E 测试
- 后端登录、CSRF、session 或 Token 业务逻辑改动
- React admin 视觉重设计
- 真实密码、Token、Nacos 密码或数据库密码示例

## Constraints

- 使用 Vitest，且 `pnpm --dir dbflow-admin test` 必须稳定通过。
- 可以 mock API client，但测试必须验证组件真实行为，不测试 mock 本身。
- 管理端安全文档必须说明 `/admin` 与 `/admin-next` cutover 状态。
- Token 明文只能作为一次性展示语义说明，不得写真实 Token。
- 完成前使用 `harness-verify` 语义做 fresh verification。

## Acceptance Criteria

- [x] AC-1: `RiskBadge` 与 `DecisionBadge` 组件测试覆盖已知值和未知值渲染。
- [x] AC-2: `TokenRevealDialog` 测试覆盖复制行为与关闭后由父组件清空明文状态。
- [x] AC-3: `LoginPage` 测试覆盖 API client mock 下的错误状态展示与不写入 session。
- [x] AC-4: 新增 `dbflow-admin/src/test/setup.ts` 并在 Vitest 配置中加载。
- [x] AC-5: 管理端文档覆盖后端/前端开发启动、`/admin-next` 入口、React admin 打包命令、登录/CSRF/session、Token 明文一次性展示、
  `/admin` 和 `/admin-next` cutover。
- [x] AC-6: 文档不包含真实密码、Token、Nacos 密码或数据库密码。
- [x] AC-7: `pnpm --dir dbflow-admin test`、`pnpm --dir dbflow-admin build`、`python3 scripts/check_harness.py` 通过。

## Risk Notes

| Risk                                      | Likelihood | Mitigation                                   |
|-------------------------------------------|------------|----------------------------------------------|
| 现有 browser-mode Vitest 对组件测试依赖 Playwright | Medium     | 保持默认 headless browser 配置；如不稳定再按项目实际改为 jsdom。 |
| Dialog/clipboard 测试过度依赖实现细节               | Medium     | 通过用户可见按钮、文本和 clipboard API 断言行为。             |
| 文档示例误写真实敏感信息                              | Low        | 只使用占位符，并用敏感关键词扫描做检查。                         |

## Implementation Steps

### Step 1: Add React admin test setup and badge tests

**Files:** `dbflow-admin/src/test/setup.ts`, `dbflow-admin/vite.config.ts`,
`dbflow-admin/src/components/dbflow/risk-badge.test.tsx`, `dbflow-admin/src/components/dbflow/decision-badge.test.tsx`
**Verification:** targeted Vitest component tests pass.

Status: ✅ Done
Evidence:
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/components/dbflow/risk-badge.test.tsx src/components/dbflow/decision-badge.test.tsx src/features/tokens/components/token-reveal-dialog.test.tsx src/features/auth/login-page.test.tsx`
passed 4 files / 17 tests.
Deviations:

### Step 2: Add Token reveal and login state tests

**Files:** `dbflow-admin/src/features/tokens/components/token-reveal-dialog.test.tsx`,
`dbflow-admin/src/features/auth/login-page.test.tsx`, `dbflow-admin/src/test-utils/*`
**Verification:** targeted Vitest auth/token tests pass.

Status: ✅ Done
Evidence:
`pnpm --dir dbflow-admin exec vitest run --browser.headless src/components/dbflow/risk-badge.test.tsx src/components/dbflow/decision-badge.test.tsx src/features/tokens/components/token-reveal-dialog.test.tsx src/features/auth/login-page.test.tsx`
passed 4 files / 17 tests.
Deviations:

### Step 3: Update admin development and deployment docs

**Files:** `docs/user-guide/admin-guide.md`, `docs/deployment/README.md`, `docs/OBSERVABILITY.md`, `README.md`
**Verification:** secret-text scan and Harness validation pass.

Status: ✅ Done
Evidence: Secret placeholder scan over `README.md`, `docs/user-guide/admin-guide.md`, `docs/deployment/README.md`, and
`docs/OBSERVABILITY.md` found no real password/Token/Nacos password patterns.
Deviations:

### Step 4: Run final verification and archive plan

**Files:** `docs/exec-plans/active/2026-05-03-react-admin-tests-and-docs.md`,
`docs/exec-plans/completed/2026-05-03-react-admin-tests-and-docs.md`, `docs/PLANS.md`
**Verification:** `pnpm --dir dbflow-admin test`, `pnpm --dir dbflow-admin build`, `python3 scripts/check_harness.py`,
`git diff --check`.

Status: ✅ Done
Evidence: `pnpm --dir dbflow-admin test` passed 44 files / 194 tests; `pnpm --dir dbflow-admin build` passed;
`python3 scripts/check_harness.py` passed; `git diff --check` passed.
Deviations:

## Progress Log

| Step | Status | Evidence                                           | Notes                                                                                                            |
|------|--------|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| 1    | ✅      | Targeted Vitest command passed 4 files / 17 tests. | Added `src/test/setup.ts`, badge tests, and test utils.                                                          |
| 2    | ✅      | Targeted Vitest command passed 4 files / 17 tests. | Added Token reveal dialog test and strengthened login failure test.                                              |
| 3    | ✅      | Secret placeholder scan passed.                    | Documented dev startup, `/admin-next`, packaging, login/CSRF/session, one-time Token reveal, and cutover status. |
| 4    | ✅      | Full verification passed.                          | Build emitted Vite chunk-size warnings for existing large frontend chunks, but exited 0.                         |

## Decision Log

| Decision                          | Context                                          | Alternatives Considered | Rationale                         |
|-----------------------------------|--------------------------------------------------|-------------------------|-----------------------------------|
| 保持 Vitest browser-mode 作为默认测试运行方式 | 当前 `pnpm --dir dbflow-admin test` 已稳定通过 41 个测试文件 | 立即迁移到 jsdom             | 避免无必要地重写现有测试运行模型；仅补 `setupFiles`。 |

## Completion Summary

Completed: 2026-05-03
Duration: 4 steps
All acceptance criteria: PASS

Summary: Added Vitest setup, DBFlow badge component tests, Token reveal dialog copy/clear coverage, and strengthened
login error-state coverage. Updated admin/deployment/observability/README docs for React admin development,
`/admin-next`, packaging, login/CSRF/session, one-time Token plaintext, and `/admin` to `/admin-next` cutover state.
