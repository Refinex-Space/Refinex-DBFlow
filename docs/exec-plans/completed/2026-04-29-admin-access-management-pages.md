# 2026-04-29 Admin Access Management Pages

## Sprint Contract

### Objective

将管理端用户、MCP Token、项目环境授权页面从演示数据升级为可操作的 Spring MVC + Thymeleaf 后端闭环，覆盖创建用户、禁用用户、颁发/吊销/重新颁发
Token、授权/撤销项目环境。

### Scope

- 新增管理端访问管理 service，读取和写入现有 `dbf_users`、`dbf_api_tokens`、`dbf_projects`、`dbf_environments`、
  `dbf_user_env_grants`。
- 将 `/admin/users`、`/admin/tokens`、`/admin/grants` 改为真实查询和 POST 操作。
- 保留既有后台布局、表格密度、筛选栏、状态文案和基础交互。
- Token 颁发成功后通过一次性页面提示展示明文，列表只展示前缀和状态。
- 增加 controller/service/security smoke 测试，覆盖创建用户、禁用、授权、撤销、颁发、吊销、重新颁发，以及权限/CSRF 边界。

### Non-Scope

- 不新增用户角色 schema；当前管理端仍沿用现有 active 用户 + password hash 的登录模型。
- 不实现完整分页、排序、审计写入或生产级 flash 消息系统。
- 不增加 Node、Vite、SPA 或独立前端构建链。
- 不修改 MCP Bearer Token 校验协议。

### Constraints

- Token 明文只能在颁发或重新颁发成功后的单次响应中出现。
- 用户/Token/授权列表不能展示数据库密码、JDBC URL 密码、Token hash 或完整 Token 明文。
- 所有 POST 表单必须走 Spring Security CSRF。
- 管理页面必须保持 `ROLE_ADMIN` 访问控制。

### Assumptions

- 项目环境来源仍由 `ProjectEnvironmentCatalogService.syncConfiguredProjectEnvironments()` 将外部配置同步到元数据库。
- 当前实体没有角色字段，本阶段列表用固定管理端角色标签展示，不扩 schema。
- Token 表当前没有 client/source_ip 字段，本阶段列表只展示当前元数据内可证明的安全字段。

### Acceptance Criteria

- `./mvnw test` 通过。
- 管理端 smoke 覆盖创建用户、授权环境、颁发和吊销 Token。
- 重新颁发会先吊销当前 active Token，再展示新 Token 明文一次。
- 非管理员不能执行管理端写操作；缺少 CSRF 的 POST 被拒绝。
- 页面响应不包含 `token_hash`、`password_hash` 或数据库密码。

## Plan

- [x] Step 1: 增加管理端访问管理 service 与 DTO/form，覆盖用户、Token、授权操作。
- [x] Step 2: 改造 users/tokens/grants controller 和 Thymeleaf 表单。
- [x] Step 3: 增加 service/controller/security smoke 测试。
- [x] Step 4: 更新 ARCHITECTURE/OBSERVABILITY/PLANS 并执行验证。

## Evidence Log

- Preflight: `python3 scripts/check_harness.py` pass.
- Baseline: `./mvnw test` pass, 114 tests, 0 failures, 0 errors, 10 skipped.
- Context7: checked Spring Security 6.5 CSRF/form POST MockMvc guidance and Thymeleaf form binding/error rendering
  guidance.
- RED: `./mvnw -Dtest=AdminAccessManagementServiceTests,AdminAccessManagementControllerTests test` failed at test
  compile because `AdminAccessManagementService` did not exist.
- Targeted GREEN: `./mvnw -Dtest=AdminAccessManagementServiceTests,AdminAccessManagementControllerTests test` pass, 5
  tests, 0 failures, 0 errors.
- Admin UI regression:
  `./mvnw -Dtest=AdminAccessManagementServiceTests,AdminAccessManagementControllerTests,AdminUiControllerTests test`
  pass, 11 tests, 0 failures, 0 errors.
- Full verification: `./mvnw test` pass, 119 tests, 0 failures, 0 errors, 10 skipped.
- Harness verification: `python3 scripts/check_harness.py` pass.
- Whitespace verification: `git diff --check` pass.
- Final verification after plan archival: `./mvnw test` pass, 119 tests, 0 failures, 0 errors, 10 skipped.
