<!-- Harness brainstorm spec. Safe to edit before approval. -->

# React Admin 登录页中文化与清爽化设计

Date: 2026-05-03

Status: Approved

## 最终修订

用户在实现验收后进一步收敛最终效果：删除左侧主标题、主说明、三条安全信号、`Refinex-DBFlow` eyebrow 和“数据库操作网关”副标题。最终登录页只保留左上角 logo + `DBFlow Admin`，登录表单在页面上下左右居中。

该修订覆盖本文后续关于左侧主文案和安全信号的设计描述。认证协议、CSRF、session store、redirect、错误 toast 和密码显示切换仍保持不变。

## 背景

React 管理端已经完成 `/admin` cutover，`/login` 现在由 `dbflow-admin/src/features/auth/login-page.tsx`
渲染，并通过 `dbflow-admin/src/api/session.ts` 调用 Spring Security JSON 登录协议。当前登录页的功能链路正确：
用户名和密码通过根路径 `/login` 以 `application/x-www-form-urlencoded` 提交，前端携带 CSRF header，成功后写入
内存中的 session store 并按 `redirect` 跳转，失败时显示 toast。

截图暴露的问题集中在用户界面表达，而不是认证机制：

- 首屏英文文本过多，尤其左侧大标题和三张说明卡让内部工具入口显得像营销页。
- 页面应是中文界面，但标题、说明、表单 label、placeholder、按钮和 toast 仍以英文为主。
- 左侧三张卡片信息重复，均在解释 DBFlow 的治理价值，增加视觉重量。
- 背景渐变、卡片阴影和大字号标题叠加后，登录入口不够清爽。

## 目标

- 将 `/login` 调整为中文、克制、清爽的内部管理端登录页。
- 保留 DBFlow Admin 的产品识别，并删除解释型文案。
- 采用 `docs/references/admin-ui-design-system.md` 的 warm neutral、轻边界、低阴影、可扫描层级。
- 遵循 `harness-frontend` 的 app/admin surface 规则：登录页是工作入口，不做营销 hero。
- 保留现有认证协议、安全边界、路由行为、错误处理和测试运行模型。

## 非目标

- 不修改后端 `/login`、`/logout`、`/admin/api/session`、CSRF 或 Spring Security 配置。
- 不改变 session 存储策略；登录状态仍只在当前前端内存状态中承接服务端 session，不写 `localStorage`。
- 不新增 OAuth、社交登录、验证码、忘记密码、注册或多因素认证入口。
- 不改动 `/admin` 认证后页面、侧边栏、Dashboard 或其他业务页面。
- 不引入新的 UI 组件库、字体包、动画库或图片资产。
- 不删除旧模板 auth demo 路由；本规格只处理当前生产 `/login` 页面。

## 用户与界面类型

- Surface type: 内部 Admin 登录页 / form surface。
- Audience: DBFlow 管理员和内部运维人员。
- Product: DBFlow Admin。
- Tech constraints: React、TypeScript、Vite、TanStack Router、shadcn/ui、Tailwind CSS、lucide-react。
- Existing system: `docs/references/admin-ui-design-system.md` 和现有 `dbflow-admin` 组件体系优先。

## 方案比较

### 方案 A：技术克制 + 中等密度中文登录面

保留左右布局，但左侧从“大标题 + 三张解释卡”收敛为“品牌标识 + 短中文定位 + 2 到 3 条扫描型安全信号”。右侧登录表单全部中文化，并减弱卡片阴影与背景装饰。

优点：

- 与内部管理端定位一致，保留产品识别和必要安全上下文。
- 能直接解决英文化和文本堆叠问题。
- 改动集中在 `LoginPage` 与对应测试，风险可控。
- 仍能让首次访问者快速理解这是 DBFlow 管理端登录入口。

风险：

- 如果左侧信号仍写得过长，会退回解释型页面。

结论：采用。

### 方案 B：极简登录，仅保留表单

删除左侧说明区，只保留居中的品牌、登录卡片和一行系统提示。

优点：

- 最清爽，视觉负担最低。
- 实现范围最小。

风险：

- DBFlow Admin 的产品识别和安全边界提示不足。
- 对首次部署或共享内网入口的管理员不够明确。

结论：最终采用；左上角保留 logo + `DBFlow Admin`，表单居中。

### 方案 C：安全控制台感更强

保留左右布局，并强化“会话校验 / 权限边界 / 审计留痕”三条控制台式安全提示。

优点：

- 企业内控感更强，安全语义更明确。
- 与 DBFlow 的审计、策略、授权价值高度一致。

风险：

- 仍可能显得文本偏多。
- 对登录页这个低频入口来说信息密度略高。

结论：不采用。

## 采用设计

### Visual Thesis

DBFlow 登录页应像一个温暖、克制、可信的内部控制面入口：纸面感背景、轻边界表单、紧凑中文标题和少量安全信号，避免营销式大标题和说明卡堆叠。

### Content Plan

页面保持左右结构，但信息层级重新收敛：

1. 左侧品牌区：DBFlow Admin、中文副标题“数据库操作网关”。
2. 左侧主文案：一句中文标题，表达登录后的工作对象，不使用英文长句。
3. 左侧安全信号：2 到 3 条短标签，每条最多一行标题和一行短说明。
4. 右侧登录表单：中文标题、中文说明、用户名、密码、显示密码按钮、登录按钮。

### Interaction Thesis

- 密码显示切换保留，`sr-only` 文案中文化为“显示密码”和“隐藏密码”。
- 提交中按钮保留 loader，按钮文字保持“登录”，不使用额外状态文案。
- 成功 toast 中文化为“已登录 DBFlow Admin”。
- 失败 toast 使用后端错误消息；非 API 错误 fallback 中文化为“登录失败，请检查用户名和密码。”
- 无额外入场动画，保留现有 focus、hover、disabled 状态。

## 具体文案决策

### 左侧品牌与定位

- 品牌 eyebrow: `Refinex-DBFlow`
- 产品名: `DBFlow Admin`
- 产品定位: `数据库操作网关`
- 主标题: `登录后管理数据库访问边界`
- 主说明: `统一管理用户、授权、Token、危险 SQL 策略和审计记录。`

### 安全信号

采用三条短信号，控制在单屏内可扫描：

| 标题 | 说明 | 图标 |
| --- | --- | --- |
| `服务端会话` | `登录状态由 DBFlow 服务端校验。` | `KeyRound` |
| `策略前置` | `授权与高危 SQL 策略在服务端执行。` | `Shield` |
| `审计留痕` | `敏感操作可追踪，不暴露密钥明文。` | `Database` |

### 登录卡片

- Card title: `管理员登录`
- Card description: `使用 DBFlow 管理员账号进入控制台。`
- Username label: `用户名`
- Username placeholder: `admin`
- Password label: `密码`
- Password placeholder: `输入密码`
- Submit button: `登录`
- Validation username: `请输入用户名。`
- Validation password: `请输入密码。`
- Show password: `显示密码`
- Hide password: `隐藏密码`
- Success toast: `已登录 DBFlow Admin`
- Generic failure toast: `登录失败，请检查用户名和密码。`

## 布局与视觉规则

- 页面背景从当前强渐变收敛为 warm neutral 背景；可以保留非常轻的径向层次，但不能成为第一视觉焦点。
- 左侧取消三张大卡片堆叠，改为轻量列表或横向/纵向 signal rows。
- 主标题不超过两行，桌面端字号控制在 `text-3xl` 到 `text-4xl`，不使用 `md:text-5xl` 级别的大 hero。
- 登录卡片保留 `Card` 语义，但降低阴影；边界使用 `border` 和 `bg-background/90`。
- 桌面端继续左右布局；移动端单列，品牌、短标题、表单依次出现，安全信号可以压缩到表单下方或标题下方。
- 使用现有 shadcn/ui `Button`、`Input`、`Form`、`Card`，不新增组件库。
- 图标继续使用 `lucide-react`，不使用 emoji 或自定义 SVG。
- 颜色只使用现有 Tailwind token：`background`、`foreground`、`muted`、`muted-foreground`、`primary`、`border`。

## 文件边界

预计实现只修改：

- `dbflow-admin/src/features/auth/login-page.tsx`
- `dbflow-admin/src/features/auth/login-page.test.tsx`

允许在必要时修改：

- `dbflow-admin/src/styles/theme.css` 或 `dbflow-admin/src/styles/index.css`，仅限登录页需要的通用 token 已不存在时。

不得修改：

- `dbflow-admin/src/api/session.ts`
- `dbflow-admin/src/stores/session-store.ts`
- `dbflow-admin/src/routes/(auth)/login.tsx`
- 后端 `src/main/java/**` 认证、安全、Session API 代码。

## 测试策略

### 组件测试

更新 `dbflow-admin/src/features/auth/login-page.test.tsx`：

- 断言中文品牌、定位、短标题、安全信号和登录控件存在。
- 断言用户名、密码、登录按钮使用中文可访问名称。
- 断言密码显示切换的 accessible name 为“显示密码 / 隐藏密码”。
- 断言 API client 错误仍展示后端消息。
- 断言非 API 错误 fallback 为中文。
- 断言成功登录仍写入 session 并导航到 redirect。
- 断言成功 toast 为“已登录 DBFlow Admin”。

### 构建与质量验证

执行计划完成时需要运行：

- `pnpm --dir dbflow-admin exec vitest run --browser.headless src/features/auth/login-page.test.tsx`
- `pnpm --dir dbflow-admin build`
- `pnpm --dir dbflow-admin lint`
- `python3 scripts/check_harness.py`
- `git diff --check`

如果改动只触及 React 登录页，`./mvnw test` 可作为最终计划中的扩展回归，由执行阶段根据实际 diff 决定是否运行；若改动触碰后端或打包资源映射，则必须运行。

## 验收标准

- AC-1: `/login` 首屏所有用户可见核心文案为中文，保留 `DBFlow Admin` 产品名。
- AC-2: 登录页不再展示英文大标题、英文说明卡、英文表单 label、英文 placeholder、英文按钮或英文 fallback toast。
- AC-3: 左侧解释内容显著减负，不再使用三张大说明卡堆叠；安全信号为短中文扫描项。
- AC-4: 登录协议、CSRF、session store、redirect 归一化、失败 toast 和密码显示切换行为保持不变。
- AC-5: 移动端保持单列可读，无横向滚动，表单控件触控目标不低于现有 shadcn/ui 默认尺寸。
- AC-6: 登录页 focused test、frontend build、frontend lint、Harness validation 和 `git diff --check` 通过。

## 风险与缓解

| 风险 | 可能性 | 缓解 |
| --- | --- | --- |
| 中文化测试只改快照式文本，遗漏行为回归 | Medium | 保留登录失败、成功跳转、session 写入和密码切换行为断言。 |
| 视觉减负后产品识别不足 | Low | 保留 `Refinex-DBFlow`、`DBFlow Admin` 和“数据库操作网关”定位。 |
| 过度改 CSS 影响其他 Admin 页面 | Low | 默认只使用现有 utility class，不改全局 token；如需 CSS，限定为必要通用 token。 |
| 后端错误消息仍可能是英文 | Medium | API client 错误继续透传后端消息；本规格只保证前端 fallback 文案中文化。 |

## Handoff

用户批准本 spec 后，下一步进入 `harness-plan`，生成可执行计划。执行阶段应先更新测试断言，再修改登录页实现，最后运行 focused frontend test、build、lint、Harness validation 和 diff check。
