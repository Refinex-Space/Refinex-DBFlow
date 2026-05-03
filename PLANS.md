# Refinex-DBFlow React Admin 0-1 落地计划

> 文件建议位置：项目根目录 `PLANS.md`，或落入 `docs/exec-plans/active/2026-05-02-react-admin-spa.md` 后在 `docs/PLANS.md`
> 建索引。  
> 执行方式：下面每一个「Prompt」都是一个可以单独复制给 Codex 的任务。必须按顺序执行；标注“可并行”的任务只允许在前置阶段完成后并行。

---

## 0. 本次任务

- 深度盘点 Refinex-DBFlow 当前 Thymeleaf 管理端已有页面、交互能力、后端服务边界和安全边界。
- 基于最终决策定版新的 React 后台技术栈、工程位置、路由策略、部署策略和模板引入方式。
- 制定从 0 到 1 引入 `satnaing/shadcn-admin`、重命名、裁剪、接入 Spring Boot、补齐 JSON API、迁移页面、验证和切换 `/admin`
  的完整开发计划。
- 把开发工作拆成阶段化 AI Prompt，后续可逐条复制给 Codex 实施。

---

## 1. 当前项目管理端现状理解

### 1.1 当前架构

Refinex-DBFlow 当前是单模块 Spring Boot WebMVC Maven 项目，管理端不是 SPA，而是：

```text
Spring Boot 3.5.x
Spring MVC Controller
Spring Security Form Login Session
Thymeleaf Templates
/static/admin-assets/css/admin.css
/static/admin-assets/js/admin.js
```

关键入口：

```text
/login                       管理端登录页
/admin                       总览页
/admin/users                 用户管理
/admin/grants                项目环境授权
/admin/tokens                MCP Token 管理
/admin/config                配置查看
/admin/policies/dangerous    危险策略
/admin/audit                 审计列表
/admin/audit/{eventId}       审计详情
/admin/health                系统健康
/admin/api/audit-events      审计 JSON API
/mcp                         MCP Streamable HTTP endpoint
```

当前模板目录：

```text
src/main/resources/templates/admin/
├── fragments/layout.html
├── login.html
├── overview.html
├── users.html
├── grants.html
├── tokens.html
├── config.html
├── policies-dangerous.html
├── audit-list.html
├── audit-detail.html
└── health.html
```

当前静态资源：

```text
src/main/resources/static/admin-assets/
├── css/admin.css
├── js/admin.js
└── img/dbflow-logo.svg
```

当前安全边界：

```text
/admin/**, /login, /logout, /admin-assets/** 使用管理端 Spring Security session 链
/login 和 /admin-assets/** 可匿名访问
/admin/** 需要 ROLE_ADMIN
/mcp 使用独立 Bearer Token 安全链，不复用管理端 session
Spring Security CSRF 当前启用
```

### 1.2 当前页面与能力清单

| 页面        | 路由                          | 当前能力                                                                 | 后续 React 迁移形态                                                              |
|-----------|-----------------------------|----------------------------------------------------------------------|----------------------------------------------------------------------------|
| 登录页       | `/login`                    | 自定义登录视觉、用户名密码、密码显隐、主题切换、错误/退出提示                                      | React 登录页，继续走 Spring Security session，补 JSON login success/failure handler |
| 总览        | `/admin`                    | 24h 指标卡、环境选择、最近审计事件、需要关注事项                                           | Dashboard，TanStack Query 拉取 `/admin/api/overview`，Recharts/Metric Cards    |
| 用户管理      | `/admin/users`              | 用户列表、username/status 筛选、新建用户、禁用/启用、重置密码、跳转授权                         | React users feature，DataTable + Sheet Form + AlertDialog                   |
| 项目授权      | `/admin/grants`             | 按用户×项目分组，查看已授权环境，编辑环境勾选，新建授权，筛选                                      | React grants feature，Permission Matrix / Project Env Checkbox Editor       |
| Token 管理  | `/admin/tokens`             | Token 列表、筛选、颁发、重新颁发、吊销、一次性明文弹窗、复制                                    | React tokens feature，TokenRevealDialog，危险操作确认                              |
| 配置查看      | `/admin/config`             | 脱敏展示 project/env/datasource/JDBC host/schema/username/Hikari 限制/同步状态 | React config feature，只读配置表格与详情 Drawer                                      |
| 危险策略      | `/admin/policies/dangerous` | 默认策略、DROP 白名单、固定强化规则、拒绝原因 Drawer                                     | React policy feature，Policy Rule Cards + Matrix + Reason Sheet             |
| 审计列表      | `/admin/audit`              | userId/project/env/risk/decision/sqlHash/tool/size 筛选、分页、详情跳转        | React audit list，使用已有 `/admin/api/audit-events`，服务端分页                      |
| 审计详情      | `/admin/audit/{eventId}`    | 身份/项目/工具/风险/SQL Hash/SQL 文本/拒绝原因/时间线                                 | React audit detail，Monaco readonly + Timeline + Copy Controls              |
| 系统健康      | `/admin/health`             | metadata DB、target pool、Nacos、MCP endpoint 健康项                       | React health feature，Health Cards + Refresh                                |
| 共享 Layout | 所有 admin 页面                 | 侧边栏、用户菜单、主题、Drawer、Toast、自定义 Select                                  | shadcn Sidebar + Header + Command Menu + Sonner + ThemeProvider            |

### 1.3 当前前端的优点

- 已经具备完整的管理端产品边界，不是空白后台。
- 服务端已经有较多安全脱敏逻辑，尤其配置、审计、Token 明文、JDBC URL/password 不展示。
- `AdminAccessManagementService` 已经沉淀用户、Token、授权服务能力。
- `AdminOperationsViewService` 已经沉淀配置、策略、审计、健康的只读视图聚合能力。
- `AdminAuditEventController` 已经有审计 JSON API，可作为 React API 风格参考。
- 当前 Thymeleaf 页面可作为 React 迁移的业务原型，不需要重新做产品设计。

### 1.4 当前前端的限制

- SSR + 表单 POST + 重定向，不适合继续扩展成专业现代后台。
- CSS/JS 集中在 `admin.css` 和 `admin.js`，组件边界弱。
- 抽屉、Toast、自定义 Select、主题切换等交互是手写 DOM 逻辑，后续维护成本高。
- 大多数页面没有 JSON API，React 不能直接复用当前 Controller，需要补齐 API 层。
- 表格能力较基础，缺少列显隐、服务端排序、URL 状态同步、批量操作、详情抽屉等现代后台能力。
- Token 明文一次性展示逻辑当前依赖 Thymeleaf flash，需要改成 JSON mutation 返回后的 React 一次性 Dialog。
- 当前登录是 Spring Security 默认表单流程，SPA 需要补 JSON 登录/登出/session 状态协议。

---

## 2. 最终定版决策

### 2.1 技术栈

```text
React
TypeScript
Vite
TanStack Router
TanStack Query
TanStack Table
Zustand
shadcn/ui
Radix UI
Tailwind CSS
Lucide React
Sonner
React Hook Form
Zod
Recharts
Monaco Editor / @monaco-editor/react
Axios
Vitest
Playwright optional
```

### 2.2 模板基座

使用：

```text
satnaing/shadcn-admin
```

采用方式：

```text
不要长期 fork
不要把整个模板原封不动当产品
使用“一次性复制 + 裁剪 + 重命名 + 产品化重建”方式
```

原因：

- 它是 Vite + React + shadcn/ui + Tailwind + TanStack Router 的现代后台基座。
- 已有 Sidebar、Header、Theme、Command Search、DataTable、Auth Pages、Settings、Error Pages 等参考实现。
- 视觉和交互更接近现代 SaaS 控制台，而不是 Ant Design Pro / Element Plus 传统企业后台。
- 适合 Refinex-DBFlow 的技术产品定位：MCP Gateway Console、Database Control Plane、Security Audit Console。

### 2.3 工程位置

最终新增：

```text
Refinex-DBFlow/
├── dbflow-admin/                  # React SPA 前端工程
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── components.json
├── src/main/java/...              # Spring Boot 后端
├── src/main/resources/static/
│   └── admin-next/                # React dist 初期输出位置
└── pom.xml
```

阶段性路由策略：

```text
阶段 1-8：React 新后台挂在 /admin-next，保留 Thymeleaf /admin
阶段 9：验收通过后，将 /admin 切到 React SPA，Thymeleaf 改为 /admin-legacy 或删除
```

原因：

- 避免开发过程破坏现有后台。
- 允许逐页迁移与对照验证。
- 方便内网部署先试运行 `/admin-next`，稳定后再 cutover。

### 2.4 部署策略

开发环境：

```text
cd dbflow-admin
pnpm install
pnpm dev

Vite dev server: http://localhost:5173/admin-next
Proxy:
  /admin/api/** -> http://localhost:8080
  /login        -> http://localhost:8080
  /logout       -> http://localhost:8080
  /actuator/**  -> http://localhost:8080
```

生产初期：

```text
pnpm --dir dbflow-admin build
输出 dist 到 src/main/resources/static/admin-next
Spring Boot jar 直接服务 /admin-next/**
```

后续也可支持 Nginx 独立部署，但本计划优先保证单 jar 内网投放能力。

---

## 3. 新 React 管理端目标信息架构

```text
/admin-next/login
/admin-next
/admin-next/users
/admin-next/grants
/admin-next/tokens
/admin-next/config
/admin-next/policies/dangerous
/admin-next/audit
/admin-next/audit/:eventId
/admin-next/health
/admin-next/settings/appearance
/admin-next/errors/401
/admin-next/errors/403
/admin-next/errors/404
/admin-next/errors/500
```

Cutover 后：

```text
/login
/admin
/admin/users
/admin/grants
/admin/tokens
/admin/config
/admin/policies/dangerous
/admin/audit
/admin/audit/:eventId
/admin/health
```

导航分组：

```text
工作台
  总览

身份与访问
  用户管理
  项目授权
  Token 管理

配置与策略
  配置查看
  危险策略

审计
  审计列表

运维
  系统健康

设置
  外观设置
```

---

## 4. 需要从 shadcn-admin 保留的内容

保留并改造：

```text
src/main.tsx
src/styles/index.css
src/context/theme-provider.tsx
src/context/font-provider.tsx
src/context/layout-provider.tsx
src/context/search-provider.tsx
src/components/layout/authenticated-layout.tsx
src/components/layout/app-sidebar.tsx
src/components/layout/header.tsx
src/components/layout/main.tsx
src/components/layout/nav-group.tsx
src/components/layout/nav-user.tsx
src/components/search.tsx
src/components/theme-switch.tsx
src/components/profile-dropdown.tsx
src/components/ui/**
src/components/data-table/**
src/hooks/use-table-url-state.ts
src/lib/utils.ts
src/lib/handle-server-error.ts
src/routes/(errors)/**
src/routes/_authenticated/route.tsx
```

可保留作为参考但最终重写：

```text
src/routes/(auth)/sign-in.tsx
src/routes/(auth)/sign-in-2.tsx
src/features/users/**
src/features/tasks/**
src/features/settings/appearance/**
```

---

## 5. 需要从 shadcn-admin 删除的内容

删除或重建：

```text
src/routes/clerk/**
src/assets/clerk-logo.*
src/features/auth/sign-up*
src/features/auth/forgot-password*
src/features/auth/otp*
src/features/apps/**
src/features/chats/**
src/features/help-center/**
src/features/tasks/**
src/features/users/data/users.ts
src/features/users/data/schema.ts
src/features/users/data/data.ts 中与示例角色相关内容
src/components/layout/team-switcher.tsx 的 demo team 数据依赖
src/components/layout/data/sidebar-data.ts 的 demo 导航
```

从依赖中删除：

```text
@clerk/react
```

如未使用，也删除：

```text
@radix-ui/react-direction     # 如果最终不保留 RTL
input-otp                     # 如果不做 OTP 页面
@faker-js/faker               # 如果不再使用 demo data
```

注意：不要删除 `src/components/ui/**` 里 shadcn 组件，除非确认完全不用。

---

## 6. 需要补充的新前端模块

```text
dbflow-admin/src/
├── api/
│   ├── client.ts
│   ├── csrf.ts
│   ├── session.ts
│   ├── overview.ts
│   ├── users.ts
│   ├── grants.ts
│   ├── tokens.ts
│   ├── config.ts
│   ├── policies.ts
│   ├── audit.ts
│   └── health.ts
├── types/
│   ├── api.ts
│   ├── session.ts
│   ├── overview.ts
│   ├── access.ts
│   ├── token.ts
│   ├── config.ts
│   ├── policy.ts
│   ├── audit.ts
│   └── health.ts
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── users/
│   ├── grants/
│   ├── tokens/
│   ├── config/
│   ├── policies/
│   ├── audit/
│   ├── health/
│   └── settings/
├── components/dbflow/
│   ├── risk-badge.tsx
│   ├── decision-badge.tsx
│   ├── env-badge.tsx
│   ├── status-badge.tsx
│   ├── metric-card.tsx
│   ├── page-header.tsx
│   ├── copy-button.tsx
│   ├── confirm-action-dialog.tsx
│   ├── token-reveal-dialog.tsx
│   ├── sql-code-viewer.tsx
│   ├── audit-timeline.tsx
│   ├── empty-state.tsx
│   └── data-table/
├── stores/
│   ├── session-store.ts
│   └── ui-store.ts
└── lib/
    ├── query-keys.ts
    ├── routes.ts
    ├── format.ts
    ├── badges.ts
    └── errors.ts
```

---

## 7. 需要补充的新后端 API

保留已有：

```text
GET /admin/api/audit-events
GET /admin/api/audit-events/{id}
```

新增：

```text
GET  /admin/api/session
POST /admin/api/session/login        # 或继续 POST /login，但要支持 JSON success/failure
POST /admin/api/session/logout       # 或继续 POST /logout，但要支持 JSON success

GET  /admin/api/shell
GET  /admin/api/overview

GET  /admin/api/users
POST /admin/api/users
POST /admin/api/users/{userId}/disable
POST /admin/api/users/{userId}/enable
POST /admin/api/users/{userId}/reset-password

GET  /admin/api/grants
GET  /admin/api/grants/options
POST /admin/api/grants
POST /admin/api/grants/update-project
POST /admin/api/grants/{grantId}/revoke

GET  /admin/api/tokens
POST /admin/api/tokens
POST /admin/api/tokens/{tokenId}/revoke
POST /admin/api/users/{userId}/tokens/reissue

GET  /admin/api/config
GET  /admin/api/policies/dangerous
GET  /admin/api/health
```

API 统一返回：

```json
{
  "success": true,
  "data": {},
  "errorCode": null,
  "message": null
}
```

继续复用当前 `ApiResult`。

安全要求：

- 所有 `/admin/api/**` 仍需要 `ROLE_ADMIN`。
- 所有 mutation 必须走 CSRF。
- Token 明文只允许在 `POST /admin/api/tokens` 和 `POST /admin/api/users/{userId}/tokens/reissue` 成功响应中出现一次。
- 列表、详情、审计、日志、配置中禁止出现 Token 明文、Token hash、password hash、数据库密码、完整 JDBC URL。
- 配置页面只能展示脱敏字段：host、port、schema、username、pool limits、sync status。

---

## 8. 大阶段与执行顺序

```text
阶段 1：引入 React 工程骨架
阶段 2：前端依赖裁剪、重命名与本地构建
阶段 3：Spring Boot 集成 SPA 静态资源与 /admin-next fallback
阶段 4：Spring Security session / CSRF / JSON 登录协议
阶段 5：补齐管理端 JSON API
阶段 6：前端 API Client、Session、路由保护、布局导航
阶段 7：Dashboard 与基础 DBFlow 组件体系
阶段 8：用户、授权、Token 三个访问管理页面
阶段 9：配置、危险策略、健康三个运维页面
阶段 10：审计列表与审计详情
阶段 11：质量、文档、打包、内网部署
阶段 12：Cutover：/admin 从 Thymeleaf 切到 React
```

---

# 阶段 1：引入 React 工程骨架

## 阶段目标

把 `satnaing/shadcn-admin` 作为一次性模板源复制进 `dbflow-admin/`，完成初始重命名，不改动 Spring Boot 运行逻辑。

## Prompt 1.1：创建 `dbflow-admin` 并复制模板

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：把 satnaing/shadcn-admin 作为一次性模板源复制到项目内 `dbflow-admin/`，不要 fork，不要改动现有 Spring Boot 后端逻辑。

步骤：
1. 在仓库根目录创建 `dbflow-admin/`。
2. 从 `https://github.com/satnaing/shadcn-admin` 的 `main` 分支复制源码到 `dbflow-admin/`。
3. 不复制 `.git/`、`.github/`、`node_modules/`、`dist/`、`.next/` 等无关目录。
4. 在 `dbflow-admin/README.md` 顶部增加说明：本工程基于 `satnaing/shadcn-admin` 一次性导入并二次开发，用于 Refinex-DBFlow React 管理端。
5. 在 `dbflow-admin/NOTICE.md` 中记录上游项目、仓库地址、导入日期、License 为 MIT。
6. 修改根目录 `.gitignore`，追加：
   - `dbflow-admin/node_modules/`
   - `dbflow-admin/dist/`
   - `dbflow-admin/.vite/`
   - `dbflow-admin/playwright-report/`
   - `dbflow-admin/test-results/`

验收：
- `dbflow-admin/package.json` 存在。
- `dbflow-admin/src/main.tsx` 存在。
- `dbflow-admin/NOTICE.md` 存在。
- 不改动 `pom.xml`、`src/main/java/**`、`src/main/resources/templates/**`。
```

## Prompt 1.2：重命名 package 与基础品牌信息

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：把模板 package 和基础品牌信息重命名为 DBFlow Admin。

修改：
1. 修改 `dbflow-admin/package.json`：
   - `name` 改为 `refinex-dbflow-admin`
   - `private` 改为 `true`
   - `version` 改为与后端一致的 `0.1.0-SNAPSHOT` 或 `0.1.0`
2. 搜索 `Shadcn Admin`、`shadcn-admin`、`satnaing`、`Acme`、`Clerk` 等模板品牌文本。
3. 页面可见品牌改为：
   - 产品名：`Refinex-DBFlow`
   - 后台名：`DBFlow Admin`
   - 副标题：`MCP SQL Gateway`
4. 不要删除 License/NOTICE 中对上游的说明。
5. 暂时保留模板 demo 页面，后续阶段再统一删除。

验收：
- `pnpm --dir dbflow-admin install` 可执行。
- `pnpm --dir dbflow-admin build` 能构建成功，若因模板已有 browser test/Playwright 依赖导致非 build 问题，不在本任务处理。
```

---

# 阶段 2：前端依赖裁剪、路由裁剪与模板清理

## 阶段目标

删除 Clerk、示例应用、示例聊天、示例任务等与 DBFlow 无关的内容，只保留现代后台骨架、Layout、Theme、Sidebar、DataTable、Error
Pages 和可复用 UI 组件。

## Prompt 2.1：删除 Clerk 与模板认证演示

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：删除 shadcn-admin 中 Clerk 相关代码与依赖，为后续接入 Spring Security session 登录做准备。

删除：
- `dbflow-admin/src/routes/clerk/**`
- `dbflow-admin/src/assets/clerk-logo*`
- 所有 `ClerkLogo` import 和 `Secured by Clerk` 导航项
- `@clerk/react` package dependency

修改：
- `dbflow-admin/src/components/layout/data/sidebar-data.ts` 中删除 Clerk 分组。
- 如果 TypeScript 因 Clerk routeTree 生成文件报错，重新生成/更新 TanStack Router route tree，或按模板方式运行构建脚本触发生成。

验收：
- `grep -R "@clerk\|Clerk\|clerk" dbflow-admin/src dbflow-admin/package.json` 不应再出现业务代码引用；NOTICE/README 中上游说明除外。
- `pnpm --dir dbflow-admin build` 通过。
```

## Prompt 2.2：删除无关 demo features 与 routes

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：删除模板中与 DBFlow 无关的 demo 页面，保留可复用组件和必要错误页。

删除或清空：
- `dbflow-admin/src/features/apps/**`
- `dbflow-admin/src/features/chats/**`
- `dbflow-admin/src/features/help-center/**`
- `dbflow-admin/src/features/tasks/**`
- demo users data：`dbflow-admin/src/features/users/data/users.ts` 等示例数据文件
- 对应 routes：`apps`、`chats`、`help-center`、`tasks`、demo `users` route，后续 DBFlow users 会重建

保留：
- `dbflow-admin/src/components/ui/**`
- `dbflow-admin/src/components/data-table/**`
- `dbflow-admin/src/components/layout/**`
- `dbflow-admin/src/components/search.tsx`
- `dbflow-admin/src/components/theme-switch.tsx`
- `dbflow-admin/src/routes/(errors)/**`
- `dbflow-admin/src/context/**`
- `dbflow-admin/src/hooks/use-table-url-state.ts`
- `dbflow-admin/src/lib/**`

重建一个临时 dashboard：
- `dbflow-admin/src/routes/_authenticated/index.tsx`
- `dbflow-admin/src/features/dashboard/index.tsx`
- 内容先显示 `DBFlow Admin`、`MCP SQL Gateway`、`React shell is ready`。

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 运行后 `/` 或当前默认 authenticated index 能看到 DBFlow 临时 Dashboard。
```

## Prompt 2.3：重建 DBFlow Sidebar 导航

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：用 DBFlow 后台信息架构替换模板 sidebar demo 数据。

修改 `dbflow-admin/src/components/layout/data/sidebar-data.ts`：
1. user 先使用占位：`admin` / `DBFlow Administrator`，后续会接 session API。
2. teams 只保留一个：
   - name: `DBFlow Admin`
   - plan: `MCP SQL Gateway`
   - logo: 使用 `Database` 或 `Command` lucide icon
3. navGroups 改为：
   - 工作台：总览 `/`
   - 身份与访问：用户管理 `/users`，项目授权 `/grants`，Token 管理 `/tokens`
   - 配置与策略：配置查看 `/config`，危险策略 `/policies/dangerous`
   - 审计：审计列表 `/audit`
   - 运维：系统健康 `/health`
   - 设置：外观设置 `/settings/appearance`
4. 图标统一使用 `lucide-react`：`LayoutDashboard`, `Users`, `Shield`, `KeyRound`, `Settings`, `TriangleAlert`, `ScrollText`, `Activity`, `Palette` 等。

验收：
- Sidebar 不再出现 demo 导航。
- `pnpm --dir dbflow-admin build` 通过。
```

---

# 阶段 3：Spring Boot 集成 `/admin-next` 静态资源

## 阶段目标

让 React 构建产物可以被 Spring Boot 以 `/admin-next/**` 服务，同时保留现有 `/admin` Thymeleaf 页面。

## Prompt 3.1：配置 Vite base、dev proxy 与 build 输出

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：配置 `dbflow-admin` 支持 `/admin-next/` 子路径部署和后端 API 代理。

修改 `dbflow-admin/vite.config.ts`：
1. 设置 `base: '/admin-next/'`。
2. 配置 dev server：
   - port: 5173
   - proxy `/admin/api` 到 `http://localhost:8080`
   - proxy `/login` 到 `http://localhost:8080`
   - proxy `/logout` 到 `http://localhost:8080`
   - proxy `/actuator` 到 `http://localhost:8080`
3. build outDir 保持 `dist`。

新增或修改 `dbflow-admin/package.json` scripts：
- `dev`: `vite --host 0.0.0.0`
- `build`: 保持 `tsc -b && vite build`
- `preview`: `vite preview --host 0.0.0.0`

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 构建后的 `dbflow-admin/dist/index.html` 引用资源路径以 `/admin-next/` 开头。
```

## Prompt 3.2：新增 Maven 前端构建 Profile

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：新增一个可选 Maven profile，用于构建 React admin 并把 dist 复制进 Spring Boot static 目录。默认 `./mvnw test` 不强制执行 Node 构建。

修改根 `pom.xml`：
1. 新增 profile：`react-admin`。
2. 在 profile 中使用 `frontend-maven-plugin` 或 `exec-maven-plugin` 调用：
   - `pnpm --dir dbflow-admin install --frozen-lockfile`
   - `pnpm --dir dbflow-admin build`
3. 把 `dbflow-admin/dist/**` 复制到：
   - `target/generated-resources/admin-next/`，再参与 resources；或
   - 直接复制到 `src/main/resources/static/admin-next/`（不推荐长期写源目录）。
4. 推荐方式：复制到 `target/classes/static/admin-next/` 或 Maven resources 的 generated resources 路径，避免污染源代码。
5. 确保默认 Maven 生命周期不因本机没有 Node/pnpm 而失败。

验收：
- `./mvnw test` 不执行前端构建且通过。
- `./mvnw -Preact-admin -DskipTests package` 会构建前端并把产物包含进 jar。
```

## Prompt 3.3：新增 `/admin-next` SPA fallback Controller

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：让 Spring Boot 在生产 jar 中服务 React SPA 的 `/admin-next/**` 路由，同时不影响现有 `/admin/**` Thymeleaf。

新增后端类：
- `src/main/java/com/refinex/dbflow/admin/controller/AdminSpaController.java`

要求：
1. 处理 GET `/admin-next` 和 `/admin-next/**`。
2. 如果请求路径是实际静态资源，例如 `.js`、`.css`、`.svg`、`.png`，不要 fallback 到 index。
3. 对非资源路径返回 forward 到 `/admin-next/index.html`。
4. 保留 `/admin` 原 Thymeleaf Controller 不变。

安全配置：
1. 修改 `AdminSecurityConfiguration` 的 securityMatcher 和 authorize rules，把 `/admin-next/**` 加入管理端安全链。
2. `/admin-next/assets/**`、`/admin-next/favicon*` 等静态资源可以匿名访问，但 SPA 页面是否匿名要谨慎：
   - 登录页阶段可能需要匿名访问 `/admin-next/login`。
   - 受保护路由由 React + session API 处理，但后端 `/admin/api/**` 仍必须保护。
3. 最低要求：`/admin/api/**` 必须 ROLE_ADMIN；静态资源可以 permitAll。

测试：
- 新增 `AdminSpaControllerTests`。
- 断言 `/admin-next`、`/admin-next/users` 在存在 index.html 时 forward 到 SPA。
- 断言 `/admin` 仍返回 Thymeleaf 总览。

验收：
- `./mvnw -Dtest=AdminSpaControllerTests,AdminUiControllerTests test` 通过。
```

---

# 阶段 4：Spring Security session、CSRF 与 JSON 登录协议

## 阶段目标

React 管理端仍使用 Spring Security session，但登录、登出、当前用户、CSRF 能用 JSON/HTTP 协议与 SPA 协作。

## Prompt 4.1：配置 CSRF Cookie 与 SPA API 兼容

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：让 React SPA 可以读取 CSRF token 并在 mutation 请求中携带。

修改：
- `src/main/java/com/refinex/dbflow/security/configuration/AdminSecurityConfiguration.java`

要求：
1. 使用 `CookieCsrfTokenRepository.withHttpOnlyFalse()`，cookie 名可保持 Spring 默认 `XSRF-TOKEN`。
2. 前端请求头使用 `X-XSRF-TOKEN`。
3. 不关闭 CSRF。
4. `/mcp` 安全链不受影响。
5. 保留现有 Thymeleaf 表单 CSRF 能力；如果 Thymeleaf form 之前依赖默认 hidden token，应确认仍能工作。

测试：
- 补充 `AdminSecurityTests` 或新增 `AdminCsrfSpaTests`。
- GET `/login` 或 `/admin-next/login` 后响应应包含/触发 CSRF token。
- 未携带 CSRF 的 POST `/admin/api/users` 应被拒绝。
- 携带 CSRF 的 POST 后续由 API 测试覆盖。

验收：
- `./mvnw -Dtest=AdminCsrfSpaTests,AdminSecurityTests test` 通过。
```

## Prompt 4.2：新增 session API

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：新增 React SPA 所需的当前 session API。

新增：
- `src/main/java/com/refinex/dbflow/admin/controller/AdminSessionApiController.java`
- 如需要，新增 DTO：`src/main/java/com/refinex/dbflow/admin/dto/AdminSessionResponse.java`

接口：
- `GET /admin/api/session`

返回：
```json
{
  "authenticated": true,
  "username": "admin",
  "displayName": "admin",
  "roles": ["ROLE_ADMIN"],
  "shell": {
    "adminName": "admin",
    "mcpStatus": "HEALTHY",
    "mcpTone": "ok",
    "configSourceLabel": "Local application config"
  }
}
```

要求：

1. 已登录管理员返回 session 信息。
2. 未登录返回 401 JSON，不要返回 HTML。
3. 复用 `AdminShellViewService`。
4. 不返回 password hash、Token、任何敏感配置。

测试：

- 已认证 MockMvc user 访问 `/admin/api/session` 返回 success true。
- 匿名访问返回 401 或 Spring Security 重定向行为按 API Accept JSON 调整；最终 React client 必须能识别未登录。

验收：

- `./mvnw -Dtest=AdminSessionApiControllerTests test` 通过。

```

## Prompt 4.3：为 `/login` 和 `/logout` 增加 JSON success/failure handler

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：在不破坏 Thymeleaf 登录页的前提下，让 React SPA 可以通过 JSON 完成登录和登出。

修改：
- `AdminSecurityConfiguration`
- 如需要，新增 `JsonAwareAuthenticationSuccessHandler`
- 如需要，新增 `JsonAwareAuthenticationFailureHandler`
- 如需要，新增 `JsonAwareLogoutSuccessHandler`

行为：
1. POST `/login`：
   - 如果请求 `Accept: application/json` 或 `X-Requested-With: XMLHttpRequest`：成功返回 `ApiResult.ok(sessionInfo)`，失败返回 401 JSON。
   - 否则保留原行为：成功 redirect `/admin`，失败 redirect `/login?error`。
2. POST `/logout`：
   - JSON 请求返回 `ApiResult.ok`。
   - 普通请求仍 redirect `/login?logout`。
3. 继续要求 CSRF。
4. 不新增弱化安全的免 CSRF 登录接口。

测试：
- JSON login 成功返回 200 JSON。
- JSON login 失败返回 401 JSON。
- 普通表单登录行为保持原状。
- JSON logout 成功后 session 失效。

验收：
- `./mvnw -Dtest=AdminJsonLoginTests,AdminSecurityTests test` 通过。
```

---

# 阶段 5：补齐管理端 JSON API

## 阶段目标

把当前 Thymeleaf 依赖的所有 view/service 能力暴露成安全 JSON API，为 React 页面迁移提供数据源。

## Prompt 5.1：新增 overview/config/policies/health JSON API

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：为总览、配置、危险策略、健康页新增只读 JSON API。

新增：
- `src/main/java/com/refinex/dbflow/admin/controller/AdminOverviewApiController.java`
- `src/main/java/com/refinex/dbflow/admin/controller/AdminOperationsApiController.java`

接口：
- `GET /admin/api/overview` -> `AdminOverviewViewService.overview()`
- `GET /admin/api/config` -> `AdminOperationsViewService.configPage()`
- `GET /admin/api/policies/dangerous` -> `AdminOperationsViewService.dangerousPolicyPage()`
- `GET /admin/api/health` -> `AdminOperationsViewService.healthPage()`

要求：
1. 返回统一 `ApiResult.ok(data)`。
2. 不改变现有 Thymeleaf Controller。
3. 不暴露完整 JDBC URL、password、Token、hash。
4. 只允许管理员访问。

测试：
- 新增 `AdminOperationsApiControllerTests`。
- 分别断言四个 API 返回 JSON，且不包含敏感字段。

验收：
- `./mvnw -Dtest=AdminOperationsApiControllerTests test` 通过。
```

## Prompt 5.2：新增 users JSON API

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：把用户管理页面能力暴露为 JSON API。

新增：
- `src/main/java/com/refinex/dbflow/admin/controller/AdminUserApiController.java`
- 如需要，新增 request DTO：`AdminCreateUserRequest`, `AdminResetPasswordRequest`

接口：
- `GET /admin/api/users?username=&status=`
- `POST /admin/api/users`
- `POST /admin/api/users/{userId}/disable`
- `POST /admin/api/users/{userId}/enable`
- `POST /admin/api/users/{userId}/reset-password`

复用：
- `AdminAccessManagementService.listUsers`
- `createUser`
- `disableUser`
- `enableUser`
- `resetPassword`

要求：
1. 错误统一转换为 `ApiResult` 或合适 HTTP 4xx JSON。
2. 列表不返回 passwordHash。
3. reset password 不返回密码。
4. mutation 必须 CSRF。

测试：
- `AdminUserApiControllerTests`
- 覆盖 list/filter/create/disable/enable/reset。
- 断言匿名/非 admin 禁止访问。

验收：
- `./mvnw -Dtest=AdminUserApiControllerTests test` 通过。
```

## Prompt 5.3：新增 grants JSON API

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：把项目环境授权页面能力暴露为 JSON API。

新增：
- `src/main/java/com/refinex/dbflow/admin/controller/AdminGrantApiController.java`
- 如需要，新增 request DTO：`GrantEnvironmentRequest`, `UpdateProjectGrantsRequest`

接口：
- `GET /admin/api/grants?username=&projectKey=&environmentKey=&status=`
- `GET /admin/api/grants/options`
- `POST /admin/api/grants`
- `POST /admin/api/grants/update-project`
- `POST /admin/api/grants/{grantId}/revoke`

复用：
- `AdminAccessManagementService.listGrantGroups`
- `listActiveUserOptions`
- `listEnvironmentOptions`
- `grantEnvironment`
- `updateUserProjectGrants`
- `revokeGrant`

要求：
1. `options` 返回 active users 与 environment options。
2. `update-project` 支持空 environmentKeys，表示撤销该用户在该项目下所有环境授权。
3. 不返回数据库连接信息。

测试：
- `AdminGrantApiControllerTests`
- 覆盖 options、list、create、update project grants、revoke。

验收：
- `./mvnw -Dtest=AdminGrantApiControllerTests test` 通过。
```

## Prompt 5.4：新增 tokens JSON API

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：把 Token 管理页面能力暴露为 JSON API，同时严格保持 Token 明文一次性展示边界。

新增：
- `src/main/java/com/refinex/dbflow/admin/controller/AdminTokenApiController.java`
- request DTO：`IssueTokenRequest`, `ReissueTokenRequest`

接口：
- `GET /admin/api/tokens?username=&status=`
- `POST /admin/api/tokens`
- `POST /admin/api/tokens/{tokenId}/revoke`
- `POST /admin/api/users/{userId}/tokens/reissue`

复用：
- `AdminAccessManagementService.listTokens`
- `issueToken`
- `revokeToken`
- `reissueToken`
- `listActiveUserOptions` 可在 `/admin/api/grants/options` 复用，也可单独提供 `/admin/api/tokens/options`

安全要求：
1. GET 列表只返回 tokenPrefix、status、expiresAt、lastUsedAt，不返回 plaintextToken、tokenHash。
2. POST issue/reissue 成功响应可以返回 plaintextToken，但只能在本次响应出现。
3. revoke 不返回明文。
4. 测试必须 assert 列表与审计中不出现明文。

测试：
- `AdminTokenApiControllerTests`
- 覆盖 issue、list redaction、reissue、revoke。

验收：
- `./mvnw -Dtest=AdminTokenApiControllerTests test` 通过。
```

---

# 阶段 6：前端 API Client、Session、路由保护、布局导航

## 阶段目标

建立 React 前端的 API 调用层、统一错误处理、CSRF、登录状态、受保护路由和 DBFlow Shell。

## Prompt 6.1：实现前端 API client 与 CSRF 支持

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：在 `dbflow-admin` 中实现统一 API client，支持 Spring Security CSRF、ApiResult 解包和错误处理。

新增：
- `dbflow-admin/src/api/client.ts`
- `dbflow-admin/src/api/csrf.ts`
- `dbflow-admin/src/types/api.ts`
- `dbflow-admin/src/lib/errors.ts`

要求：
1. 使用 axios。
2. baseURL 使用相对路径，不能写死 `localhost:8080`。
3. request interceptor：对非 GET 请求读取 `XSRF-TOKEN` cookie，并设置 `X-XSRF-TOKEN` header。
4. response interceptor：识别 `ApiResult`，如果 success false 抛出带 errorCode/message 的错误。
5. 401 时不要直接全局跳死；由 session/router 层处理。
6. 提供泛型方法：`apiGet<T>`, `apiPost<T>`, `apiDelete<T>`。

验收：
- `pnpm --dir dbflow-admin build` 通过。
```

## Prompt 6.2：实现 session API、store 与路由保护

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：React SPA 通过 `/admin/api/session` 判断登录状态，并保护 authenticated routes。

新增：
- `dbflow-admin/src/api/session.ts`
- `dbflow-admin/src/types/session.ts`
- `dbflow-admin/src/stores/session-store.ts`
- 如需要，新增 `dbflow-admin/src/components/auth/require-auth.tsx`

修改：
- TanStack Router authenticated route loader 或 beforeLoad。
- 未登录时跳转 `/login?redirect=<current>`。
- 已登录访问 `/login` 时跳转 `/`。

要求：
1. session 数据包含 username、displayName、roles、shell。
2. 受保护页面先显示 loading skeleton，不闪烁错误页面。
3. 401 时清空 session store。
4. 不使用 localStorage 存放登录态；session 以服务端 cookie 为准。

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 手动运行时未登录访问 `/admin-next/users` 会跳到 `/admin-next/login`。
```

## Prompt 6.3：实现 React 登录页

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：用 React + shadcn/ui 重写 DBFlow 登录页，替代模板 sign-in demo。

新增或修改：
- `dbflow-admin/src/routes/(auth)/login.tsx` 或当前路由规范下的 `/login`
- `dbflow-admin/src/features/auth/login-page.tsx`
- `dbflow-admin/src/api/session.ts` 增加 `login(username, password)` 和 `logout()`

视觉要求：
1. 左侧或背景体现 `MCP SQL Gateway`、`Database Operation Governance`、`Audit Ready`。
2. 使用 shadcn Card/Input/Button/Form。
3. 使用 Lucide：`Database`, `Shield`, `KeyRound`。
4. 可少量使用 subtle gradient，不要引入 Aceternity/Magic UI 作为核心依赖。
5. 支持密码显隐、错误 toast、加载状态。
6. 成功后按 redirect 参数跳转，否则跳转 `/`。

协议：
- POST `/login`，`Content-Type: application/x-www-form-urlencoded` 或后端支持的 JSON login 格式。
- Accept 使用 `application/json`。
- 携带 CSRF header。

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 登录失败能显示错误。
- 登录成功后能访问 Dashboard。
```

## Prompt 6.4：改造 Layout、Header、ProfileDropdown、Command Search

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：让 shadcn-admin 的 Layout 真正显示 DBFlow session shell 和 DBFlow 导航。

修改：
- `dbflow-admin/src/components/layout/app-sidebar.tsx`
- `dbflow-admin/src/components/layout/data/sidebar-data.ts`
- `dbflow-admin/src/components/profile-dropdown.tsx`
- `dbflow-admin/src/components/search.tsx`
- 需要时新增 `dbflow-admin/src/lib/routes.ts`

要求：
1. Sidebar user 从 session store 读取，不再使用静态 satnaing 用户。
2. Header 显示 MCP 状态 badge、配置来源、ThemeSwitch、ProfileDropdown。
3. ProfileDropdown 支持登出，调用 `/logout` JSON。
4. Command Search 中列出 DBFlow 页面：总览、用户、授权、Token、配置、策略、审计、健康。
5. 保留暗色/亮色主题。
6. 删除 TeamSwitcher 中无意义 demo team，或改成 DBFlow brand selector。

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 不再出现 `satnaing`、`Acme`、`Shadcn Admin` 可见文本，NOTICE/README 除外。
```

---

# 阶段 7：Dashboard 与 DBFlow 组件体系

## 阶段目标

先建立设计系统里的 DBFlow 业务组件，再迁移总览页。

## Prompt 7.1：实现 DBFlow Badge、Card、Copy、EmptyState 基础组件

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：建立 DBFlow 管理端通用业务组件，不直接把状态样式散落在页面里。

新增：
- `dbflow-admin/src/components/dbflow/risk-badge.tsx`
- `dbflow-admin/src/components/dbflow/decision-badge.tsx`
- `dbflow-admin/src/components/dbflow/status-badge.tsx`
- `dbflow-admin/src/components/dbflow/env-badge.tsx`
- `dbflow-admin/src/components/dbflow/metric-card.tsx`
- `dbflow-admin/src/components/dbflow/page-header.tsx`
- `dbflow-admin/src/components/dbflow/copy-button.tsx`
- `dbflow-admin/src/components/dbflow/empty-state.tsx`
- `dbflow-admin/src/lib/badges.ts`
- `dbflow-admin/src/lib/format.ts`

要求：
1. risk: LOW/MEDIUM/HIGH/CRITICAL 有稳定视觉映射。
2. decision: EXECUTED/POLICY_DENIED/REQUIRES_CONFIRMATION/FAILED 有稳定视觉映射。
3. status: ACTIVE/DISABLED/REVOKED/EXPIRED/HEALTHY/UNHEALTHY 有稳定视觉映射。
4. 组件使用 shadcn Badge/Card/Button，不自造复杂样式。
5. CopyButton 使用 navigator.clipboard + sonner toast。

验收：
- `pnpm --dir dbflow-admin build` 通过。
```

## Prompt 7.2：实现 overview API 与 Dashboard 页面

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：迁移 `/admin` 总览为 React Dashboard。

新增：
- `dbflow-admin/src/api/overview.ts`
- `dbflow-admin/src/types/overview.ts`
- `dbflow-admin/src/features/dashboard/index.tsx`
- `dbflow-admin/src/features/dashboard/components/*`

页面内容：
1. PageHeader：标题 `总览`，描述使用后端 `windowLabel`。
2. 指标卡：渲染 `overview.metrics`。
3. 最近审计事件表：时间、用户、project/env、risk、decision、sqlHash、详情按钮。
4. 需要关注事项：渲染 `overview.attentionItems`。
5. 环境选择器：优先保留 UI，后续可接过滤；不能做假功能时需显示为 disabled 或仅文案说明。
6. 空状态与加载骨架。

数据：
- GET `/admin/api/overview`
- TanStack Query query key: `['overview']`

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 后端运行时访问 `/admin-next` 能看到真实 overview 数据。
```

---

# 阶段 8：用户、授权、Token 页面

## 阶段目标

迁移身份与访问管理三大核心页面，并替换 Thymeleaf 的表单 POST/flash 逻辑。

## Prompt 8.1：实现 Users 页面

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：实现 React `/users` 用户管理页面。

新增：
- `dbflow-admin/src/api/users.ts`
- `dbflow-admin/src/types/access.ts`
- `dbflow-admin/src/features/users/index.tsx`
- `dbflow-admin/src/features/users/components/users-table.tsx`
- `dbflow-admin/src/features/users/components/create-user-sheet.tsx`
- `dbflow-admin/src/features/users/components/reset-password-dialog.tsx`
- `dbflow-admin/src/features/users/components/user-actions.tsx`

功能：
1. 列表：id、username、displayName、role、status、grantCount、activeTokenCount、actions。
2. 筛选：username、status，并同步到 URL search。
3. 新建用户 Sheet：username、displayName、password 可选。
4. 禁用/启用操作使用 AlertDialog 二次确认。
5. 重置密码 Dialog。
6. 操作成功后 invalidate users query 并 toast。
7. 错误显示后端 message。

API：
- GET `/admin/api/users`
- POST `/admin/api/users`
- POST `/admin/api/users/{id}/disable`
- POST `/admin/api/users/{id}/enable`
- POST `/admin/api/users/{id}/reset-password`

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 用户列表不展示 passwordHash。
```

## Prompt 8.2：实现 Grants 页面

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：实现 React `/grants` 项目环境授权页面。

新增：
- `dbflow-admin/src/api/grants.ts`
- `dbflow-admin/src/features/grants/index.tsx`
- `dbflow-admin/src/features/grants/components/grants-table.tsx`
- `dbflow-admin/src/features/grants/components/create-grant-sheet.tsx`
- `dbflow-admin/src/features/grants/components/edit-project-grants-sheet.tsx`
- `dbflow-admin/src/features/grants/components/grant-filter-bar.tsx`

功能：
1. 列表按用户×项目展示：username、projectKey、environment badges、actions。
2. 筛选：username、projectKey、environmentKey、status。
3. 新建授权：选择用户、项目、授权类型、多个环境 checkbox。
4. 编辑授权：对某用户某项目勾选/取消环境，保存调用 update-project。
5. `environmentOptions` 按 projectKey 分组。
6. 空环境时给出配置提示，而不是报错。

API：
- GET `/admin/api/grants`
- GET `/admin/api/grants/options`
- POST `/admin/api/grants/update-project`
- POST `/admin/api/grants/{grantId}/revoke`

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 不展示 JDBC、password、Token。
```

## Prompt 8.3：实现 Tokens 页面与一次性 Token 明文弹窗

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：实现 React `/tokens` Token 管理页面，尤其要正确处理一次性 Token 明文展示。

新增：
- `dbflow-admin/src/api/tokens.ts`
- `dbflow-admin/src/types/token.ts`
- `dbflow-admin/src/features/tokens/index.tsx`
- `dbflow-admin/src/features/tokens/components/tokens-table.tsx`
- `dbflow-admin/src/features/tokens/components/issue-token-sheet.tsx`
- `dbflow-admin/src/features/tokens/components/token-reveal-dialog.tsx`
- `dbflow-admin/src/features/tokens/components/token-actions.tsx`

功能：
1. 列表：id、username、tokenPrefix、status、expiresAt、lastUsedAt、actions。
2. 筛选：username、status。
3. 颁发 Token：选择 active user、expiresInDays。
4. 重新颁发 Token：从行操作触发，支持 expiresInDays 默认 30。
5. 吊销 Token：危险操作二次确认。
6. issue/reissue 成功后立即打开 TokenRevealDialog。
7. TokenRevealDialog 中显示 plaintextToken、prefix、tokenId、username，带复制按钮。
8. Dialog 关闭后立即清空本地 plaintextToken state，不能持久化到 localStorage/sessionStorage/Zustand persist。
9. tokens list 永远不能显示 plaintextToken。

API：
- GET `/admin/api/tokens`
- POST `/admin/api/tokens`
- POST `/admin/api/users/{userId}/tokens/reissue`
- POST `/admin/api/tokens/{tokenId}/revoke`

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 搜索源码，不允许把 plaintextToken 存入任何持久化 storage。
```

---

# 阶段 9：配置、危险策略、健康页面

## 阶段目标

迁移当前只读运维页面，保持脱敏、只读、安全边界。

## Prompt 9.1：实现 Config 页面

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：实现 React `/config` 配置查看页面。

新增：
- `dbflow-admin/src/api/config.ts`
- `dbflow-admin/src/types/config.ts`
- `dbflow-admin/src/features/config/index.tsx`
- `dbflow-admin/src/features/config/components/config-table.tsx`
- `dbflow-admin/src/features/config/components/config-detail-sheet.tsx`

功能：
1. 显示 sourceLabel。
2. 表格字段：project、projectName、env、envName、datasource、type、host、port、schema、username、limits、syncStatus。
3. 支持刷新按钮。
4. 点击行打开详情 Sheet，但仍只能展示脱敏字段。
5. 空状态：当前未配置 dbflow.projects。

安全：
- 页面和 types 中不要定义 password 字段。
- 不展示完整 jdbcUrl。

验收：
- `pnpm --dir dbflow-admin build` 通过。
```

## Prompt 9.2：实现 Dangerous Policies 页面

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：实现 React `/policies/dangerous` 危险策略只读页面。

新增：
- `dbflow-admin/src/api/policies.ts`
- `dbflow-admin/src/types/policy.ts`
- `dbflow-admin/src/features/policies/dangerous/index.tsx`
- `dbflow-admin/src/features/policies/dangerous/components/policy-defaults-table.tsx`
- `dbflow-admin/src/features/policies/dangerous/components/drop-whitelist-table.tsx`
- `dbflow-admin/src/features/policies/dangerous/components/policy-rules.tsx`
- `dbflow-admin/src/features/policies/dangerous/components/policy-reason-sheet.tsx`

功能：
1. 默认高危策略：operation、risk、decision、requirement。
2. DROP 白名单：operation、risk、project、env、schema、table、allowProd、prodRule。
3. 固定强化规则：DROP 白名单、TRUNCATE confirmation、prod 强化。
4. `查看被拒绝审计` 按钮跳转 `/audit?decision=POLICY_DENIED`。
5. 空白名单时显示明确说明。

验收：
- `pnpm --dir dbflow-admin build` 通过。
```

## Prompt 9.3：实现 Health 页面

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：实现 React `/health` 系统健康页面。

新增：
- `dbflow-admin/src/api/health.ts`
- `dbflow-admin/src/types/health.ts`
- `dbflow-admin/src/features/health/index.tsx`
- `dbflow-admin/src/features/health/components/health-card.tsx`

功能：
1. 顶部显示 overall 与 unhealthyCount/totalCount。
2. 渲染 metadata database、target datasource registry、Nacos、MCP endpoint 等健康项。
3. 每个健康项显示 name、component、status、description、detail、tone。
4. 提供刷新按钮，invalidate health query。
5. 不能显示 JDBC password、完整 JDBC URL、Token。

验收：
- `pnpm --dir dbflow-admin build` 通过。
```

---

# 阶段 10：审计中心

## 阶段目标

迁移审计列表和审计详情，充分利用已有 `/admin/api/audit-events`。

## Prompt 10.1：实现 Audit List 页面

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：实现 React `/audit` 审计列表页面，支持服务端分页和筛选。

新增：
- `dbflow-admin/src/api/audit.ts`
- `dbflow-admin/src/types/audit.ts`
- `dbflow-admin/src/features/audit/list/index.tsx`
- `dbflow-admin/src/features/audit/list/components/audit-table.tsx`
- `dbflow-admin/src/features/audit/list/components/audit-filter-sheet.tsx`
- `dbflow-admin/src/features/audit/list/components/audit-filter-chips.tsx`

功能：
1. 查询参数同步 URL：from、to、userId、project、env、risk、decision、sqlHash、tool、page、size、sort、direction。
2. 请求 GET `/admin/api/audit-events`。
3. 表格字段：createdAt、user、project/env、tool、operation、risk、decision、sqlHash、summary、详情。
4. 支持 page/size 的服务端分页。
5. 筛选 Sheet 提供所有现有 Thymeleaf 筛选项。
6. 点击详情跳转 `/audit/:eventId`。
7. SQL Hash 支持复制。

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 与当前 Thymeleaf `/admin/audit` 的筛选能力一致或更强。
```

## Prompt 10.2：实现 Audit Detail 页面与 SQL Viewer

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：实现 React `/audit/:eventId` 审计详情页面。

新增：
- `dbflow-admin/src/features/audit/detail/index.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-identity-panel.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-sql-panel.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-failure-panel.tsx`
- `dbflow-admin/src/features/audit/detail/components/audit-timeline.tsx`
- `dbflow-admin/src/components/dbflow/sql-code-viewer.tsx`

功能：
1. 请求 GET `/admin/api/audit-events/{id}`。
2. 顶部展示 Audit #id、decision badge、返回列表、复制 SQL Hash。
3. 身份面板展示请求时间、用户、project/env、tool、operation、risk、sqlHash、requestId、client、sourceIp、affectedRows、confirmationId。
4. SQL 文本用 Monaco readonly 或轻量 code viewer 展示；如果引入 Monaco 导致 bundle 配置复杂，可先用 shadcn card + `<pre>`，再单独任务升级 Monaco。
5. 拒绝/失败原因展示 status、errorCode、failureReason、resultSummary。
6. 时间线还原 request received、authorization、classification、policy decision、audit persisted。
7. 不展示 Token 明文、Token hash、数据库密码、完整 JDBC URL。

验收：
- `pnpm --dir dbflow-admin build` 通过。
```

## Prompt 10.3：升级 SQL Viewer 到 Monaco readonly

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：将审计详情 SQL Viewer 从普通 `<pre>` 升级为 Monaco Editor readonly。

新增依赖：
- `@monaco-editor/react` 或按 Vite 推荐方式接入 `monaco-editor`

修改：
- `dbflow-admin/src/components/dbflow/sql-code-viewer.tsx`

要求：
1. readonly。
2. language 优先使用 `sql`。
3. 高度自适应，默认 220px，长 SQL 可滚动。
4. 跟随 dark/light theme。
5. 不允许编辑和提交 SQL。
6. 如果 Monaco 构建需要 worker 配置，正确修改 Vite 配置。

验收：
- `pnpm --dir dbflow-admin build` 通过。
- 审计详情页 SQL 展示正常。
```

---

# 阶段 11：质量、文档、打包与部署

## 阶段目标

把前后端验证、构建、文档和部署流程固化，达到可交付状态。

## Prompt 11.1：新增前端测试基础

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：为 React admin 增加基础测试，覆盖关键组件和页面状态。

新增：
- `dbflow-admin/src/test/setup.ts`
- `dbflow-admin/src/components/dbflow/*.test.tsx`
- `dbflow-admin/src/features/auth/login-page.test.tsx`
- `dbflow-admin/src/features/tokens/components/token-reveal-dialog.test.tsx`
- 必要的 test utils

要求：
1. 使用 Vitest。
2. 能 mock API client。
3. 测试 RiskBadge、DecisionBadge、TokenRevealDialog 清空/复制行为、Login 错误状态。
4. 不要求完整 E2E。

验收：
- `pnpm --dir dbflow-admin test` 通过，若模板 browser test 依赖 Playwright，按项目实际情况配置 headless 或改为 jsdom。
- `pnpm --dir dbflow-admin build` 通过。
```

## Prompt 11.2：新增端到端 smoke 脚本或手册

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：补充新 React 管理端的开发和部署验证文档。

新增或修改：
- `docs/user-guide/admin-guide.md`
- `docs/deployment/README.md`
- `docs/OBSERVABILITY.md` 如需要
- `README.md` 的管理端入口说明

内容：
1. 开发启动：后端 `./mvnw spring-boot:run`，前端 `pnpm --dir dbflow-admin dev`。
2. React 新后台试运行入口：`/admin-next`。
3. 打包命令：`./mvnw -Preact-admin -DskipTests package`。
4. 登录、CSRF、session 说明。
5. Token 明文一次性展示安全提醒。
6. `/admin` 和 `/admin-next` cutover 状态说明。

验收：
- 文档中不写真实密码、Token、Nacos 密码、数据库密码。
- `python3 scripts/check_harness.py` 通过。
```

## Prompt 11.3：全量验证与修复

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：对 React admin 集成状态做一次完整验证并修复失败项。

运行：
```bash
pnpm --dir dbflow-admin install --frozen-lockfile
pnpm --dir dbflow-admin lint
pnpm --dir dbflow-admin build
./mvnw test
./mvnw -Preact-admin -DskipTests package
python3 scripts/check_harness.py
git diff --check
```

要求：

1. 如果 lint 因模板旧规则或现有代码风格失败，优先修代码，不要粗暴关闭规则。
2. 如果 `./mvnw test` 因 Docker/Testcontainers skip，这是已有行为，不视为失败；真实 failure 必须修复。
3. 确认 jar 中包含 `/static/admin-next/index.html` 与 assets。
4. 确认当前 `/admin` Thymeleaf 仍可访问。

验收：

- 所有命令通过或记录明确可接受 skip。

```

---

# 阶段 12：Cutover：`/admin` 从 Thymeleaf 切到 React

## 阶段目标

在 `/admin-next` 验收完成后，把正式后台入口从 Thymeleaf 切换到 React SPA，并处理旧 Thymeleaf 页面归档或删除。

## Prompt 12.1：将 React base 从 `/admin-next` 切换到 `/admin`

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

前提：`/admin-next` 已经完成验收，所有核心页面均可用。

目标：把 React SPA 正式切到 `/admin`。

修改：
1. `dbflow-admin/vite.config.ts`：base 从 `/admin-next/` 改为 `/admin/`。
2. Maven react-admin profile：构建产物输出到 `/static/admin/` 或 `target/classes/static/admin/`。
3. `AdminSpaController`：fallback 从 `/admin-next/**` 改为 `/admin/**`，但不要截获 `/admin/api/**`。
4. `AdminHomeController` 中 Thymeleaf 页面路由改到 `/admin-legacy/**`，或如果决定完全删除，则删除对应 Thymeleaf routes。
5. 登录成功默认跳转从 `/admin` 保持不变，此时应进入 React SPA。
6. `/admin/api/**` 保持 JSON API，不被 SPA fallback 捕获。
7. 静态资源 `/admin/assets/**` permitAll。

测试：
- `/admin` -> React index。
- `/admin/users` -> React index。
- `/admin/api/session` -> JSON API。
- `/admin-legacy` 如保留，仍能访问旧页面。

验收：
- `./mvnw -Dtest=AdminSpaControllerTests,AdminSecurityTests test` 通过。
- `pnpm --dir dbflow-admin build` 通过。
- `./mvnw -Preact-admin -DskipTests package` 通过。
```

## Prompt 12.2：归档或删除 Thymeleaf 管理端

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

前提：React `/admin` 已完成 cutover。

目标：处理旧 Thymeleaf 管理端，避免双后台长期并存造成维护混乱。

二选一执行，优先方案 A：

方案 A：保留短期 legacy
1. 把旧 Thymeleaf routes 全部移到 `/admin-legacy/**`。
2. 旧模板目录保留 `src/main/resources/templates/admin/**`。
3. README 标注 `/admin-legacy` 仅用于过渡期排障。
4. Security 中 `/admin-legacy/**` 仍需 ROLE_ADMIN。

方案 B：完全删除旧后台(推荐)
1. 删除 `src/main/resources/templates/admin/**`。
2. 删除 `src/main/resources/static/admin-assets/**` 中仅 Thymeleaf 使用的 CSS/JS。
3. 删除 `AdminHomeController` 中 SSR page methods，只保留 API controllers。
4. 删除或重写相关 Thymeleaf Controller tests。

推荐先执行方案 A，稳定一个版本后再执行方案 B。

验收：
- 不存在 `/admin` 同时由 Thymeleaf 和 React 竞争的情况。
- API、MCP、Actuator 不受影响。
- `./mvnw test` 通过。
```

## Prompt 12.3：最终文档与 README 更新

```text
你是 Codex，在 Refinex-DBFlow 仓库根目录执行本任务。

目标：React 管理端 cutover 后，更新所有用户可见文档。

修改：
- `README.md`
- `docs/user-guide/admin-guide.md`
- `docs/deployment/README.md`
- `docs/ARCHITECTURE.md`
- `docs/OBSERVABILITY.md` 如涉及验证命令
- `docs/PLANS.md`

内容：
1. 管理端说明从 Thymeleaf 更新为 React SPA。
2. 后台入口仍是 `/login` 和 `/admin`。
3. 增加 `dbflow-admin/` 目录说明。
4. 增加前端技术栈说明：React、Vite、TypeScript、shadcn/ui、Tailwind、TanStack。
5. 增加构建命令与 profile：`./mvnw -Preact-admin -DskipTests package`。
6. 说明旧 `/admin-legacy` 的保留/删除状态。
7. 保持安全边界说明：MCP 不使用管理端 session，仍要求 Bearer Token。

验收：
- 文档没有过时地称“项目当前不引入独立前端 SPA”。
- `python3 scripts/check_harness.py` 通过。
```

---

## 9. 后端 API DTO 建议

为避免 React 直接依赖 Thymeleaf view model 命名，建议后续逐步增加稳定 DTO：

```text
admin/dto/session/AdminSessionResponse.java
admin/dto/overview/AdminOverviewResponse.java
admin/dto/access/AdminUserResponse.java
admin/dto/access/AdminTokenResponse.java
admin/dto/access/AdminGrantGroupResponse.java
admin/dto/operations/AdminConfigResponse.java
admin/dto/operations/AdminDangerousPolicyResponse.java
admin/dto/operations/AdminHealthResponse.java
```

MVP 可以先复用现有 view records，后续再重构为 DTO。优先级：Token、Config、Audit 的 DTO 最重要，因为它们有敏感字段边界。

---

## 10. 安全验收清单

每个阶段都要检查：

- `/mcp` 不得因为管理端 React 改造而接受 session cookie 鉴权。
- `/admin/api/**` 必须要求 `ROLE_ADMIN`。
- mutation 必须要求 CSRF。
- GET Token 列表不返回 plaintextToken/tokenHash。
- Token 明文只在 issue/reissue mutation 成功响应中出现一次。
- React 不把 Token 明文写入 localStorage、sessionStorage、URL、Zustand persist、日志。
- Config API 和页面不返回完整 JDBC URL，不返回 password query param，不返回数据库密码。
- Audit API 和页面不返回 Token 明文、Token hash、完整结果集、数据库密码。
- 错误消息经过服务端脱敏，不把异常中的连接串原样返回。
- `/admin` cutover 时不能让 `/admin/api/**` 被 SPA fallback 吞掉。

---

## 11. 最终完成标准

React 管理端达到可替代当前 Thymeleaf 后台的标准：

- 登录、登出、session 过期处理正常。
- 总览、用户、授权、Token、配置、策略、审计、健康所有页面可用。
- 所有 mutation 有成功 toast、失败 toast、加载态、危险确认。
- Token 明文一次性展示边界正确。
- 表格筛选和分页至少与旧后台一致。
- `/admin-next` 试运行通过后，`/admin` cutover 完成。
- 旧 Thymeleaf 页面归档到 `/admin-legacy` 或删除。
- `pnpm --dir dbflow-admin build` 通过。
- `./mvnw test` 通过。
- `./mvnw -Preact-admin -DskipTests package` 通过。
- `python3 scripts/check_harness.py` 通过。
- README、admin-guide、deployment、architecture 已更新。

---

## 12. 不做事项

本轮 React 管理端迁移不做：

- 不引入 Next.js。
- 不引入 Ant Design Pro。
- 不引入 Vue。
- 不做公网部署能力。
- 不改 MCP 协议。
- 不改 SQL 执行安全策略。
- 不把 Token 明文、数据库密码、Nacos 密码写入任何前端配置。
- 不做低代码后台。
- 不做多租户 SaaS 控制台。
- 不做拖拽式策略编辑器，危险策略本轮仍只读展示。

---

## 13. 推荐执行节奏

```text
第 1 天：阶段 1-3，完成前端工程导入、裁剪、/admin-next 静态集成
第 2 天：阶段 4-5，完成 session/CSRF/JSON API
第 3 天：阶段 6-7，完成前端 shell、登录、dashboard、组件体系
第 4 天：阶段 8，完成 users/grants/tokens
第 5 天：阶段 9-10，完成 config/policies/health/audit
第 6 天：阶段 11，测试、文档、打包
第 7 天：阶段 12，cutover 或保留 /admin-next 试运行
```

不要为了追求一次性切换而跳过 `/admin-next`。当前后台已经能用，最稳妥的路径是先并行试运行，再替换正式入口。
