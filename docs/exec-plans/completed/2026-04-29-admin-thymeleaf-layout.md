# 2026-04-29 Admin Thymeleaf Layout

## Sprint Contract

### Objective

将 P09 后台管理 HTML 原型转化为 Spring MVC + Thymeleaf + 静态 CSS/JS 的管理端基础布局，让管理端在未登录时跳转自定义登录页，登录后可访问高密度后台
shell 和基础页面。

### Scope

- 增加 Thymeleaf 依赖和模板资源。
- 用自定义 `/login` 模板替换 Spring Security 默认登录页。
- 将 `/admin` 及基础管理页面映射到 Thymeleaf 模板。
- 提供共享后台布局、侧边导航、顶部状态、筛选栏、表格、详情、状态样例等原型对应组件。
- 保留 P09 原型的信息密度、布局比例、文案层级、状态色和安全脱敏语义。
- 增加控制器/安全/模板访问测试。

### Non-Scope

- 不实现用户、Token、授权、配置、策略、健康的真实 CRUD 后端。
- 不引入 Node、Vite、SPA 或前端构建链。
- 不改变 MCP Bearer Token 安全链路。
- 不把演示数据接入真实 SQL 执行、审计查询或配置服务。

### Constraints

- 管理端必须使用 Spring Security form login。
- 静态资源通过 Spring Boot 静态资源机制提供。
- 后台页面必须保持企业内网管理工具风格，避免营销页、hero、装饰性卡片堆叠。
- 敏感信息仍按原型约束脱敏，不能展示 Token 明文、数据库密码或完整连接串。

### Assumptions

- 本阶段是 P10 基础布局，不要求完成全部管理业务 API 的页面接入。
- `/admin-assets/**` 作为后台静态资源前缀，便于安全链 permitAll。
- 演示数据放在 controller model 中，后续业务页面可逐步替换为真实 service 数据。

### Acceptance Criteria

- `./mvnw test` 通过。
- `/admin` 未登录会跳转 `/login`。
- `/login` 返回自定义 Thymeleaf 登录页，并提交到 Spring Security form login。
- 登录后 `/admin` 和基础页面返回 HTML。
- 静态 CSS/JS 可被未登录请求访问。
- 基础页面覆盖总览、用户管理、项目环境授权、Token、配置查看、危险策略、审计列表、审计详情、系统健康。

## Plan

- [x] Step 1: 增加 Thymeleaf 依赖、管理端 MVC controller 和安全链调整。
- [x] Step 2: 从原型拆出 Thymeleaf 模板、共享 fragment、静态 CSS/JS。
- [x] Step 3: 增加 MockMvc 测试覆盖登录页、未登录跳转、登录后页面、静态资源。
- [x] Step 4: 更新 ARCHITECTURE/OBSERVABILITY/PLANS 并执行验证。

## Evidence Log

- Preflight: `python3 scripts/check_harness.py` pass.
- Baseline: `./mvnw test` pass, 108 tests, 0 failures, 0 errors, 10 skipped.
- Context7: checked Spring Security 6.5 form login custom page/static resource guidance and Thymeleaf 3.1
  fragment/static resource guidance.
- Targeted: `./mvnw -Dtest=AdminUiControllerTests,AdminSecurityTests test` pass, 11 tests, 0 failures, 0 errors.
- Full verification: `./mvnw test` pass, 114 tests, 0 failures, 0 errors, 10 skipped.
- Harness verification: `python3 scripts/check_harness.py` pass.
- Whitespace verification: `git diff --check` pass.
- Local runtime: `./mvnw spring-boot:run ... --server.port=18080` started successfully with local initial admin user.
- HTTP runtime: unauthenticated `GET /admin` returned 302 to `/login`; `GET /login` returned 200;
  `GET /admin-assets/js/admin.js` returned 200.
- Browser runtime: Playwright logged in through custom form login, reached `/admin`, navigated to `/admin/tokens` and
  `/admin/audit/A-20260429-145511`, opened the status drawer, and reported 0 console errors / 0 warnings.
