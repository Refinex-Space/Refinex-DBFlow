# React Admin Login Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `harness-execute` semantics to implement this plan task by task. Use `harness-dispatch` only for independent subtasks with disjoint write scopes. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 React `/login` 调整为中文、克制、清爽的 DBFlow Admin 登录入口，同时保持现有 Spring Security JSON 登录、CSRF、session store 和 redirect 行为不变。

**Architecture:** 本计划只触碰 React 登录页 surface 和对应 browser-mode Vitest 测试。认证 API、Session store、TanStack route guard、后端安全配置和 `/admin` 认证后页面保持不变。实现采用现有 shadcn/ui、Tailwind token 和 lucide-react，不引入新依赖或全局视觉重构。最终验收版本进一步收敛为左上角 logo + `DBFlow Admin`，表单上下左右居中。

**Tech Stack:** React 19、TypeScript、Vite、TanStack Router、react-hook-form、zod、shadcn/ui、Tailwind CSS、lucide-react、Vitest browser mode。

**Design Source:** `docs/exec-plans/specs/2026-05-03-react-admin-login-polish-design.md`

---

## Scope

**In scope:**

- 中文化 `dbflow-admin/src/features/auth/login-page.tsx` 的用户可见核心文案。
- 将左侧“大标题 + 三张说明卡”收敛为品牌、短中文标题、短说明和三条轻量安全信号。
- 降低登录页背景、卡片和信号区视觉重量，保持 warm neutral、轻边界、少阴影。
- 更新 `dbflow-admin/src/features/auth/login-page.test.tsx` 的中文断言，并保留登录成功、失败、session 写入、redirect 和密码显示切换行为覆盖。
- 运行 focused frontend test、frontend build、frontend lint、Harness validation 和 diff check。

**Out of scope:**

- 不修改 `dbflow-admin/src/api/session.ts`。
- 不修改 `dbflow-admin/src/stores/session-store.ts`。
- 不修改 `dbflow-admin/src/routes/(auth)/login.tsx`。
- 不修改后端 `src/main/java/**` 认证、CSRF、Session API 或 Spring Security 配置。
- 不新增 UI 组件库、字体、图片、动画库、OAuth、验证码、注册或忘记密码入口。
- 不改动 `/admin` 认证后页面、侧边栏、Dashboard 或其他业务页面。

## Decisions

- 采用 spec 中的方案 A：技术克制 + 中等密度中文登录面。
- 保留左右布局，但左侧说明内容必须轻量化；不得继续使用三张大说明卡堆叠。
- 继续使用 `Card` 承载登录表单，因为表单本身是交互面；左侧安全信号不使用重卡片样式。
- 所有前端 fallback 文案中文化；后端 API 错误消息继续透传，避免遮蔽服务端真实错误。
- 不改全局 CSS token，除非实现阶段发现现有 utility class 无法表达 spec 要求；即便修改，也只允许最小通用样式补丁。
- 不并行 dispatch：测试文件和实现文件互相依赖，最佳执行路径是串行红绿修改。

## Files

**Modify:**

- `dbflow-admin/src/features/auth/login-page.test.tsx` - 更新中文渲染、密码切换、错误和成功登录断言。
- `dbflow-admin/src/features/auth/login-page.tsx` - 更新文案、左侧 signal 数据、布局 utility class 和 toast fallback。
- `docs/PLANS.md` - 登记 active plan，完成时由 `harness-execute` 归档更新。
- `docs/exec-plans/active/2026-05-03-react-admin-login-polish.md` - 执行过程中更新 checkbox 与 evidence。

**Expected unchanged:**

- `dbflow-admin/src/api/session.ts`
- `dbflow-admin/src/stores/session-store.ts`
- `dbflow-admin/src/routes/(auth)/login.tsx`
- `src/main/java/**`

## Verification Strategy

Run during execution:

```bash
pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/auth/login-page.test.tsx
pnpm --dir dbflow-admin build
pnpm --dir dbflow-admin lint
python3 scripts/check_harness.py
git diff --check
```

Expected evidence:

- Focused login page test passes with中文文案和行为断言。
- Frontend build exits 0.
- Frontend lint exits 0.
- Harness validation exits 0.
- Diff check exits 0.

If implementation touches global styles, production route mapping, backend code, or packaged static resource behavior, also run:

```bash
./mvnw test
```

Expected: Maven test suite exits 0.

## Risks

| Risk | Priority | Mitigation |
| --- | --- | --- |
| Security regression from accidental auth API/session changes | High | Keep `api/session.ts`, session store, login route and backend security files out of scope; inspect final diff. |
| Correctness regression in redirect or failure behavior | High | Preserve existing success/failure tests and update only visible text assertions. |
| Accessibility regression for password toggle or labels | Medium | Assert Chinese accessible names for username, password, submit, show/hide password. |
| Visual work remains too text-heavy | Medium | Replace explanatory cards with short signal rows and keep title at `text-3xl`/`text-4xl`, not `md:text-5xl`. |
| Lint/build instability from Tailwind class formatting | Low | Run frontend lint and build before handoff. |

## Task 1: Update Login Page Tests For Chinese Contract

**Files:**

- Modify: `dbflow-admin/src/features/auth/login-page.test.tsx`

**Decision Trace:** Spec sections “具体文案决策”, “测试策略”, “验收标准 AC-1/AC-2/AC-4”.

- [x] **Step 1: Replace English rendering assertions with Chinese contract assertions**

Update the render test to assert:

- `DBFlow Admin` remains visible.
- `数据库操作网关` is visible.
- `登录后管理数据库访问边界` is visible.
- `服务端会话`, `策略前置`, and `审计留痕` are visible.
- username textbox uses accessible name `用户名`.
- password field uses accessible name `密码`.
- submit button uses accessible name `登录`.

- [x] **Step 2: Update password toggle test names**

Change the toggle test to use Chinese accessible names:

- initial button name: `显示密码`
- toggled button name: `隐藏密码`

Keep the existing `type='password'` to `type='text'` assertion.

- [x] **Step 3: Update generic failure and success toast expectations**

Keep the API-client error test proving backend messages are passed through. Update only frontend-owned fallback/success strings:

- generic fallback: `登录失败，请检查用户名和密码。`
- success toast: `已登录 DBFlow Admin`

- [x] **Step 4: Run focused test and record red/green evidence**

Run:

```bash
pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/auth/login-page.test.tsx
```

Expected before implementation: tests may fail on old English UI. Expected after Task 2: all tests in `login-page.test.tsx` pass.

**Evidence:**

- Red evidence before implementation: `pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/auth/login-page.test.tsx` exited 1; old English UI failed 5/5 tests against the new Chinese contract.
- Green evidence after Task 2: same command exited 0; `login-page.test.tsx` passed 5/5 tests.

## Task 2: Implement Chinese, Lighter Login Page

**Files:**

- Modify: `dbflow-admin/src/features/auth/login-page.tsx`

**Decision Trace:** Spec sections “采用设计”, “具体文案决策”, “布局与视觉规则”, “文件边界”.

- [x] **Step 1: Localize validation and toast fallback strings**

Update `loginSchema` validation messages:

- username: `请输入用户名。`
- password: `请输入密码。`

Update submit success toast to `已登录 DBFlow Admin`.

Update non-API fallback in `getLoginErrorMessage` to `登录失败，请检查用户名和密码。`.

Do not change `login(values.username, values.password)`, `setSession(session)`, or `navigate({to: normalizeRedirect(redirectTo), replace: true})`.

- [x] **Step 2: Replace governance signal data with short Chinese signals**

Replace `governanceSignals` with:

- `服务端会话` / `登录状态由 DBFlow 服务端校验。` / `KeyRound`
- `策略前置` / `授权与高危 SQL 策略在服务端执行。` / `Shield`
- `审计留痕` / `敏感操作可追踪，不暴露密钥明文。` / `Database`

Keep icon imports limited to existing `Database`, `KeyRound`, `Shield`, `Eye`, `EyeOff`, and `Loader2`.

- [x] **Step 3: Rework left-side content hierarchy**

Replace the current English heading area with:

- Brand eyebrow `Refinex-DBFlow`
- Product name `DBFlow Admin`
- Product positioning `数据库操作网关`
- Main title `登录后管理数据库访问边界`
- Description `统一管理用户、授权、Token、危险 SQL 策略和审计记录。`

Use `text-3xl` to `text-4xl` desktop sizing for the main title. Do not use `md:text-5xl`.

- [x] **Step 4: Decard and lighten signal rows**

Keep signal rows scannable but reduce card weight:

- avoid heavy shadow on each signal row;
- use whitespace, muted text, small icon cells and light border/divider treatment;
- keep each signal title and description short enough to read without wrapping aggressively on desktop.

Maintain mobile single-column flow without horizontal scrolling.

- [x] **Step 5: Localize login card controls**

Update the login card:

- title `管理员登录`
- description `使用 DBFlow 管理员账号进入控制台。`
- username label `用户名`
- username placeholder `admin`
- password label `密码`
- password placeholder `输入密码`
- password toggle sr-only `显示密码` / `隐藏密码`
- submit button `登录`

Keep `autoComplete='username'`, `autoComplete='current-password'`, disabled states, password visibility state, and `FormMessage` behavior.

- [x] **Step 6: Run focused test**

Run:

```bash
pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/auth/login-page.test.tsx
```

Expected: login page test file passes.

**Evidence:**

- Modified `dbflow-admin/src/features/auth/login-page.tsx` only for planned UI text, signal rows, layout utility classes, validation messages, password toggle sr-only labels, and toast fallback/success strings.
- `pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/auth/login-page.test.tsx` exited 0; 1 test file and 5 tests passed.

## Task 3: Final Verification And Handoff

**Files:**

- Modify: `docs/exec-plans/active/2026-05-03-react-admin-login-polish.md`
- Modify later during completion: `docs/PLANS.md`

**Decision Trace:** Spec sections “构建与质量验证”, “验收标准”.

- [x] **Step 1: Run frontend build**

Run:

```bash
pnpm --dir dbflow-admin build
```

Expected: TypeScript and Vite build exit 0.

- [x] **Step 2: Run frontend lint**

Run:

```bash
pnpm --dir dbflow-admin lint
```

Expected: ESLint exits 0.

- [x] **Step 3: Run Harness validation**

Run:

```bash
python3 scripts/check_harness.py
```

Expected: Harness validation exits 0.

- [x] **Step 4: Run diff hygiene check**

Run:

```bash
git diff --check
```

Expected: no whitespace errors.

- [x] **Step 5: Inspect diff boundaries**

Inspect:

```bash
git diff --stat
git diff -- dbflow-admin/src/features/auth/login-page.tsx dbflow-admin/src/features/auth/login-page.test.tsx docs/PLANS.md docs/exec-plans/active/2026-05-03-react-admin-login-polish.md
```

Expected: implementation stays within planned frontend/test/doc scope. If backend or auth API files changed, stop and justify or revert only the agent-owned unintended changes.

- [x] **Step 6: Handoff to harness-verify before completion claim**

Use `harness-verify` semantics before claiming the work is complete. Verification must cite fresh command evidence from this task.

**Evidence:**

- `pnpm --dir dbflow-admin build` exited 0. Vite emitted the existing large chunk warning for the Monaco-backed audit detail bundle, but build succeeded.
- `pnpm --dir dbflow-admin lint` exited 0 with 7 existing Fast Refresh warnings in files outside this change's implementation surface.
- `python3 scripts/check_harness.py` exited 0; Harness Validator found 13 artifacts and all checks passed.
- `git diff --check` exited 0.
- `git status --short` showed only planned files: `login-page.tsx`, `login-page.test.tsx`, `docs/PLANS.md`, the new active plan, and the approved spec.
- Browser visual sanity: Vite dev server running at `http://127.0.0.1:5174/admin/`; Playwright confirmed `/admin/login` renders the Chinese login page at desktop `1440x900` and mobile `390x844`. Screenshots were written outside the repo to `/tmp/dbflow-admin-login-polish.png` and `/tmp/dbflow-admin-login-polish-mobile.png`.

## Dispatch Notes

Do not use `harness-dispatch` for the default path. Task 1 and Task 2 are coupled through the same component contract, and Task 3 depends on their final diff. A single inline `harness-execute` pass is the safest path.

## Completion Summary

Completed: 2026-05-03
All planned tasks: PASS

Summary: React `/login` was Chinese-localized and visually simplified. The final page keeps only a top-left logo + `DBFlow Admin` brand mark and centers the administrator login form both vertically and horizontally, while preserving the existing Spring Security JSON login, CSRF, session store, redirect normalization, failure toast behavior, and password visibility toggle. The earlier access-boundary headline, descriptive paragraph, safety signals, `Refinex-DBFlow` eyebrow, and “数据库操作网关” subtitle were removed per final user direction.

Verification:

- `pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/auth/login-page.test.tsx` exited 0; 1 file and 5 tests passed.
- `pnpm --dir dbflow-admin build` exited 0; Vite emitted the existing large chunk warning for the Monaco-backed audit detail bundle.
- `pnpm --dir dbflow-admin lint` exited 0 with 7 existing Fast Refresh warnings outside the implementation surface.
- `python3 scripts/check_harness.py` exited 0; all Harness checks passed.
- `git diff --check` exited 0.
- Playwright confirmed `/admin/login` renders at desktop `1440x900` and mobile `390x844`; screenshots are outside the repo under `/tmp/`.
