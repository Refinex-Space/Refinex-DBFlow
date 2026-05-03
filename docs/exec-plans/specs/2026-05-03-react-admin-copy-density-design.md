<!-- Harness brainstorm spec. Safe to edit before approval. -->

# React Admin 全局低文案密度设计

Date: 2026-05-03

Status: Approved

## 背景

DBFlow Admin 当前认证后页面已经具备核心数据面：侧边栏导航、顶部工具栏、搜索、主题切换、业务表格、状态卡片、筛选器、抽屉和详情页。问题不在功能链路，而在页面表达方式过于解释型：

- 每个页面左上角都有大标题和页面描述，和侧边栏导航、页面上下文重复。
- 总览、用户、授权、Token、配置、危险策略、审计、健康和设置页中存在大量说明性句子。
- 多数说明是在解释系统设计或后端策略，而不是帮助用户完成当前操作。
- 删除文案后如果不调整组件间距，会留下空白、弱边界和不均衡布局。

用户已明确要求采用更激进的清理方向：专业的内部系统不应把大量解释铺在页面上。

## 目标

- 将 React Admin 的认证后页面调整为低文案密度、数据优先、操作优先的内部控制台。
- 删除所有页面级“大标题 + 描述”，只保留左上角紧凑面包屑路径。
- 删除大部分说明型副标题、helper text、empty state description 和策略解释文案。
- 清理后同步压缩页面垂直间距、卡片内距和 section 布局，避免视觉空洞。
- 保留必要的可操作文本、安全文本和可访问性文本。
- 更新受影响测试，确保测试断言从“说明文案存在”转向“路径、数据、操作和状态存在”。

## 非目标

- 不修改后端 API、SQL 策略、审计语义、鉴权逻辑或 Nacos 配置。
- 不修改 API response schema；例如 overview 的 `windowLabel` 可以继续由后端返回，但 React 页面不再渲染该类说明文本。
- 不重做侧边栏、顶部栏、全局搜索、主题切换或登录页。
- 不新增 onboarding、帮助中心、文档弹窗或内嵌教程来替代被删除的说明。
- 不移除表格列名、表单 label、筛选器 label、按钮文字、状态 badge、错误消息和确认对话框中的必要文本。
- 不把本次工作扩展为视觉主题重设计；颜色、字体、组件库继续沿用现有 design system。

## 设计原则

### 1. 页面不是说明书

认证后页面只展示路径、数据、状态和操作。解释 DBFlow 架构、安全边界、后端策略、数据口径的长句默认删除。

### 2. 导航上下文由面包屑承担

页面左上角统一显示紧凑路径，例如：

- `工作台 / 总览`
- `身份与访问 / 用户管理`
- `身份与访问 / 项目授权`
- `身份与访问 / Token 管理`
- `配置与策略 / 配置查看`
- `配置与策略 / 危险策略`
- `审计 / 审计列表`
- `审计 / 审计详情`
- `运维 / 系统健康`
- `设置 / 外观设置`

面包屑路径替代原来的 `eyebrow`、`h1` 和 `description`。页面内容区不再额外重复当前页面标题。

### 3. 说明文案默认删除

以下类型默认删除：

- 页面描述：例如“最近 24 小时网关安全、执行和健康摘要。”
- section 描述：例如“按创建时间倒序展示最近 5 条审计记录”。
- 策略解释：例如“这些规则由服务端强制执行，React 页面只读展示。”
- 后续计划提示：例如“环境过滤将在后续阶段接入”。
- 空状态说明：例如“调整筛选条件，或等待新的 MCP SQL 请求进入 DBFlow 网关。”
- 英文模板文案：例如“Manage your account settings and set e-mail preferences.”

### 4. 必要文本必须保留

以下类型不属于冗余说明，必须保留或压缩后保留：

- 表单 label、placeholder、校验错误。
- 按钮、菜单项、tab、筛选器、列名、卡片指标名。
- 状态 badge：risk、decision、健康状态、启用状态、环境标识。
- 后端错误消息、用户操作失败提示。
- 危险操作确认对话框、Token 明文一次性展示提示、权限或安全边界相关的操作确认。
- `sr-only` 可访问性文本，例如图标按钮的 accessible name。

## 采用方案

### 方案：全局 PageHeader 收敛为 Breadcrumb Strip

新增或改造一个全局页面路径组件，用路由元数据渲染紧凑面包屑。所有认证后页面从 `PageHeader` 迁移到该组件。

组件职责：

- 从调用方接收 `items` 或接收当前页面 route key 后生成路径。
- 左侧渲染一行紧凑路径，字号小于原 `h1`，不使用 hero 级排版。
- 右侧保留原 `PageHeader.actions` 的操作区，例如新增用户、刷新、过滤入口。
- 底部只保留轻分隔或局部留白，不再使用大面积 header 区。

路由来源：

- 优先复用 `dbflow-admin/src/lib/routes.ts` 的 `dbflowRouteGroups`。
- 对非侧边栏路由补充显式映射，例如审计详情页显示 `审计 / 审计详情`。
- 不从 URL 字符串临时拼中文，避免路径文案漂移。

结论：采用。

## 拒绝方案

### 只删除 description，保留大标题

该方案改动小，但仍保留每页左上角大标题。用户明确指出“大标题 + 页面描述”整体冗余，且保留大标题会继续和侧边栏当前项重复。

结论：拒绝。

### 每个页面手工删除文案

该方案可以快速处理截图问题，但会留下多个局部风格：有的页面有 header，有的页面无 header，有的页面仍有大 section subtitle。后续新增页面也容易复发。

结论：拒绝。

### 把说明搬进帮助抽屉

该方案减少首屏文字，但仍把产品说明内嵌在业务页面中，扩大实现范围，也不是当前用户目标。

结论：拒绝。

## 页面级设计

### 总览

删除：

- `MCP SQL Gateway` eyebrow。
- `总览` 大标题。
- `overviewQuery.data.windowLabel` 页面描述。
- 环境选择器下方“环境过滤将在后续阶段接入”。
- 最近审计事件副标题“按创建时间倒序展示最近 5 条审计记录”。
- 需要关注事项副标题“策略拒绝、待确认 SQL、异常连接池和临期 Token”。
- 最近审计 empty state description。

保留：

- 指标卡片标题和值。
- 指标单位或紧凑辅助值，但避免整句说明。
- 最近审计表格列名和事件数据。
- 需要关注事项标题、状态列表和操作入口。

布局调整：

- 顶部从 header band 收敛为 breadcrumb strip。
- 指标卡片区域上移，减少 header 下方空白。
- section 标题只保留一行，标题与内容间距收敛。

### 用户管理

删除：

- 页面描述“管理 DBFlow 操作员账户、访问状态、授权数量与活跃 MCP Token。”
- empty state description。

保留：

- 用户表格、筛选器、状态 badge、操作按钮、创建或编辑表单。

布局调整：

- 原 header actions 移到 breadcrumb strip 右侧。
- 表格容器上移，减少首屏解释区。

### 项目授权

删除：

- 页面描述“按用户和项目管理可访问的 DBFlow 环境边界。”
- empty state description。

保留：

- 授权表格、用户/项目/环境字段、状态、创建授权操作。

布局调整：

- 表格与 breadcrumb strip 之间只保留紧凑间距。

### Token 管理

删除：

- 页面描述“管理 DBFlow MCP SQL Gateway 的访问 Token，并确保明文只在颁发后一次性展示。”
- empty state description。

保留：

- Token 列表、颁发按钮、撤销/状态操作。
- Token 颁发后明文只显示一次的安全提示，但应压缩为短句。

布局调整：

- 创建 Token 的主要操作进入 breadcrumb strip 右侧。

### 配置查看

删除：

- 页面描述“查看 DBFlow 项目环境配置的脱敏摘要。”
- empty state description。

保留：

- 配置字段、脱敏值、环境和数据源标识。
- 错误状态和刷新操作。

布局调整：

- 配置卡片按信息密度重新贴近顶部。

### 危险策略

删除：

- 页面描述“只读查看 DBFlow 对 DROP 与 TRUNCATE 等高危操作的服务端策略。”
- `SectionTitle` 描述“未命中白名单或确认挑战时，后端按默认策略 fail-closed。”
- `DROP 白名单` 描述“仅展示 YAML/Nacos 当前生效的 DROP 白名单范围。”
- 空白名单描述“DROP_DATABASE / DROP_TABLE 将按默认策略拒绝。”
- 固定规则描述“这些规则由服务端强制执行，React 页面只读展示。”

保留：

- 策略名称、操作类型、risk、requirement、decision、白名单条目。
- 只读状态可以通过 badge 或列名表达，不需要整句说明。
- 策略详情抽屉中的必要字段和值。

布局调整：

- 高危策略页保留分组标题，但不加解释段落。
- 空白名单时显示短标题，例如“无白名单条目”，不再追加解释句。

### 审计列表

删除：

- 页面描述“按时间、用户、项目、环境、risk、decision、SQL hash 和 tool 查询 MCP SQL Gateway 审计事件。”
- empty state description。
- 筛选抽屉中重复说明筛选字段的整句文案。

保留：

- 筛选器 label、时间范围、用户、项目、环境、risk、decision、SQL hash、tool。
- 审计表格、详情入口、错误消息。

布局调整：

- 筛选入口和刷新操作进入 breadcrumb strip 或表格工具栏。

### 审计详情

删除：

- 页面描述“只读审计详情，用于复盘 MCP SQL 请求、策略判定与失败原因。”
- timeline 描述“基于已持久化审计详情重建的只读链路摘要。”

保留：

- 审计 ID、状态、risk、decision、SQL hash、tool、用户、项目、环境、时间。
- 后端返回的 failure/error/result summary。
- timeline 节点名称和值。

布局调整：

- 详情页顶部显示 `审计 / 审计详情`，关键状态 badge 靠近路径或首个详情卡。

### 系统健康

删除：

- 页面描述“查看 DBFlow 元数据、目标数据源、Nacos 与 MCP Endpoint 的只读健康摘要。”
- 指标卡片 description，例如“非健康项数量 / 健康项总数”“当前后端返回的健康项数量”。

保留：

- 健康状态、子项名称、状态 badge、详情值、刷新操作。
- `tone` 这类技术字段仅在有排障价值时作为紧凑 key/value 展示，不作为整句说明。

布局调整：

- 健康指标卡片转为更紧凑的 metric grid。

### 设置

删除：

- `Settings` 大标题。
- `Manage your account settings and set e-mail preferences.`

保留：

- 设置页侧向导航或 tab。
- 外观设置控件、主题选择、可操作 label。

布局调整：

- 设置内容区顶部统一使用 breadcrumb strip。
- 设置导航与内容之间减少说明区占位。

## 组件边界

预计实现涉及：

- `dbflow-admin/src/components/dbflow/page-header.tsx`
- `dbflow-admin/src/lib/routes.ts`
- `dbflow-admin/src/features/dashboard/**`
- `dbflow-admin/src/features/users/index.tsx`
- `dbflow-admin/src/features/grants/index.tsx`
- `dbflow-admin/src/features/tokens/index.tsx`
- `dbflow-admin/src/features/config/index.tsx`
- `dbflow-admin/src/features/policies/dangerous/**`
- `dbflow-admin/src/features/audit/list/**`
- `dbflow-admin/src/features/audit/detail/**`
- `dbflow-admin/src/features/health/index.tsx`
- `dbflow-admin/src/features/settings/index.tsx`
- 受影响的 `*.test.tsx`

允许改造：

- `PageHeader` 可以被重命名或内部改造成低密度组件，但调用点最终语义应是 breadcrumb path，而不是 title header。
- `EmptyState` 可以继续支持 `description` prop，但业务调用默认不传；保留 prop 是为了错误或少数必要空状态。
- 页面本地的 `SectionTitle`、metric card、filter sheet intro 可以删除 description 参数或改成只渲染标题。

不得改造：

- API client、React Query hooks、业务 DTO。
- 后端返回字段。
- 认证、权限、审计策略。
- 全局导航结构和路由 URL。

## 测试策略

### 组件测试

更新受影响页面测试：

- 移除对被删除说明文案的断言。
- 新增或调整对 breadcrumb path 的断言。
- 保留对表格、按钮、筛选器、状态 badge、错误和关键数据的断言。
- 危险策略测试不再断言长说明文案；改为断言策略数据、白名单空状态短标题和详情抽屉字段。
- Dashboard 测试不再断言 `windowLabel` 被渲染；API 测试可继续断言后端 payload 解析。

### 视觉验证

执行阶段应使用真实浏览器检查代表页面：

- `/admin/`
- `/admin/users`
- `/admin/grants`
- `/admin/tokens`
- `/admin/config`
- `/admin/policies/dangerous`
- `/admin/audit`
- `/admin/health`
- `/admin/settings/appearance`

每页验证：

- 左上角只有紧凑面包屑，没有大标题和页面描述。
- 删除文案后没有异常空白、错位、组件漂移或文本重叠。
- 桌面和移动视口均可扫描。

### 命令验证

执行计划完成时需要运行：

- `pnpm --dir dbflow-admin test`
- `pnpm --dir dbflow-admin lint`
- `pnpm --dir dbflow-admin build`
- `python3 scripts/check_harness.py`
- `git diff --check`

如果实现触碰 Java 后端或打包资源映射，再补充运行：

- `./mvnw test`

## 验收标准

- 认证后每个页面左上角均使用 breadcrumb path，不再渲染原 `PageHeader` 大标题和页面描述。
- 用户列出的所有示例说明文案均不再出现在页面可见文本中。
- 同类说明文案被系统性清理，不只处理用户列出的样例。
- 清理后页面垂直间距明显收敛，首屏优先展示数据、表格、筛选和操作。
- 表单、筛选、按钮、表格、状态和错误仍保持可理解、可访问、可测试。
- 危险操作、Token 一次性展示、错误回退等安全关键文本没有被误删。
- 受影响测试通过，且测试关注点从说明文案迁移到路径、数据、状态和操作。

## 风险与控制

- 风险：激进删除后新用户缺少上下文。
  控制：侧边栏、breadcrumb、表格列名、状态 badge 和操作按钮承担上下文；不在页面写培训材料。

- 风险：误删安全关键提示。
  控制：安全确认、Token 明文一次性展示、错误消息和权限失败提示明确列为保留项。

- 风险：测试大量依赖旧说明文案。
  控制：同步更新测试断言到业务数据和操作语义，避免继续把说明文案当成产品契约。

- 风险：删除 header 后布局不平衡。
  控制：把 breadcrumb strip 作为统一顶部结构，并逐页压缩 section 间距和卡片 padding。

## 计划交接

下一步进入 `harness-plan` 时，实施计划应按以下顺序拆分：

1. 建立全局 breadcrumb path 组件和路由映射。
2. 迁移所有 `PageHeader` 调用并保留 actions。
3. 分页面删除说明文案并调整局部布局。
4. 更新测试断言。
5. 运行命令验证和浏览器视觉验证。
