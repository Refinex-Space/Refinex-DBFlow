# React Admin Copy Density Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `harness-execute` semantics to implement this plan task by task. Use `harness-dispatch` only for independent subtasks with disjoint write scopes. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert authenticated React Admin pages to a low-copy, data-first console by replacing page headers with compact breadcrumbs and removing redundant explanatory text.

**Architecture:** Keep the backend and API payload contracts unchanged. Introduce a route-backed breadcrumb surface for authenticated pages, migrate existing `PageHeader` usage to that surface, then clean page-local section descriptions and empty-state descriptions while preserving operation, security, error, and accessibility text.

**Tech Stack:** React 19, TypeScript, Vite, TanStack Router, TanStack Query, shadcn/ui, Tailwind CSS, lucide-react, Vitest browser tests.

**Design Source:** `docs/exec-plans/specs/2026-05-03-react-admin-copy-density-design.md`

---

## Scope

Create or change:

- A compact authenticated-page breadcrumb component backed by route metadata.
- Breadcrumb metadata for sidebar routes and authenticated non-sidebar routes such as audit detail and settings children.
- All authenticated page entry components that currently render `PageHeader`.
- Dashboard, dangerous policy, audit, health, settings, and empty-state copy that currently renders explanatory prose.
- Affected frontend tests so assertions target breadcrumbs, data, statuses, and operations instead of removed prose.

Explicitly exclude:

- Backend Java code, Spring Security, SQL policy behavior, audit semantics, Nacos configuration, and API response schemas.
- Login page, global sidebar IA, global top header/search/theme controls, route URLs, and authentication guards.
- Help drawers, onboarding, documentation popovers, or replacement explanatory surfaces.

## Decisions

- Replace page-level `eyebrow` + `h1` + `description` with a single compact breadcrumb path.
- Do not render `overview.windowLabel`; leave API parsing and API tests intact.
- Keep table headers, form labels, placeholders, filters, buttons, status badges, backend errors, destructive confirmations, Token one-time reveal warnings, and `sr-only` text.
- Remove `EmptyState.description` from business pages by default, but keep the component prop available for future necessary error or safety cases.
- Remove explanatory `SheetDescription` and `CardDescription` only when they repeat field labels or system architecture; keep descriptions that are required for a security confirmation or a backend error.
- Treat settings template copy as in scope because it is visible authenticated UI and currently contains non-DBFlow English prose.

## Files

Expected creates:

- `dbflow-admin/src/components/dbflow/page-breadcrumb.tsx` - compact breadcrumb strip with optional action slot.

Expected modifies:

- `dbflow-admin/src/lib/routes.ts` - central breadcrumb metadata and helper functions.
- `dbflow-admin/src/components/dbflow/page-header.tsx` - delete after all authenticated pages migrate to `PageBreadcrumb`.
- `dbflow-admin/src/components/dbflow/empty-state.tsx` - keep API stable; no forced removal of prop.
- `dbflow-admin/src/features/dashboard/index.tsx`
- `dbflow-admin/src/features/dashboard/components/environment-selector.tsx`
- `dbflow-admin/src/features/dashboard/components/recent-audit-table.tsx`
- `dbflow-admin/src/features/dashboard/components/overview-metrics.tsx`
- `dbflow-admin/src/features/dashboard/components/attention-items.tsx`
- `dbflow-admin/src/features/users/index.tsx`
- `dbflow-admin/src/features/grants/index.tsx`
- `dbflow-admin/src/features/tokens/index.tsx`
- `dbflow-admin/src/features/config/index.tsx`
- `dbflow-admin/src/features/policies/dangerous/index.tsx`
- `dbflow-admin/src/features/audit/list/index.tsx`
- `dbflow-admin/src/features/audit/list/components/audit-filter-sheet.tsx`
- `dbflow-admin/src/features/audit/detail/index.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-timeline.tsx`
- `dbflow-admin/src/features/health/index.tsx`
- `dbflow-admin/src/features/settings/index.tsx`
- `dbflow-admin/src/features/settings/components/content-section.tsx`
- `dbflow-admin/src/features/settings/profile/index.tsx`
- `dbflow-admin/src/features/settings/account/index.tsx`
- `dbflow-admin/src/features/settings/appearance/index.tsx`
- `dbflow-admin/src/features/settings/notifications/index.tsx`
- `dbflow-admin/src/features/settings/display/index.tsx`
- Settings forms under `dbflow-admin/src/features/settings/**` only where visible English prose is explanatory and not required for operation.

Expected tests:

- `dbflow-admin/src/features/dashboard/dashboard.test.tsx`
- `dbflow-admin/src/features/users/users-page.test.tsx`
- `dbflow-admin/src/features/grants/grants-page.test.tsx`
- `dbflow-admin/src/features/tokens/tokens-page.test.tsx`
- `dbflow-admin/src/features/config/config-page.test.tsx`
- `dbflow-admin/src/features/policies/dangerous/dangerous-policies-page.test.tsx`
- `dbflow-admin/src/features/audit/list/audit-list-page.test.tsx`
- `dbflow-admin/src/features/audit/detail/audit-detail-page.test.tsx`
- `dbflow-admin/src/features/health/health-page.test.tsx`

## Verification Strategy

Focused checks during implementation:

```bash
pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/dashboard/dashboard.test.tsx src/features/users/users-page.test.tsx src/features/grants/grants-page.test.tsx src/features/tokens/tokens-page.test.tsx src/features/config/config-page.test.tsx src/features/policies/dangerous/dangerous-policies-page.test.tsx src/features/audit/list/audit-list-page.test.tsx src/features/audit/detail/audit-detail-page.test.tsx src/features/health/health-page.test.tsx
```

Final checks:

```bash
pnpm --dir dbflow-admin test
pnpm --dir dbflow-admin lint
pnpm --dir dbflow-admin build
python3 scripts/check_harness.py
git diff --check
```

Browser evidence:

- Desktop and mobile screenshots for `/admin/`, `/admin/users`, `/admin/grants`, `/admin/tokens`, `/admin/config`, `/admin/policies/dangerous`, `/admin/audit`, `/admin/health`, and `/admin/settings/appearance`.
- Each screenshot must show compact breadcrumb context, no page-level big title/description, and no abnormal blank header area.

## Risks

- Security copy removal risk: preserve Token reveal, dangerous confirmation, backend error, and access failure text.
- Test brittleness risk: replace old heading/prose assertions with breadcrumb, data, and operation assertions.
- Layout regression risk: reduce spacing only around removed header/prose areas; do not redesign table or form internals.
- Route-label drift risk: derive breadcrumbs from shared route metadata instead of hard-coding different labels per page.

## Task 1: Establish Breadcrumb Foundation

**Files:**

- Create: `dbflow-admin/src/components/dbflow/page-breadcrumb.tsx`
- Modify: `dbflow-admin/src/lib/routes.ts`
- Delete: `dbflow-admin/src/components/dbflow/page-header.tsx`

**Decision Trace:** Design sections "导航上下文由面包屑承担" and "采用方案：全局 PageHeader 收敛为 Breadcrumb Strip".

- [x] **Step 1: Add explicit breadcrumb metadata.**

Extend `dbflow-admin/src/lib/routes.ts` so authenticated routes can resolve to ordered breadcrumb labels. Reuse `dbflowRouteGroups` for sidebar routes and add explicit entries for `/audit/:eventId`, `/settings`, `/settings/account`, `/settings/appearance`, `/settings/notifications`, and `/settings/display`.

- [x] **Step 2: Create the compact breadcrumb component.**

Create `PageBreadcrumb` with props for ordered breadcrumb items and optional `actions`. Render one compact left-aligned path and a right-aligned action slot. Use existing Tailwind tokens, restrained border/spacing, no hero typography, and accessible `nav aria-label='页面路径'`.

- [x] **Step 3: Retire title-header semantics.**

After page migration tasks remove all imports from `PageHeader`, delete `page-header.tsx`. Final state must not render `eyebrow`, `h1`, or `description` for authenticated page context.

- [x] **Step 4: Focused verification.**

Run:

```bash
pnpm --dir dbflow-admin lint
```

Expected: no unused export/import errors from breadcrumb or retired header files.

**Evidence:** Created `dbflow-admin/src/components/dbflow/page-breadcrumb.tsx`, added `dbflowBreadcrumbs` and `dbflowBreadcrumbForPath` in `dbflow-admin/src/lib/routes.ts`, deleted `dbflow-admin/src/components/dbflow/page-header.tsx`, and ran `pnpm --dir dbflow-admin lint` successfully with 0 errors and 7 pre-existing Fast Refresh warnings.

## Task 2: Migrate Main Page Headers

**Files:**

- Modify: `dbflow-admin/src/features/dashboard/index.tsx`
- Modify: `dbflow-admin/src/features/users/index.tsx`
- Modify: `dbflow-admin/src/features/grants/index.tsx`
- Modify: `dbflow-admin/src/features/tokens/index.tsx`
- Modify: `dbflow-admin/src/features/config/index.tsx`
- Modify: `dbflow-admin/src/features/policies/dangerous/index.tsx`
- Modify: `dbflow-admin/src/features/audit/list/index.tsx`
- Modify: `dbflow-admin/src/features/audit/detail/index.tsx`
- Modify: `dbflow-admin/src/features/health/index.tsx`

**Decision Trace:** Design sections "页面不是说明书", "页面级设计", and acceptance criterion "认证后每个页面左上角均使用 breadcrumb path".

- [x] **Step 1: Replace all `PageHeader` calls.**

Use `PageBreadcrumb` at the top of each page content section. Preserve existing header actions by moving them to the breadcrumb action slot, including environment selector, create buttons, refresh buttons, audit filter, and back links where currently attached to `PageHeader`.

- [x] **Step 2: Remove page-level descriptions.**

Remove all `description` text listed in the spec from page entry components. Dashboard must stop rendering `overviewQuery.data.windowLabel` while leaving `fetchOverview` and API payload parsing unchanged.

- [x] **Step 3: Tighten page spacing.**

Reduce top-level section spacing where the old header band created extra vertical rhythm. Keep dense internal-tool layout and avoid changing table/form semantics.

- [x] **Step 4: Focused verification.**

Run:

```bash
rg -n "PageHeader|windowLabel|管理 DBFlow|按用户和项目|查看 DBFlow 项目环境配置|只读查看 DBFlow|按时间、用户、项目、环境|只读审计详情|查看 DBFlow 元数据" dbflow-admin/src/features dbflow-admin/src/components
```

Expected: no authenticated UI render path still imports `PageHeader` or renders the removed page descriptions; API tests may still contain `windowLabel`.

**Evidence:** Migrated dashboard, users, grants, tokens, config, dangerous policies, audit list/detail, and health pages to `PageBreadcrumb`; removed page-level `description` rendering including `overviewQuery.data.windowLabel`; search for `PageHeader`, `page-header`, and removed page-description phrases under authenticated UI source returned no matches.

## Task 3: Clean Dashboard Local Copy And Layout

**Files:**

- Modify: `dbflow-admin/src/features/dashboard/components/environment-selector.tsx`
- Modify: `dbflow-admin/src/features/dashboard/components/recent-audit-table.tsx`
- Modify: `dbflow-admin/src/features/dashboard/components/overview-metrics.tsx`
- Modify: `dbflow-admin/src/features/dashboard/components/attention-items.tsx`
- Modify: `dbflow-admin/src/features/dashboard/dashboard.test.tsx`

**Decision Trace:** Design section "页面级设计 / 总览".

- [x] **Step 1: Remove dashboard helper prose.**

Delete the environment follow-up helper text, recent-audit subtitle, attention-items subtitle, and recent-audit empty-state description.

- [x] **Step 2: Compress dashboard cards and sections.**

Keep metric labels and values. Remove or shorten metric hint rendering when it reads as a full explanatory sentence. Preserve critical attention item labels because they are data, not static help text.

- [x] **Step 3: Update dashboard tests.**

Remove assertions for the old `总览` heading and `windowLabel` rendering. Assert breadcrumb text, metric data, table data, environment selector, and concise empty-state titles.

- [x] **Step 4: Focused verification.**

Run:

```bash
pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/dashboard/dashboard.test.tsx
```

Expected: dashboard tests pass without asserting removed explanatory copy.

**Evidence:** Removed environment helper text, recent-audit subtitle, attention-items subtitle, dashboard empty-state descriptions, and metric hint rendering. Focused matrix including `src/features/dashboard/dashboard.test.tsx` passed: 9 files, 38 tests.

## Task 4: Clean Access Management Pages

**Files:**

- Modify: `dbflow-admin/src/features/users/index.tsx`
- Modify: `dbflow-admin/src/features/grants/index.tsx`
- Modify: `dbflow-admin/src/features/tokens/index.tsx`
- Modify: `dbflow-admin/src/features/users/users-page.test.tsx`
- Modify: `dbflow-admin/src/features/grants/grants-page.test.tsx`
- Modify: `dbflow-admin/src/features/tokens/tokens-page.test.tsx`

**Decision Trace:** Design sections "页面级设计 / 用户管理", "项目授权", and "Token 管理".

- [x] **Step 1: Remove empty-state descriptions.**

Keep empty-state titles such as "没有匹配的用户" and remove explanatory descriptions that tell users to adjust filters or create records.

- [x] **Step 2: Preserve access operations.**

Keep filters, create/edit/revoke/disable/reset controls, validation errors, backend errors, and Token one-time plaintext reveal safety copy.

- [x] **Step 3: Update access tests.**

Change old heading assertions to breadcrumb assertions and keep assertions for table data, mutation actions, sensitive data redaction, backend errors, and Token plaintext lifecycle.

- [x] **Step 4: Focused verification.**

Run:

```bash
pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/users/users-page.test.tsx src/features/grants/grants-page.test.tsx src/features/tokens/tokens-page.test.tsx
```

Expected: access management tests pass and still cover sensitive-data boundaries.

**Evidence:** Removed users/grants/tokens page descriptions and empty-state descriptions, kept filters, create/edit/revoke controls, backend error alerts, and Token reveal safety dialog. Focused matrix including access tests passed: 9 files, 38 tests.

## Task 5: Clean Config And Dangerous Policy Pages

**Files:**

- Modify: `dbflow-admin/src/features/config/index.tsx`
- Modify: `dbflow-admin/src/features/policies/dangerous/index.tsx`
- Modify: `dbflow-admin/src/features/config/config-page.test.tsx`
- Modify: `dbflow-admin/src/features/policies/dangerous/dangerous-policies-page.test.tsx`

**Decision Trace:** Design sections "页面级设计 / 配置查看" and "危险策略".

- [x] **Step 1: Remove config explanatory empty-state text.**

Keep sanitized config data, refresh action, detail sheet, and redaction assertions. Remove config page description and empty-state explanatory paragraph.

- [x] **Step 2: Remove dangerous-policy explanation sections.**

Keep policy defaults, whitelist rows, fixed rule data, reason details, risk/decision/requirement fields, and denied-audit link. Remove `SectionTitle.description` usage and the long empty whitelist sentence. Use a short empty-state title such as "无白名单条目".

- [x] **Step 3: Update config and policy tests.**

Change heading assertions to breadcrumb assertions. Remove assertions for long policy explanation and empty whitelist sentence. Keep sensitive redaction, rule data, reason sheet, and audit navigation checks.

- [x] **Step 4: Focused verification.**

Run:

```bash
pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/config/config-page.test.tsx src/features/policies/dangerous/dangerous-policies-page.test.tsx
```

Expected: config and dangerous-policy tests pass with long explanatory copy removed.

**Evidence:** Removed config page description, config empty-state description, dangerous-policy section descriptions, policy detail sheet description, and long empty whitelist sentence; retained policy table data and reason fields. Focused matrix including config and dangerous-policy tests passed: 9 files, 38 tests.

## Task 6: Clean Audit And Health Pages

**Files:**

- Modify: `dbflow-admin/src/features/audit/list/index.tsx`
- Modify: `dbflow-admin/src/features/audit/list/components/audit-filter-sheet.tsx`
- Modify: `dbflow-admin/src/features/audit/detail/index.tsx`
- Modify: `dbflow-admin/src/features/audit/detail/components/audit-timeline.tsx`
- Modify: `dbflow-admin/src/features/health/index.tsx`
- Modify: `dbflow-admin/src/features/audit/list/audit-list-page.test.tsx`
- Modify: `dbflow-admin/src/features/audit/detail/audit-detail-page.test.tsx`
- Modify: `dbflow-admin/src/features/health/health-page.test.tsx`

**Decision Trace:** Design sections "页面级设计 / 审计列表", "审计详情", and "系统健康".

- [x] **Step 1: Remove audit explanatory prose.**

Remove audit list page description, empty-state description, audit filter sheet field-summary description, audit detail page description, and audit timeline card description. Keep timeline item labels and data because they are event reconstruction content.

- [x] **Step 2: Remove health metric descriptions.**

Keep health status, unhealthy count, total count, item names, item details, and refresh. Remove page description and metric-card explanation strings such as count formulas.

- [x] **Step 3: Update audit and health tests.**

Change page heading assertions to breadcrumb assertions. Keep audit data, secret redaction, SQL hash copy, detail navigation, timeline labels, health item detail, and refresh assertions.

- [x] **Step 4: Focused verification.**

Run:

```bash
pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/audit/list/audit-list-page.test.tsx src/features/audit/detail/audit-detail-page.test.tsx src/features/health/health-page.test.tsx
```

Expected: audit and health tests pass while no removed explanatory copy is asserted.

**Evidence:** Removed audit list/detail descriptions, audit filter sheet summary, audit timeline description, audit identity/failure card descriptions, health page description, and health metric descriptions. Focused matrix including audit and health tests passed: 9 files, 38 tests.

## Task 7: Clean Settings Template Copy

**Files:**

- Modify: `dbflow-admin/src/features/settings/index.tsx`
- Modify: `dbflow-admin/src/features/settings/components/content-section.tsx`
- Modify: `dbflow-admin/src/features/settings/profile/index.tsx`
- Modify: `dbflow-admin/src/features/settings/account/index.tsx`
- Modify: `dbflow-admin/src/features/settings/appearance/index.tsx`
- Modify: `dbflow-admin/src/features/settings/notifications/index.tsx`
- Modify: `dbflow-admin/src/features/settings/display/index.tsx`
- Modify: `dbflow-admin/src/features/settings/profile/profile-form.tsx`
- Modify: `dbflow-admin/src/features/settings/account/account-form.tsx`
- Modify: `dbflow-admin/src/features/settings/notifications/notifications-form.tsx`
- Modify: `dbflow-admin/src/features/settings/display/display-form.tsx`

**Decision Trace:** Design sections "页面级设计 / 设置" and decision "Treat settings template copy as in scope".

- [x] **Step 1: Remove settings page title and description.**

Replace `Settings` and `Manage your account settings and set e-mail preferences.` with the shared breadcrumb strip.

- [x] **Step 2: Simplify settings content sections.**

Change `ContentSection` to remove the `desc` prop and render only the section title plus children. Keep section titles and form controls. Remove English template descriptions from settings child index files.

- [x] **Step 3: Remove non-operational English helper prose.**

For settings forms, remove descriptive paragraphs that explain profile/email/social/sidebar concepts. Keep form labels, select placeholders, validation errors, and links only when they lead to real DBFlow settings. Any retained visible labels and submit buttons must be Chinese or DBFlow-specific; do not leave imported template English as visible UI text.

- [x] **Step 4: Verification search.**

Run:

```bash
rg -n "Manage your account|This is how others|Customize the appearance|Configure how you receive|Turn items on or off|Receive emails|Update profile|Update account|Update notifications|Update display" dbflow-admin/src/features/settings
```

Expected: no removed English explanatory template copy or imported English submit labels remain in visible settings UI source.

**Evidence:** Replaced settings title/description with breadcrumb, removed `ContentSection.desc`, removed imported English helper prose, and localized visible settings labels/buttons. Search for removed English template phrases under `dbflow-admin/src/features/settings` returned no matches.

## Task 8: Global Copy Sweep And Test Updates

**Files:**

- Modify: `dbflow-admin/src/features/**/*.test.tsx`
- Modify: any remaining authenticated UI source found by the copy sweep.

**Decision Trace:** Design sections "说明文案默认删除", "必要文本必须保留", and acceptance criteria.

- [x] **Step 1: Run a broad copy sweep.**

Run:

```bash
rg -n "环境过滤将在后续阶段接入|按创建时间倒序展示最近 5 条审计记录|策略拒绝、待确认 SQL、异常连接池和临期 Token|最近 24 小时网关安全、执行和健康摘要|管理 DBFlow 操作员账户|按用户和项目管理|管理 DBFlow MCP SQL Gateway 的访问 Token|查看 DBFlow 项目环境配置的脱敏摘要|只读查看 DBFlow|未命中白名单|仅展示 YAML/Nacos|当前无 DROP 白名单条目，DROP_DATABASE|这些规则由服务端强制执行|按时间、用户、项目、环境、risk|查看 DBFlow 元数据|Manage your account settings" dbflow-admin/src
```

Expected: no visible UI source or UI tests depend on the removed explanatory examples. API tests may keep API fixture values where the UI no longer renders them.

- [x] **Step 2: Run PageHeader migration sweep.**

Run:

```bash
rg -n "PageHeader|description=\\{|description='" dbflow-admin/src/features dbflow-admin/src/components/dbflow
```

Expected: no authenticated page uses `PageHeader`; remaining `description` props are reviewed and limited to necessary error, safety, or component-internal semantics.

- [x] **Step 3: Run focused test matrix.**

Run the focused Vitest command from the verification strategy.

Expected: all affected page tests pass.

**Evidence:** Broad copy sweep for all user-listed explanatory examples returned no matches under `dbflow-admin/src`; PageHeader migration sweep returned no matches under authenticated features and DBFlow components; focused Vitest matrix passed: 9 files, 38 tests.

## Task 9: Final Verification And Visual Review

**Files:**

- Modify: plan evidence only after commands and visual checks complete.

**Decision Trace:** Design sections "测试策略", "视觉验证", and "验收标准".

- [x] **Step 1: Run full frontend and Harness verification.**

Run:

```bash
pnpm --dir dbflow-admin test
pnpm --dir dbflow-admin lint
pnpm --dir dbflow-admin build
python3 scripts/check_harness.py
git diff --check
```

Expected: all commands pass. If implementation unexpectedly touches Java or packaged backend asset mapping, also run `./mvnw test`.

- [x] **Step 2: Browser visual verification.**

Start the appropriate local admin frontend/server and capture desktop and mobile screenshots for the representative pages listed in the verification strategy.

Expected: each page uses a compact breadcrumb, no big page title/description remains, data and operations move higher in the viewport, and text does not overlap or leave obvious blank header bands.

- [x] **Step 3: Update plan evidence.**

Record command outputs, screenshot paths, any browser URLs used, and any skipped checks with concrete reasons.

**Evidence:** Final command matrix passed on the current worktree: `pnpm --dir dbflow-admin test` passed 44 files / 194 tests; `pnpm --dir dbflow-admin lint` passed with 0 errors and 7 existing Fast Refresh warnings; `pnpm --dir dbflow-admin build` passed with the existing large-chunk warning; `python3 scripts/check_harness.py` passed; `git diff --check` passed. Browser verification used Vite at `http://127.0.0.1:5173/admin/` with mocked `/admin/api/**` responses, captured desktop and mobile screenshots for dashboard, users, grants, tokens, config, dangerous policies, audit, health, and settings appearance under `/tmp/dbflow-copy-density-screenshots`, verified each page has `nav[aria-label="页面路径"]`, no `main h1`, and none of the removed example copy.

## Dispatch Safety

Independent implementation slices are possible after Task 1 establishes the breadcrumb component:

- Dashboard slice: Task 3.
- Access management slice: Task 4.
- Config/policy slice: Task 5.
- Audit/health slice: Task 6.
- Settings slice: Task 7.

Do not run these in parallel until Task 1 and the shared route metadata are complete. Parallel workers must avoid editing the same test file or shared component files.
