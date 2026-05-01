<!-- Harness brainstorm spec. Safe to edit before approval. -->

# Admin UI 深度打磨设计

Date: 2026-04-30

Status: Approved

## 背景

DBFlow 管理端已经具备 Spring MVC + Thymeleaf 页面骨架，覆盖登录、总览、用户、Token、项目环境授权、配置查看、危险策略、审计列表、审计详情和系统健康页面。它现在是可用的内部运维控制台，但视觉、布局、交互状态和响应式体验仍保留早期工程原型气质：

- 颜色偏冷灰蓝，和新的 Notion-inspired 设计标准不一致。
- 左侧导航、顶栏、面板、表格、筛选区、按钮和状态 badge 都是基础样式，缺少统一 design token。
- 目前没有主题概念，无法提供专业的亮色/暗色体验。
- 表格和表单可用，但扫描层级、焦点状态、hover/active 状态、移动端布局和空/错误/提示状态需要系统性打磨。
- 页面中仍存在部分 inline style 和原型 fallback 文案，需要迁移到统一样式体系。

根目录原 `DESIGN.md` 已归位为 `docs/references/admin-ui-design-system.md`，作为后台 UI 设计基线，并已从 `README.md`、`AGENTS.md`、`docs/ARCHITECTURE.md` 建立渐进式索引。

## 目标

- 基于 `docs/references/admin-ui-design-system.md` 建立 DBFlow 管理端专用的亮色和暗色主题系统。
- 保留现有 Spring MVC + Thymeleaf + 静态 CSS/JS 架构，不引入 SPA、Node/Vite、Tailwind 或新前端构建链。
- 将现有后台从“工程原型”提升为“专业内部运维控制台”：温暖中性、清晰层级、高密度可扫描、交互状态完整。
- 用 design token 统一颜色、字体、空间、圆角、边框、阴影、状态色、焦点环和表格密度。
- 打磨全局 shell、登录页、总览页、列表/筛选页、详情页、配置/策略/健康页、drawer、notice、empty state。
- 支持主题跟随系统偏好、显式切换和浏览器本地持久化。
- 保持安全边界：UI 打磨不新增敏感字段展示、不改变权限、不改变业务数据口径。

## 非目标

- 不重写为 React/Vue/SPA。
- 不新增 JSON API、WebSocket、SSE 或自动刷新机制。
- 不修改 MCP tool、SQL 执行、审计写入、Token 或授权业务语义。
- 不新增品牌营销页、hero 页面、装饰插画或大面积宣传文案。
- 不引入外部字体下载依赖；字体优先使用本地系统栈。
- 不做配置编辑能力；配置页仍为只读展示。
- 不实现服务端保存的用户主题偏好；本阶段主题偏好仅存在浏览器本地。

## 设计依据

### 项目约束

- 管理端是内部数据库运维工具，不是营销站点。
- 页面打开后应立即进入工作面，首屏展示状态、筛选、表格或操作区域。
- DBFlow 的核心价值是安全、审计、可解释和可操作，视觉必须服务于这四点。
- 当前页面模板位于 `src/main/resources/templates/admin/`，静态资源位于 `src/main/resources/static/admin-assets/`。

### 视觉标准

采用 `docs/references/admin-ui-design-system.md` 的核心原则，但按 admin tool 场景收敛：

- 温暖中性色，而不是冷灰蓝。
- 轻边界：`1px` whisper border，不使用厚重分割线。
- 克制阴影：只用于 shell、drawer、modal 类需要层级的区域。
- Notion Blue 作为唯一核心交互强调色。
- 状态色只表达语义状态，不作为装饰色使用。
- 卡片和面板必须服务于信息分组，不做营销式卡片瀑布。
- 字体不使用大 display hero；后台标题、表格、标签和数字采用紧凑层级。

### Dashboard / Admin 规则

采用 `harness-frontend` dashboard/admin rule pack：

- 管理端是工作面，不做 hero、欢迎语或装饰背景。
- 默认高密度，但保持可扫描。
- KPI 区、筛选区、表格和状态面板优先。
- 标题命名“对象和任务”，避免营销式文案。
- 数据新鲜度、错误、关注项必须能让操作员一眼判断下一步。

## 方案比较

### 方案 A：Design-token first 的 Thymeleaf 深度重构

保留现有页面架构，重构 CSS token、全局 shell、组件样式、主题切换和页面布局。

优点：

- 与当前仓库架构最契合。
- 不增加构建复杂度。
- 可以一次性统一所有页面的视觉语言。
- MockMvc 和 Playwright 可验证响应式、权限、主题切换和页面可用性。

风险：

- 单个 CSS 文件会明显变大，需要按 token、layout、component、page、responsive 分区组织。
- 全页面打磨范围较广，需要清晰任务拆分，避免局部样式互相覆盖。

结论：采用。

### 方案 B：先重做 `docs/prototypes/admin/index.html`，再回迁 Thymeleaf

先做高保真静态原型，再把样式和结构迁移到生产模板。

优点：

- 视觉探索自由度更高。
- 可以先通过浏览器快速比较多个方向。

风险：

- 会产生 prototype 与生产模板的二次同步成本。
- 当前目标已明确，不需要再通过独立原型探索方向。

结论：不采用；如执行中需要视觉校准，可用截图迭代生产页面本身。

### 方案 C：只换 CSS 色板和圆角

保留现有结构，仅改颜色、边框、圆角和少量 hover。

优点：

- 快。
- 回归风险低。

风险：

- 无法解决信息层级、主题系统、移动端、交互状态和专业感问题。
- 不满足“深度打磨”目标。

结论：拒绝。

## 设计决策

采用方案 A。

### 视觉 Thesis

DBFlow Admin 是温暖、克制、纸面感的内部数据库运维控制台：Notion-inspired 的温暖中性和轻边界，加上 Linear/Nacos 式高密度扫描效率。界面不解释自己，而是让管理员快速判断状态、定位风险、追踪审计和完成操作。

### 主题系统

新增主题体系，但只通过 CSS custom properties 和一个轻量 JS 控制器实现。

主题模式：

| 模式 | 行为 |
| --- | --- |
| `system` | 默认模式。根据 `prefers-color-scheme` 自动使用 light 或 dark。 |
| `light` | 显式亮色主题，覆盖系统偏好。 |
| `dark` | 显式暗色主题，覆盖系统偏好。 |

持久化：

- 前端将用户选择写入 `localStorage`，key 为 `dbflow-admin-theme`。
- `html` 或 `body` 上写入 `data-theme="light"` / `data-theme="dark"`。
- 当 localStorage 缺失或值为 `system` 时，使用 `matchMedia('(prefers-color-scheme: dark)')`。
- 不写服务端 session、数据库或 cookie，避免引入后端状态。

切换入口：

- 放在顶栏右侧，与 MCP 状态、配置源、管理员、退出同级。
- 使用紧凑 segmented control 或 icon button menu，标签为“系统 / 亮色 / 暗色”。
- 按钮必须有 `aria-label`、可键盘访问、可见焦点状态。

无 JS 降级：

- CSS 默认是亮色主题。
- JS 不加载时页面仍可完整使用，只是不能显式切换主题。

### 亮色主题

亮色主题继承 `admin-ui-design-system.md` 的温暖纸面感。

核心 token：

| 语义 | 值 |
| --- | --- |
| `--surface-page` | `#f6f5f4` |
| `--surface-canvas` | `#ffffff` |
| `--surface-raised` | `#ffffff` |
| `--surface-subtle` | `#fbfaf8` |
| `--text-strong` | `rgba(0, 0, 0, 0.95)` |
| `--text-main` | `#31302e` |
| `--text-muted` | `#615d59` |
| `--text-weak` | `#a39e98` |
| `--border-whisper` | `rgba(0, 0, 0, 0.10)` |
| `--border-soft` | `rgba(0, 0, 0, 0.07)` |
| `--accent` | `#0075de` |
| `--accent-hover` | `#005bab` |
| `--focus` | `#097fe8` |

亮色主题布局感：

- 页面背景为 warm white，内容面板为 white。
- Sidebar 不再使用重黑蓝背景；采用白色或 warm white 浅侧栏，让管理端更像工作台而不是旧式控制台。
- 当前导航项使用浅蓝底 + 蓝色左侧线或胶囊高亮，不使用整块深蓝填充。
- 表格 header 使用 warm white，不使用冷灰蓝。

### 暗色主题

暗色主题不是亮色反相，而是“温暖石墨纸面”。

核心 token：

| 语义 | 值 |
| --- | --- |
| `--surface-page` | `#191817` |
| `--surface-canvas` | `#23211f` |
| `--surface-raised` | `#2b2926` |
| `--surface-subtle` | `#1f1e1c` |
| `--text-strong` | `rgba(255, 255, 255, 0.94)` |
| `--text-main` | `#f2efeb` |
| `--text-muted` | `#c8c1ba` |
| `--text-weak` | `#918a83` |
| `--border-whisper` | `rgba(255, 255, 255, 0.12)` |
| `--border-soft` | `rgba(255, 255, 255, 0.08)` |
| `--accent` | `#62aef0` |
| `--accent-hover` | `#8cc6f4` |
| `--focus` | `#62aef0` |

暗色主题布局感：

- 背景保持 warm dark，不使用纯黑。
- 表格 hover 不能过亮，使用低透明 warm white overlay。
- 状态 badge 必须保持可读，使用低饱和背景和清晰文字，不只依赖色点。
- SQL/code 区域使用比页面更深一阶的 surface，保留等宽字体和可复制性。

### 语义状态色

状态色在 light/dark 下使用同一语义，不复用装饰色。

| 语义 | Light text | Light bg | Dark text | Dark bg |
| --- | --- | --- | --- | --- |
| OK / Success | `#1a7f45` | `#edf8f1` | `#7bd99b` | `rgba(26, 127, 69, 0.18)` |
| Warn | `#a45500` | `#fff4df` | `#ffbd66` | `rgba(221, 91, 0, 0.18)` |
| Bad / Danger | `#b42318` | `#fff0ee` | `#ff9b91` | `rgba(180, 35, 24, 0.20)` |
| Neutral | `#615d59` | `#f1efed` | `#c8c1ba` | `rgba(255, 255, 255, 0.08)` |
| Info / Accent | `#097fe8` | `#f2f9ff` | `#8cc6f4` | `rgba(98, 174, 240, 0.18)` |

状态组件规则：

- 状态 badge 使用圆点 + 文本，文本必须可读。
- 危险操作按钮和警告 notice 不能只靠颜色表达，必须保留明确文字。
- focus ring 不使用状态色，统一使用 `--focus`。

### 字体与排版

不引入外部 `NotionInter` 字体文件。采用系统字体栈：

```css
--font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
--font-mono: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
```

后台专用层级：

| 用途 | 字号 | 字重 | 行高 |
| --- | --- | --- | --- |
| Page title | 24px | 700 | 1.18 |
| Panel title | 15px | 650 | 1.35 |
| Body / table | 13px | 400 | 1.45 |
| UI label | 12px | 600 | 1.35 |
| Badge | 12px | 600 | 1.25 |
| Metric value | 28px | 700 | 1.05 |
| Mono/code | 12px | 400 | 1.45 |

规则：

- 不使用 hero 级 48px/64px 标题。
- 不使用负 letter-spacing 作为后台通用样式；只允许 page title 轻微压缩。
- 中文界面优先可读性和扫描性。

### 全局 Shell

目标：

- 从旧式左侧深色导航改为温暖轻量工作台 shell。
- 左侧导航保持固定，但减少视觉重量。
- 顶栏从“状态文本堆叠”升级为稳定运行状态条。

布局：

- `.app` 保持 sidebar + main 两列。
- sidebar 桌面宽度 232px，背景使用主题 surface。
- main 背景使用 `--surface-page`。
- topbar 高度 56px，内容紧凑，支持横向溢出处理。

导航：

- 分组标签使用 muted micro label。
- active nav 使用 accent-tinted background、左侧 3px accent bar 或 inset marker。
- hover 使用 subtle background，不改变布局尺寸。
- 审计详情不作为独立主导航项高亮；访问详情时高亮审计列表。

顶栏：

- 左侧显示当前页面 route hint 或 workspace label。
- 右侧显示 MCP 状态、配置源、管理员、主题切换、退出。
- 长配置源文本应省略，不挤压状态和退出按钮。

### 页面布局

总览页：

- KPI 区作为第一工作面，使用 typography-first metric strip。
- 最近审计事件表格和关注事项保持左右分栏，宽屏 7:5 或 8:4。
- 小屏下单列堆叠。
- 环境选择器和刷新操作保持右上角，按钮尺寸稳定。

列表页：

- 筛选区从厚重表单块变为浅色 filter surface。
- 筛选表单响应式折行，移动端单列。
- 表格 header 固定视觉层级，列间距统一。
- 行 hover 使用极轻背景，不改变行高。
- 分页区和结果计数更清晰。

详情页：

- 基础信息使用 definition grid。
- SQL 文本区域用 code surface，支持换行、长文本、暗色可读。
- 时间线从普通 small-row 升级为清晰步骤列表，但不引入图标装饰。

登录页：

- 采用居中 compact auth panel。
- 背景为 warm page surface。
- 登录 box 使用 whisper border + soft shadow。
- 密码显示按钮、错误提示、退出提示使用统一 notice token。

配置/策略/健康页：

- 配置页强调脱敏和来源，不展示大段说明。
- 危险策略页用风险状态和只读说明形成清晰安全边界。
- 健康页按 component/status/detail 分组，异常项在视觉上优先。

### 组件体系

CSS 按以下分区组织：

1. Tokens: theme variables, typography, spacing, radius, shadow.
2. Reset/base: body, links, form base, focus base.
3. Layout: app, sidebar, topbar, content, responsive shell.
4. Components: buttons, inputs, select, panels, tables, badges, notices, drawers, metrics, detail grid.
5. Page patterns: login, overview, audit, health, config.
6. Responsive: desktop/tablet/mobile overrides.

组件规则：

- `.panel` 使用 8px radius、whisper border、无默认重阴影。
- `.btn` 高度不低于 34px；移动端触控不低于 44px。
- `.control` / `.select` 高度不低于 34px，focus ring 清晰。
- `.notice` 按语义状态继承 status token。
- `.drawer` 支持暗色主题、ESC 关闭和 backdrop。
- `.empty` 有统一样式，不使用插画。
- `.mono` 在暗色下保持足够 contrast。

### 交互行为

新增或改造 `admin.js`：

- 初始化主题：读取 localStorage、系统偏好，设置 `data-theme`。
- 监听主题切换按钮。
- 监听系统主题变化，仅在 mode 为 `system` 时更新。
- 保留 drawer open/close 和 password toggle。
- drawer 支持 ESC 关闭；关闭按钮有明确 focus。
- 不新增动画依赖。

动效：

- 只使用 CSS transition，时长 120-180ms。
- 允许 hover/focus 背景、边框、颜色、阴影微变化。
- 不使用数字滚动、复杂 entrance animation、parallax 或页面切换动画。

### 响应式

断点：

- `>= 1180px`: 标准桌面，两列 shell。
- `768px - 1179px`: sidebar 可收窄，内容单列优先。
- `< 768px`: sidebar 变为顶部/抽屉导航，内容 padding 收紧，表格横向滚动。
- `< 480px`: 表单和操作按钮单列，触控目标至少 44px。

移动端原则：

- 管理端移动体验是可用，不追求桌面同等效率。
- 表格不强行卡片化，保留横向滚动和 sticky first important column 作为可选增强。
- 顶栏长信息折叠，主题和退出保留可访问。

### 可访问性

- 所有可点击元素必须是 `button` 或 `a`。
- 主题切换控件必须有 `aria-label` 或文本标签。
- 当前 nav 使用 `aria-current="page"`。
- drawer 打开后应能用 ESC 关闭；关闭按钮可聚焦。
- focus-visible 样式对按钮、链接、输入、select、drawer 控件可见。
- 颜色对比：正文 AA，关键状态和按钮 AA。
- 不只通过颜色表达状态，保留文本。

### 安全边界

UI 打磨不得改变以下边界：

- 不展示 Token 明文，除现有一次性签发/重发 flash。
- 不展示 Token hash、password hash、数据库密码、完整 JDBC URL。
- 不新增非管理员可访问页面。
- 不把主题偏好写入后端，避免引入用户设置存储。
- 不在前端拼接或执行任何 SQL。

## 文件影响

预计修改：

- `src/main/resources/static/admin-assets/css/admin.css`
- `src/main/resources/static/admin-assets/js/admin.js`
- `src/main/resources/templates/admin/fragments/layout.html`
- `src/main/resources/templates/admin/login.html`
- `src/main/resources/templates/admin/overview.html`
- `src/main/resources/templates/admin/users.html`
- `src/main/resources/templates/admin/tokens.html`
- `src/main/resources/templates/admin/grants.html`
- `src/main/resources/templates/admin/config.html`
- `src/main/resources/templates/admin/policies-dangerous.html`
- `src/main/resources/templates/admin/audit-list.html`
- `src/main/resources/templates/admin/audit-detail.html`
- `src/main/resources/templates/admin/health.html`

预计测试新增或调整：

- 管理端 smoke tests：确保主题按钮、关键页面、权限仍可渲染。
- JS 行为测试或 Playwright 检查：主题切换、localStorage、dark/light 截图、drawer ESC。
- CSS/HTML residue scan：避免 inline style、冷灰旧 token、不可达 DESIGN.md 链接。

文档：

- `docs/references/admin-ui-design-system.md` 已作为设计系统标准。
- 本 spec 是 UI 深度打磨的执行依据。
- `docs/PLANS.md` 在后续 `harness-plan` 阶段添加 active plan。

## 验收标准

- [ ] 亮色主题符合温暖中性、白色内容面、轻边界、Notion Blue 单主强调色。
- [ ] 暗色主题符合温暖石墨纸面，不使用纯黑或冷蓝黑，状态色在暗色下可读。
- [ ] 默认支持 `system` 模式，显式 `light` / `dark` 可切换并通过 localStorage 持久化。
- [ ] 所有 admin 页面在 light/dark 下无明显文字重叠、按钮溢出、低对比文本或不可读 code 区。
- [ ] 全局 shell、sidebar、topbar、按钮、表单、表格、badge、notice、drawer、empty state 使用统一 token。
- [ ] 页面保留内部运维控制台密度，不出现 marketing hero、装饰图片、渐变背景、欢迎语或无效说明。
- [ ] 移动端至少可用：登录、导航、总览、列表筛选、表格横向滚动、详情页可阅读。
- [ ] 键盘 focus 可见，主题切换和 drawer 可通过键盘操作。
- [ ] 不新增敏感字段展示，不改变权限和业务数据口径。
- [ ] `./mvnw test`、`python3 scripts/check_harness.py`、`git diff --check` 通过。
- [ ] 使用浏览器或 Playwright 验证 `/login`、`/admin`、`/admin/audit`、`/admin/audit/{id}`、`/admin/users`、`/admin/tokens`、`/admin/grants`、`/admin/config`、`/admin/policies/dangerous`、`/admin/health` 在桌面和移动视口下可用。

## 验证策略

技术验证：

```bash
./mvnw test
python3 scripts/check_harness.py
git diff --check
```

前端视觉/交互验证：

- 启动本地应用后，用浏览器或 Playwright 打开 `/login` 和 `/admin/**` 核查 light/dark。
- 桌面视口：`1440x900`。
- 平板视口：`1024x768`。
- 手机视口：`390x844`。
- 至少保存或人工检查 light/dark 截图，确认文本、表格、状态 badge、drawer、notice、focus ring 可读。
- 检查 localStorage `dbflow-admin-theme` 在 `system`、`light`、`dark` 间切换行为正确。

质量扫描：

```bash
rg -n "style=|#f4f6f9|#1d2733|#146a9f|hero|Welcome back|linear-gradient" src/main/resources/templates/admin src/main/resources/static/admin-assets/css/admin.css
```

预期：

- 旧冷灰蓝核心 token 不再作为主题基础。
- admin 页面不出现营销式 hero 或欢迎语。
- inline style 被移除或仅保留确有必要的 Thymeleaf 小范围兼容场景。

## 实施分解建议

后续 `harness-plan` 应按以下任务拆分：

1. 建立 CSS token 与 light/dark 主题基础。
2. 改造 `admin.js` 主题控制、drawer 键盘行为和无 JS 降级。
3. 改造全局 shell、sidebar、topbar、导航和主题切换入口。
4. 打磨基础组件：button、input/select、panel、table、badge、notice、drawer、empty、metric、detail grid。
5. 逐页打磨登录、总览、用户/Token/授权、配置/策略、审计列表/详情、健康。
6. 添加或调整测试与 Playwright/browser 验证。
7. 更新 active plan 证据并通过 `harness-verify`。

## 已拒绝选择

- 拒绝引入 SPA 或前端构建链。
- 拒绝只做 CSS 换肤。
- 拒绝使用纯黑暗色主题。
- 拒绝在后台使用 marketing hero、装饰插画或大面积渐变。
- 拒绝把主题偏好写入后端。
- 拒绝把状态色用于装饰。
