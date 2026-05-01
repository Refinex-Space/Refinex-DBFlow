# Admin UI Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `harness-execute` semantics to implement this plan task by task. Use `harness-dispatch` only for independent subtasks with disjoint write scopes. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deeply polish the DBFlow admin frontend into a professional, warm-neutral, high-density internal operations console with first-class light/dark themes.

**Architecture:** Keep the current Spring MVC + Thymeleaf + static CSS/JS frontend architecture. Use CSS custom properties as the single design-token and theme layer, use lightweight `admin.js` behavior for theme selection and drawer accessibility, and keep all security/data boundaries server-side.

**Tech Stack:** Spring Boot 3.5, Spring MVC, Thymeleaf, Spring Security form login, static CSS custom properties, vanilla JavaScript, MockMvc, Harness validator, Playwright/browser visual verification.

**Design Source:** `docs/exec-plans/specs/2026-04-30-admin-ui-polish-design.md`

---

## Scope

In scope:

- Rebuild the admin CSS around explicit design tokens for color, typography, spacing, radius, borders, shadows, status colors, focus rings, and density.
- Add light, dark, and system theme modes using CSS custom properties and localStorage-backed vanilla JavaScript.
- Add a topbar theme control that is keyboard-accessible and does not require backend user preference storage.
- Polish the global shell, sidebar, topbar, route hint, page headers, metric strip, panels, tables, filters, forms, buttons, status badges, notices, drawers, empty states, SQL/code blocks, and detail grids.
- Polish all existing admin templates under `src/main/resources/templates/admin/`.
- Remove old cold-gray/blue visual tokens, avoid inline styles where practical, and remove marketing-style or prototype-looking UI language.
- Preserve admin-only access, CSRF behavior, one-time Token plaintext display behavior, and all existing server-side data/redaction boundaries.
- Add or adjust tests and browser verification so the result is technically and visually auditable.
- Keep `docs/references/admin-ui-design-system.md` as the design standard and keep the active plan indexed in `docs/PLANS.md`.

Out of scope:

- React, Vue, SPA migration, Radix UI, shadcn/ui, Tailwind, Vite, or Node build chain.
- New JSON API, WebSocket, SSE, polling, or client-side data store.
- Service-side theme preference persistence.
- Business behavior changes for MCP, SQL execution, audit, Token, grant, user, policy, health, or config pages.
- Configuration editing capability.
- Marketing hero sections, decorative illustrations, decorative gradients, welcome copy, or dashboard card mosaics.

## Decisions

| Decision | Rationale | Rejected Alternative |
| --- | --- | --- |
| Keep Thymeleaf and static assets. | Current security, CSRF, redaction, and routing already fit server-rendered admin pages. | Create a new root React frontend project. |
| Use CSS custom properties as the theme layer. | It keeps the theme system build-free, auditable, and consistent across all templates. | Duplicate light/dark selectors per component. |
| Support `system`, `light`, and `dark` modes. | Users get OS-aligned behavior by default plus explicit control when needed. | Only provide a manual dark-mode toggle. |
| Persist theme in localStorage only. | Avoids adding backend preference storage and database/user schema changes. | Store theme in session, cookie, or metadata database. |
| Use warm-neutral Notion-inspired tokens adapted for admin density. | The design standard is warm and restrained, while DBFlow needs high-density scan efficiency. | Copy Notion marketing-page spacing and display typography. |
| Keep status colors semantic. | DBFlow statuses must communicate safety and operational state clearly. | Use green/orange/red as decorative palette colors. |
| Verify through MockMvc plus browser screenshots. | MockMvc proves routing/security; browser verification proves theme, layout, and interaction quality. | Rely only on unit tests or manual CSS inspection. |

## Files

Create:

- `src/test/java/com/refinex/dbflow/admin/AdminThemeControllerTests.java` - theme-control, admin rendering, and security smoke coverage.

Modify:

- `src/main/resources/static/admin-assets/css/admin.css` - design tokens, light/dark themes, shell, components, page patterns, responsive rules.
- `src/main/resources/static/admin-assets/js/admin.js` - theme initialization, localStorage persistence, system preference handling, drawer keyboard behavior.
- `src/main/resources/templates/admin/fragments/layout.html` - theme control, accessible nav state, shell/topbar markup, route hint cleanup.
- `src/main/resources/templates/admin/login.html` - polished auth panel and notice/control structure.
- `src/main/resources/templates/admin/overview.html` - metric strip, overview split, empty/attention state polish.
- `src/main/resources/templates/admin/users.html` - form/filter/table/action polish and inline-style cleanup.
- `src/main/resources/templates/admin/tokens.html` - one-time secret flash, form/filter/table/action polish.
- `src/main/resources/templates/admin/grants.html` - grant form/filter/table/action polish.
- `src/main/resources/templates/admin/config.html` - redacted config table and empty state polish.
- `src/main/resources/templates/admin/policies-dangerous.html` - policy tables, drawer, and risk-state polish.
- `src/main/resources/templates/admin/audit-list.html` - filter bar, table, pagination, empty state polish.
- `src/main/resources/templates/admin/audit-detail.html` - detail grid, SQL/code blocks, timeline, copy action polish.
- `src/main/resources/templates/admin/health.html` - health grid and abnormal-state polish.
- `src/test/java/com/refinex/dbflow/admin/AdminUiControllerTests.java` - adjust broad smoke assertions if shell/theme markup changes.
- `src/test/java/com/refinex/dbflow/admin/AdminOperationsPageControllerTests.java` - adjust page assertions only if markup changes affect stable text.
- `src/test/java/com/refinex/dbflow/admin/AdminAccessManagementControllerTests.java` - adjust only if shared shell changes affect response assumptions.
- `docs/PLANS.md` - index this active plan.

Reference-only:

- `docs/references/admin-ui-design-system.md` - do not rewrite during implementation unless a discovered contradiction must be corrected.

## Verification Strategy

Run focused admin tests while implementing:

```bash
./mvnw -Dtest=AdminThemeControllerTests,AdminUiControllerTests,AdminOperationsPageControllerTests,AdminAccessManagementControllerTests test
```

Expected: admin pages render, non-admin requests remain forbidden, theme controls exist, and existing management workflows still pass.

Run residue/static scans:

```bash
rg -n "style=|#f4f6f9|#1d2733|#146a9f|hero|Welcome back|linear-gradient" src/main/resources/templates/admin src/main/resources/static/admin-assets/css/admin.css
```

Expected: no old cold-gray core tokens, marketing hero/welcome language, or broad inline styles remain. Any remaining `style=` must be explicitly justified in plan evidence.

Run full verification before completion:

```bash
./mvnw test
python3 scripts/check_harness.py
git diff --check
```

Expected: Maven suite passes, Harness validator passes, and no whitespace errors are reported.

Run browser verification after implementation:

- Start the app with the repository-supported dev command.
- Verify `/login`, `/admin`, `/admin/audit`, `/admin/users`, `/admin/tokens`, `/admin/grants`, `/admin/config`, `/admin/policies/dangerous`, and `/admin/health`.
- Use desktop `1440x900`, tablet `1024x768`, and mobile `390x844` viewports.
- Check both light and dark themes, theme persistence through localStorage, drawer open/close/ESC, keyboard focus visibility, text contrast, table readability, and absence of incoherent overlap.

## Risks

| Risk | Priority | Mitigation |
| --- | --- | --- |
| Theme polish accidentally hides sensitive one-time Token warning semantics. | Security | Keep Token plaintext flash text and warning treatment explicit; add smoke assertions and visual check. |
| Dark theme introduces low-contrast text, code, or status badges. | Correctness | Use semantic dark tokens and browser verify every page in dark mode. |
| Broad CSS rewrite breaks existing layout or forms. | Correctness | Preserve class names where practical; run focused admin tests and browser checks. |
| localStorage theme JS causes page flash or breaks when JS fails. | Compatibility | Default CSS to light; JS only enhances by setting `data-theme`; page remains usable without JS. |
| Mobile shell becomes unusable due to dense admin layout. | UX correctness | Use explicit breakpoints and verify `390x844`; keep tables horizontally scrollable. |
| Scope expands into frontend architecture migration. | Delivery | Keep React/Radix/Tailwind out of scope and defer any future migration to a new approved spec. |

## Task 1: Lock The Theme Contract And RED/Contract Tests

**Files:**

- Create: `src/test/java/com/refinex/dbflow/admin/AdminThemeControllerTests.java`
- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`

**Decision Trace:** Spec sections “主题系统”, “可访问性”, “验收标准”.

- [x] **Step 1: Add admin theme contract tests.**

Add MockMvc tests that prove:

- `/admin` as `ROLE_ADMIN` renders a theme control with `system`, `light`, and `dark` options.
- `/login` includes the admin stylesheet and renders without admin session.
- `/admin` as a non-admin remains forbidden.
- Shared layout still renders MCP status, config source, current admin, and logout.

Use stable product text and attributes, not brittle CSS class internals, except where the class or data attribute is the contract for the theme switcher.

- [x] **Step 2: Run the RED test.**

Run:

```bash
./mvnw -Dtest=AdminThemeControllerTests test
```

Expected: tests fail before theme controls and theme attributes are implemented.

**Evidence:** Added `AdminThemeControllerTests`. RED run `./mvnw -Dtest=AdminThemeControllerTests test` failed as expected with one failure: `/admin` did not yet render `data-theme-choice="system"`. The same run reached the real admin shell and showed current MCP/config/admin/logout content, proving the missing theme control contract before implementation.

## Task 2: Build Design Tokens And Light/Dark Theme Foundation

**Files:**

- Modify: `src/main/resources/static/admin-assets/css/admin.css`

**Decision Trace:** Spec sections “亮色主题”, “暗色主题”, “语义状态色”, “字体与排版”.

- [x] **Step 1: Reorganize CSS into auditable sections.**

Restructure `admin.css` in-place with clear comment sections:

- tokens and theme variables
- reset/base
- layout
- components
- page patterns
- responsive rules

Keep the file build-free and avoid introducing external fonts or CSS dependencies.

- [x] **Step 2: Define global tokens.**

Create tokens for:

- font stacks
- spacing and density
- radius
- border
- shadow
- focus ring
- transitions
- light theme surfaces/text/accent/status
- dark theme surfaces/text/accent/status

Use the spec token values as the source of truth. Keep light theme as the default so no-JS fallback is usable.

- [x] **Step 3: Replace old cold-gray base colors.**

Replace old base tokens such as `#f4f6f9`, `#1d2733`, `#146a9f`, and cold gray table/header colors with warm-neutral semantic variables.

- [x] **Step 4: Run CSS residue scan.**

Run:

```bash
rg -n "#f4f6f9|#1d2733|#146a9f|linear-gradient" src/main/resources/static/admin-assets/css/admin.css
```

Expected: old core theme colors are absent. `linear-gradient` is absent unless used only for a non-decorative skeleton/loading treatment with evidence.

**Evidence:** Rebuilt `admin.css` around theme variables, warm-neutral light defaults, warm dark theme overrides, semantic status tokens, focus rings, layout/component/page/responsive sections, and no external dependencies. `rg -n "#f4f6f9|#1d2733|#146a9f|linear-gradient" src/main/resources/static/admin-assets/css/admin.css` returned no matches.

## Task 3: Implement Theme JavaScript And Accessible Shell Controls

**Files:**

- Modify: `src/main/resources/static/admin-assets/js/admin.js`
- Modify: `src/main/resources/templates/admin/fragments/layout.html`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminThemeControllerTests.java`

**Decision Trace:** Spec sections “主题系统”, “交互行为”, “全局 Shell”, “可访问性”.

- [x] **Step 1: Add theme initialization in `admin.js`.**

Implement build-free vanilla JS that:

- reads `localStorage.getItem("dbflow-admin-theme")`
- supports `system`, `light`, and `dark`
- writes `data-theme="light"` or `data-theme="dark"` on `document.documentElement`
- reacts to `prefers-color-scheme` changes only when the stored mode is `system` or absent
- handles invalid stored values by falling back to `system`

- [x] **Step 2: Add theme control markup.**

Update `layout.html` to add a compact theme segmented control or menu with buttons for `system`, `light`, and `dark`.

Requirements:

- uses real `button` elements
- includes accessible labels or readable text
- exposes a stable data attribute for JS, such as `data-theme-choice`
- does not submit forms or navigate
- does not hide logout or status metadata

- [x] **Step 3: Improve drawer keyboard behavior.**

Keep existing drawer behavior and add ESC close support. Ensure close buttons remain focusable and drawer behavior works in dark mode.

- [x] **Step 4: Re-run theme tests.**

Run:

```bash
./mvnw -Dtest=AdminThemeControllerTests test
```

Expected: theme contract tests pass.

**Evidence:** Rebuilt `admin.js` with `dbflow-admin-theme` localStorage support, `system/light/dark` mode handling, `documentElement[data-theme]`, system preference listeners, active button state updates, drawer ESC close, and preserved password toggle. Updated `layout.html` with an accessible theme control using `data-theme-choice`. `./mvnw -Dtest=AdminThemeControllerTests test` passed with 3 tests, 0 failures, 0 errors.

## Task 4: Polish Global Shell, Navigation, Topbar, And Responsive Frame

**Files:**

- Modify: `src/main/resources/templates/admin/fragments/layout.html`
- Modify: `src/main/resources/static/admin-assets/css/admin.css`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminUiControllerTests.java`

**Decision Trace:** Spec sections “全局 Shell”, “响应式”, “可访问性”.

- [x] **Step 1: Rework shell layout tokens.**

Update `.app`, `.sidebar`, `.main`, `.topbar`, `.content`, `.route-hint`, `.side-brand`, `.nav-group`, `.nav-label`, and `.nav-item` to use warm-neutral theme variables.

Implementation constraints:

- sidebar remains usable at desktop width
- active navigation uses accent-tinted treatment, not heavy dark fill
- hover/focus states do not shift layout
- long topbar metadata truncates cleanly

- [x] **Step 2: Add nav accessibility contract.**

Add `aria-current="page"` or equivalent Thymeleaf conditional attributes for active navigation links. Keep audit detail visually associated with audit navigation rather than a separate primary nav destination.

- [x] **Step 3: Add responsive shell rules.**

Implement breakpoints:

- desktop two-column shell
- tablet narrowed/stacked behavior
- mobile usable navigation and content spacing

Keep mobile tables horizontally scrollable rather than forcing lossy card conversion.

- [x] **Step 4: Run shell smoke tests.**

Run:

```bash
./mvnw -Dtest=AdminThemeControllerTests,AdminUiControllerTests test
```

Expected: all shell/login/admin smoke tests pass.

**Evidence:** Updated shell CSS for warm-neutral sidebar/topbar/content/route hint, active nav accent treatment, truncating topbar metadata, and mobile/tablet breakpoints. Updated `layout.html` navigation with `aria-current="page"` for active destinations and collapsed audit detail into the audit navigation state. `./mvnw -Dtest=AdminThemeControllerTests,AdminUiControllerTests test` passed with 9 tests, 0 failures, 0 errors.

## Task 5: Polish Core Components

**Files:**

- Modify: `src/main/resources/static/admin-assets/css/admin.css`
- Modify: `src/main/resources/templates/admin/*.html`

**Decision Trace:** Spec sections “组件体系”, “语义状态色”, “交互行为”, “安全边界”.

- [x] **Step 1: Polish buttons and form controls.**

Update `.btn`, `.btn.primary`, `.btn.danger`, `.btn.text`, `.control`, `.select`, labels, disabled controls, hover, active, and focus-visible states.

Requirements:

- desktop height at least 34px
- mobile touch targets at least 44px
- focus rings are visible in both themes
- danger actions remain textually explicit

- [x] **Step 2: Polish panels, notices, empty states, and metrics.**

Update `.panel`, `.panel-head`, `.panel-body`, `.notice`, `.empty`, `.metric-grid`, `.metric`, `.small-list`, and `.small-row`.

Requirements:

- panels use 8px radius and whisper borders
- notices inherit semantic status tokens
- empty states are concise and do not use illustrations
- metric strip uses typography-first hierarchy and stable dimensions

- [x] **Step 3: Polish tables and code/detail surfaces.**

Update `.table-wrap`, `table`, `th`, `td`, row hover, `.mono`, `.sql-box`, `.detail-grid`, `.kv`, status badges, and decision badges.

Requirements:

- table headers use warm neutral surfaces
- long code/text remains readable and scrollable
- status badges keep dot + text
- dark mode contrast is preserved

- [x] **Step 4: Polish drawer and flash secret states.**

Update drawer styles, backdrop treatment if added, `.flash-secret`, and related controls.

Requirements:

- drawer works in light/dark
- one-time Token plaintext remains visually prominent and explicitly warning-colored
- no sensitive value is made easier to leak beyond the existing one-time display behavior

- [x] **Step 5: Run component residue scan.**

Run:

```bash
rg -n "style=|#f4f6f9|#1d2733|#146a9f|hero|Welcome back|linear-gradient" src/main/resources/templates/admin src/main/resources/static/admin-assets/css/admin.css
```

Expected: no broad inline style or old theme residue remains. Any remaining hit is documented in the Evidence field with a reason.

**Evidence:** Polished the core component layer in `admin.css` for buttons, form controls, panels, notices, metrics, tables, status badges, detail grids, SQL/code blocks, drawer, and one-time Token flash states. Added reusable layout utilities (`actions-between`, spacing helpers, `control-pair`, `detail-grid.two`, `w-full`) and removed the remaining broad inline styles from admin templates, including the policy drawer initial `aria-hidden="true"` state and an invalid extra closing `div` in `audit-detail.html`. Residue scan `rg -n "style=|#f4f6f9|#1d2733|#146a9f|hero|Welcome back|linear-gradient" src/main/resources/templates/admin src/main/resources/static/admin-assets/css/admin.css` returned no matches.

## Task 6: Polish Page-Specific Layouts

**Files:**

- Modify: `src/main/resources/templates/admin/login.html`
- Modify: `src/main/resources/templates/admin/overview.html`
- Modify: `src/main/resources/templates/admin/users.html`
- Modify: `src/main/resources/templates/admin/tokens.html`
- Modify: `src/main/resources/templates/admin/grants.html`
- Modify: `src/main/resources/templates/admin/config.html`
- Modify: `src/main/resources/templates/admin/policies-dangerous.html`
- Modify: `src/main/resources/templates/admin/audit-list.html`
- Modify: `src/main/resources/templates/admin/audit-detail.html`
- Modify: `src/main/resources/templates/admin/health.html`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminUiControllerTests.java`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminOperationsPageControllerTests.java`
- Test: `src/test/java/com/refinex/dbflow/admin/AdminAccessManagementControllerTests.java`

**Decision Trace:** Spec sections “页面布局”, “响应式”, “验收标准”.

- [x] **Step 1: Polish login and overview.**

Update login and overview markup only where structural classes or accessibility hooks are needed. Keep server-side data model unchanged.

Expected outcomes:

- login uses compact auth panel with polished notices
- overview KPI strip and split layout feel like a working surface
- no marketing hero or welcome copy appears

- [x] **Step 2: Polish access-management pages.**

Update users, tokens, and grants pages.

Expected outcomes:

- create/filter forms have consistent density
- action clusters do not wrap awkwardly
- one-time Token flash remains high-signal
- empty states are consistent

- [x] **Step 3: Polish operations pages.**

Update config, dangerous policies, audit list, audit detail, and health pages.

Expected outcomes:

- config page foregrounds redaction/source clearly
- dangerous policy page makes risk and read-only status obvious
- audit list filters and pagination scan cleanly
- audit detail SQL/code/timeline are readable in both themes
- health abnormal states are visually prioritized

- [x] **Step 4: Run admin regression tests.**

Run:

```bash
./mvnw -Dtest=AdminUiControllerTests,AdminOperationsPageControllerTests,AdminAccessManagementControllerTests test
```

Expected: existing admin pages render, non-admin users remain forbidden, access-management write flows still pass, and no test relies on removed prototype visual text.

**Evidence:** Page-specific polish was applied through shared component classes and targeted markup cleanup across login, overview, access-management, config, dangerous policies, audit, and health pages without changing server-side models or write-flow behavior. Access-management and operations regression run `./mvnw -Dtest=AdminUiControllerTests,AdminOperationsPageControllerTests,AdminAccessManagementControllerTests test` passed with 16 tests, 0 failures, 0 errors, 0 skipped.

## Task 7: Browser Verification For Professional Frontend Quality

**Files:**

- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`

**Decision Trace:** Spec sections “验收标准” and “验证策略”.

- [x] **Step 1: Start the local app.**

Start the app with the repository-supported development command, using the current project’s documented local configuration path.

Record the command and localhost URL in Evidence.

- [x] **Step 2: Verify light and dark themes across key pages.**

Use browser automation or Playwright to inspect:

- `/login`
- `/admin`
- `/admin/audit`
- `/admin/users`
- `/admin/tokens`
- `/admin/grants`
- `/admin/config`
- `/admin/policies/dangerous`
- `/admin/health`

Check both light and dark theme modes at desktop `1440x900`.

- [x] **Step 3: Verify responsive layouts.**

Check at least:

- tablet `1024x768`
- mobile `390x844`

Expected: no incoherent overlap, no clipped primary controls, readable text, usable navigation, and horizontally scrollable tables where needed.

- [x] **Step 4: Verify interactions.**

Check:

- theme mode changes update `data-theme`
- `dbflow-admin-theme` persists in localStorage
- `system` mode follows `prefers-color-scheme`
- drawer opens and closes
- ESC closes drawer
- keyboard focus is visible on theme controls, nav, buttons, and forms

**Evidence:** Started local runtime at `http://localhost:18080` with `./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=18080 --spring.cloud.nacos.config.enabled=false --spring.cloud.nacos.discovery.enabled=false --spring.cloud.service-registry.auto-registration.enabled=false --spring.datasource.url=jdbc:h2:mem:admin_ui_browser;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1 --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --dbflow.admin.initial-user.enabled=true --dbflow.admin.initial-user.username=admin --dbflow.admin.initial-user.display-name=DBFlow Administrator --dbflow.admin.initial-user.password=admin --dbflow.security.mcp-token.pepper=dev-only-change-me --dbflow.datasource-defaults.validate-on-startup=false"`, then copied updated static resources with `./mvnw resources:resources`. First Playwright pass found real tablet overflow on `/admin/users` at `1024x768` (`bodyOverflow=95`) from the topbar/theme control; fixed by allowing the topbar metadata row to wrap at tablet width. Second Playwright pass logged in through `/login`, visited `/admin`, `/admin/audit`, `/admin/users`, `/admin/tokens`, `/admin/grants`, `/admin/config`, `/admin/policies/dangerous`, and `/admin/health` in desktop light and dark modes, checked `users:tablet-light`, `overview:mobile-dark`, and `audit:mobile-dark`, verified `dbflow-admin-theme` persistence, `system` mode mapping to emulated light/dark `prefers-color-scheme`, visible keyboard focus, and policy drawer open/ESC close with `aria-hidden` toggling. No console warnings/errors and no post-fix overflow failures. Screenshots captured under `target/admin-ui-screenshots/`: `login-light.png`, `admin-light.png`, `admin-dark.png`, `policies-drawer-dark.png`, `admin-mobile-dark.png`, and `audit-mobile-dark.png`.

## Task 8: Final Verification And Plan Handoff

**Files:**

- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`
- Modify: `docs/PLANS.md`

**Decision Trace:** Spec sections “验收标准”, “验证策略”, and Harness lifecycle requirements.

- [x] **Step 1: Run full technical verification.**

Run:

```bash
./mvnw test
python3 scripts/check_harness.py
git diff --check
```

Expected: all commands pass. Maven may skip Docker/Testcontainers MySQL tests if Docker is unavailable; record skip counts accurately.

- [x] **Step 2: Run final residue scan.**

Run:

```bash
rg -n "style=|#f4f6f9|#1d2733|#146a9f|hero|Welcome back|linear-gradient" src/main/resources/templates/admin src/main/resources/static/admin-assets/css/admin.css
```

Expected: no disallowed residue remains. Any allowed hit must be documented with a precise reason.

- [x] **Step 3: Record evidence and prepare completion.**

Fill each task Evidence field with command outputs, browser observations, screenshots or screenshot paths if produced, and residual risks.

Do not claim completion until `harness-verify` has fresh evidence.

- [x] **Step 4: Keep plan index accurate.**

Ensure `docs/PLANS.md` still lists this file under Active Plans until `harness-finish` archives it.

**Evidence:** Full verification passed: `./mvnw test` reported 143 tests, 0 failures, 0 errors, 10 skipped; `python3 scripts/check_harness.py` passed with 14 manifest artifacts; `git diff --check` passed with no output. Final residue scan `rg -n "style=|#f4f6f9|#1d2733|#146a9f|hero|Welcome back|linear-gradient" src/main/resources/templates/admin src/main/resources/static/admin-assets/css/admin.css` returned no matches. `docs/PLANS.md` still indexes this active plan at line 21. Current changed files are source, tests, and Harness docs only; Playwright screenshots are under ignored `target/`.

## Task 9: Login Page Split Layout Iteration

**Files:**

- Modify: `src/main/resources/templates/admin/login.html`
- Modify: `src/main/resources/static/admin-assets/css/admin.css`
- Modify: `src/main/resources/static/admin-assets/js/admin.js`
- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`

**Decision Trace:** User screenshot feedback on `2026-04-30`: evolve login from centered card into a Nacos-inspired two-column entry surface while keeping DBFlow's professional internal-control-plane tone.

- [x] **Step 1: Rebuild login composition.**

Replace the centered-only login card with a desktop two-column layout: left visual/brand panel near 2/3 width, right login form panel near 1/3 width. Keep mobile stacked and avoid marketing-copy sprawl.

- [x] **Step 2: Replace text password toggle with an inline icon button.**

Move password visibility control inside the password input, use SVG eye/eye-off icons, and keep `aria-label` / `aria-pressed` synchronized.

- [x] **Step 3: Verify login page behavior and responsive quality.**

Run focused login/shell tests and browser verification for desktop/mobile screenshots, overflow, icon-only password control, and click behavior.

**Evidence:** Updated `login.html` with a left-side DBFlow brand/positioning panel (`AI 数据库操作的内部控制面`) and right-side form panel. Updated login CSS with a dark dotted technical background, line intersections, soft radial accents, form polish, mobile stacking, and an inline password icon button. Updated `admin.js` password toggle behavior to switch icon state through `is-visible`, `aria-label`, and `aria-pressed` instead of changing visible text. `./mvnw -Dtest=AdminThemeControllerTests,AdminUiControllerTests test` passed with 9 tests, 0 failures, 0 errors, 0 skipped. Login residue scan over `login.html` and `admin.css` returned no matches for broad inline style, old core colors, `linear-gradient`, hero, or welcome copy. Playwright runtime at `http://localhost:18080/login?logout` verified desktop `2048x1067` columns as `1365/683`, no overflow, icon-only password toggle inside the input, password visibility changing to `type=text` with `aria-label=隐藏密码`, and mobile `390x844` no overflow with 44px submit button. Screenshots: `target/admin-ui-screenshots/login-split-desktop.png` and `target/admin-ui-screenshots/login-split-mobile.png`.

## Task 10: Login Page Light Grid Refinement

**Files:**

- Modify: `src/main/resources/templates/admin/login.html`
- Modify: `src/main/resources/static/admin-assets/css/admin.css`
- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`

**Decision Trace:** User screenshot feedback on `2026-04-30`: reduce left-side text density, replace dark visual treatment with a light technical grid, and make the right form panel fill the column rather than appear as a floating card.

- [x] **Step 1: Reduce left-side content density.**

Shorten the left-side title/subtitle and reduce capability badges to lightweight operational signals.

- [x] **Step 2: Replace dark visual with light grid.**

Use a light grid surface, restrained diagonal lines, soft nodes, and subtle tiles instead of a dark background.

- [x] **Step 3: Make right form column integrated.**

Remove card border/shadow from the right form, keep the whole right column as a full-height white panel, and vertically center the form content.

- [x] **Step 4: Verify responsive and persisted-theme behavior.**

Run focused tests and browser checks for desktop/mobile overflow, form centering, and login staying light even when `dbflow-admin-theme=dark` exists.

**Evidence:** Updated the login left side to `数据库操作控制面` plus one short subtitle and two meta signals. Replaced the dark visual field with a light SVG grid, pale diagonal lines, soft blue nodes, and subtle background tiles. Converted the right side from a bordered/shadowed card into a full-height white form column with the form vertically centered. `./mvnw -Dtest=AdminThemeControllerTests,AdminUiControllerTests test` passed with 9 tests, 0 failures, 0 errors, 0 skipped. Login residue scan over `login.html` and `admin.css` returned no matches for broad inline style, old core colors, `linear-gradient`, hero, welcome copy, or the removed `login-capabilities` class. Playwright runtime verified desktop `2048x1067` columns as `1365/683`, no overflow, light visual background `rgb(251, 251, 250)`, white right panel `rgb(255, 255, 255)`, form border `0px`, shadow `none`, vertical center offset `0`, and persisted `dbflow-admin-theme=dark` still leaves the login composition light. Mobile `390x844` had no overflow, 342px visual block, 44px submit button, and no card border. Screenshots: `target/admin-ui-screenshots/login-grid-desktop.png` and `target/admin-ui-screenshots/login-grid-mobile.png`.

## Task 11: Login Page Brand Copy Refinement

**Files:**

- Modify: `src/main/resources/templates/admin/login.html`
- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`

**Decision Trace:** User copy feedback on `2026-04-30`: left-side headline and subtitle should be shorter, less technical, and more professional.

- [x] **Step 1: Refine left-side headline and subtitle.**

Replace technical/vendor-heavy wording with concise governance-oriented product copy.

- [x] **Step 2: Verify login rendering contract.**

Run focused login/theme rendering tests.

**Evidence:** Replaced the left-side headline/subtitle with `数据库操作治理中心` and `让每一次访问都有授权、边界与审计。`, removing vendor/tool enumeration and reducing technical density. `./mvnw -Dtest=AdminThemeControllerTests test` passed with 3 tests, 0 failures, 0 errors, 0 skipped.

## Task 12: Admin Brand Logo Asset Replacement

**Files:**

- Create: `src/main/resources/static/admin-assets/img/dbflow-logo.svg`
- Modify: `src/main/resources/templates/admin/login.html`
- Modify: `src/main/resources/templates/admin/fragments/layout.html`
- Modify: `src/main/resources/static/admin-assets/css/admin.css`
- Modify: `src/test/java/com/refinex/dbflow/admin/AdminThemeControllerTests.java`
- Modify: `src/test/java/com/refinex/dbflow/admin/AdminUiControllerTests.java`
- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`

**Decision Trace:** User supplied `/Users/refinex/Downloads/claw-icon.svg` and requested it be copied into the project with an appropriate name, then used on the login page and system top-left brand area.

- [x] **Step 1: Copy and rename the logo asset.**

Place the SVG under the admin static asset namespace using a product-oriented filename.

- [x] **Step 2: Replace text marks in login and shell.**

Replace the old `DB` text mark with the SVG logo in the login brand lockup and sidebar brand lockup.

- [x] **Step 3: Update tests and visual verification.**

Add render/static-resource assertions and verify the logo loads at the intended size.

**Evidence:** Copied `/Users/refinex/Downloads/claw-icon.svg` to `src/main/resources/static/admin-assets/img/dbflow-logo.svg`. The SVG was inspected and contains no `<script>` or external `http(s)` references. Replaced login and sidebar `DB` text marks with `<img class="brand-logo" th:src="@{/admin-assets/img/dbflow-logo.svg}">`, removed stale `.brand-mark` CSS, and added explicit logo sizing (`38px` on login, `34px` in the sidebar). Updated MockMvc assertions so `/login` and `/admin` render the new logo path and `/admin-assets/img/dbflow-logo.svg` is anonymously accessible. `./mvnw -Dtest=AdminThemeControllerTests,AdminUiControllerTests test` passed with 9 tests, 0 failures, 0 errors, 0 skipped. Playwright runtime verified the login logo and admin sidebar logo load successfully with `naturalWidth > 0`, expected rendered sizes, and no horizontal overflow; screenshots: `target/admin-ui-screenshots/login-logo.png` and `target/admin-ui-screenshots/admin-logo.png`.

## Task 13: Login Page Theme Toggle Icon

**Files:**

- Modify: `src/main/resources/templates/admin/login.html`
- Modify: `src/main/resources/static/admin-assets/css/admin.css`
- Modify: `src/main/resources/static/admin-assets/js/admin.js`
- Modify: `src/test/java/com/refinex/dbflow/admin/AdminThemeControllerTests.java`
- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`

**Decision Trace:** User requested a theme switch icon in the top-right of the login page's right column, with different light and dark icon styles.

- [x] **Step 1: Add a right-column theme toggle control.**

Place a compact icon button at the top-right of the login form panel.

- [x] **Step 2: Use distinct icons for each theme state.**

Show a moon icon in light mode and a sun icon in dark mode, with accessible labels that describe the next action.

- [x] **Step 3: Wire the control into persisted theme behavior.**

Reuse the existing admin theme storage key so login and admin shell theme preferences stay consistent.

- [x] **Step 4: Verify interaction and responsive layout.**

Run focused tests plus browser checks for icon switching, localStorage persistence, ARIA state, and desktop/mobile overflow.

**Evidence:** Added a top-right `.login-theme-toggle` button to the login right column with separate moon and sun SVG icons. Updated `admin.js` so `[data-theme-toggle]` writes `dbflow-admin-theme`, applies `html[data-theme]`, updates `aria-pressed`, and changes the accessible label between `切换到暗色主题` and `切换到亮色主题`. Added scoped login dark-mode CSS plus icon visibility rules. Updated `AdminThemeControllerTests` to assert `data-theme-toggle`, `theme-toggle-moon`, and `theme-toggle-sun`. `./mvnw -Dtest=AdminThemeControllerTests,AdminUiControllerTests test` passed with 9 tests, 0 failures, 0 errors, 0 skipped. Residue scan over admin templates and CSS returned no matches for inline style, old core colors, stale hero/welcome copy, `linear-gradient`, or `.brand-mark`. Playwright verified light mode uses the moon icon and `aria-label=切换到暗色主题`, dark mode uses the sun icon and `aria-label=切换到亮色主题`, both states persist to `localStorage`, second click restores light mode, and desktop/mobile have no horizontal overflow. Screenshots: `target/admin-ui-screenshots/login-theme-light.png`, `target/admin-ui-screenshots/login-theme-dark.png`, and `target/admin-ui-screenshots/login-theme-mobile.png`.

## Task 14: Login Theme Toggle Visual Refinement

**Files:**

- Modify: `src/main/resources/templates/admin/login.html`
- Modify: `src/main/resources/static/admin-assets/css/admin.css`
- Modify: `docs/exec-plans/active/2026-04-30-admin-ui-polish.md`

**Decision Trace:** User feedback on `2026-05-01`: the moon icon style was not polished enough, and the login page theme toggle should not have a border, shadow, or floating-button effect.

- [x] **Step 1: Refine the light-mode moon icon.**

Replace the plain crescent with a lighter moon-star outline while keeping a distinct sun icon for dark mode.

- [x] **Step 2: Remove floating button treatment.**

Remove the default border, shadow, raised surface, and active transform from the login theme toggle.

- [x] **Step 3: Verify style and interaction.**

Run the theme rendering test and browser computed-style checks for the updated icon button.

**Evidence:** Replaced the login theme toggle moon SVG with a moon-star outline using two paths. Updated `.login-theme-toggle` to use `border: 0`, `background: transparent`, `box-shadow: none`, no active transform, and only a subtle hover background/color change. `./mvnw -Dtest=AdminThemeControllerTests test` passed with 3 tests, 0 failures, 0 errors, 0 skipped. CSS scan confirmed `.login-theme-toggle` has `border: 0` and `box-shadow: none`. Playwright verified the button computed style as `borderTopWidth=0px`, `boxShadow=none`, `backgroundColor=rgba(0, 0, 0, 0)`, `transform=none`, the light-mode moon icon has two SVG paths, dark mode still swaps to the sun icon, and no desktop overflow occurs. Screenshots: `target/admin-ui-screenshots/login-theme-icon-light.png` and `target/admin-ui-screenshots/login-theme-icon-dark.png`.

## Dispatch Safety

Potential independent work after Task 2:

- A worker can own `admin.js` and `layout.html` theme-control behavior after CSS token names are stable.
- A worker can own page-specific template cleanup after core component classes are stable.
- A verifier can run browser checks while final CSS polish continues only if no files are edited by the verifier.

Recommended default: execute inline with `harness-execute` because `admin.css`, `layout.html`, and all templates share a dense write scope and visual consistency matters more than parallel speed.
