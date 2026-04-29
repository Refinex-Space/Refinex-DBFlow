package com.refinex.dbflow.admin;

import com.refinex.dbflow.audit.service.AuditQueryCriteria;
import com.refinex.dbflow.common.DbflowException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.List;

/**
 * 管理端 Thymeleaf 页面控制器。
 *
 * @author refinex
 */
@Controller
public class AdminHomeController {

    /**
     * 管理端访问管理服务。
     */
    private final AdminAccessManagementService accessManagementService;

    /**
     * 管理端运维页面视图服务。
     */
    private final AdminOperationsViewService operationsViewService;

    /**
     * 创建管理端 Thymeleaf 页面控制器。
     *
     * @param accessManagementService 管理端访问管理服务
     * @param operationsViewService   管理端运维页面视图服务
     */
    public AdminHomeController(
            AdminAccessManagementService accessManagementService,
            AdminOperationsViewService operationsViewService
    ) {
        this.accessManagementService = accessManagementService;
        this.operationsViewService = operationsViewService;
    }

    /**
     * 显示管理端登录页。
     *
     * @return 登录页模板
     */
    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    /**
     * 显示管理端总览页。
     *
     * @param model 页面模型
     * @return 总览页模板
     */
    @GetMapping("/admin")
    public String overview(Model model) {
        addCommonModel(model, "overview", "route=/admin · template=admin/overview.html · fragment=dashboardSummary + recentAuditTable + attentionList");
        model.addAttribute("metrics", metrics());
        model.addAttribute("auditRows", auditRows());
        model.addAttribute("attentionItems", attentionItems());
        return "admin/overview";
    }

    /**
     * 显示用户管理页。
     *
     * @param model 页面模型
     * @return 用户管理页模板
     */
    @GetMapping("/admin/users")
    public String users(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            Model model
    ) {
        addCommonModel(model, "users", "route=/admin/users · template=admin/users.html · fragment=filterBar + userTable");
        model.addAttribute("users", accessManagementService.listUsers(
                new AdminAccessManagementService.UserFilter(username, status)
        ));
        return "admin/users";
    }

    /**
     * 创建用户。
     *
     * @param username           用户名
     * @param displayName        显示名
     * @param password           管理端密码
     * @param redirectAttributes 重定向属性
     * @return 用户管理页重定向
     */
    @PostMapping("/admin/users")
    public String createUser(
            @RequestParam String username,
            @RequestParam String displayName,
            @RequestParam(required = false) String password,
            RedirectAttributes redirectAttributes
    ) {
        try {
            accessManagementService.createUser(
                    new AdminAccessManagementService.CreateUserCommand(username, displayName, password)
            );
            redirectAttributes.addFlashAttribute("successMessage", "用户已创建");
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * 禁用用户。
     *
     * @param userId             用户主键
     * @param redirectAttributes 重定向属性
     * @return 用户管理页重定向
     */
    @PostMapping("/admin/users/{userId}/disable")
    public String disableUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            accessManagementService.disableUser(userId);
            redirectAttributes.addFlashAttribute("successMessage", "用户已禁用");
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * 显示项目环境授权页。
     *
     * @param model 页面模型
     * @return 项目环境授权页模板
     */
    @GetMapping("/admin/grants")
    public String grants(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String projectKey,
            @RequestParam(required = false) String environmentKey,
            @RequestParam(required = false) String status,
            Model model
    ) {
        addCommonModel(model, "grants", "route=/admin/grants · template=admin/grants.html · fragment=grantFilterBar + grantTable + grantModal");
        model.addAttribute("grants", accessManagementService.listGrants(
                new AdminAccessManagementService.GrantFilter(username, projectKey, environmentKey, status)
        ));
        model.addAttribute("userOptions", accessManagementService.listActiveUserOptions());
        model.addAttribute("environmentOptions", accessManagementService.listEnvironmentOptions());
        return "admin/grants";
    }

    /**
     * 授权用户访问项目环境。
     *
     * @param userId             用户主键
     * @param projectKey         项目标识
     * @param environmentKey     环境标识
     * @param grantType          授权类型
     * @param redirectAttributes 重定向属性
     * @return 授权页重定向
     */
    @PostMapping("/admin/grants")
    public String grantEnvironment(
            @RequestParam Long userId,
            @RequestParam String projectKey,
            @RequestParam String environmentKey,
            @RequestParam String grantType,
            RedirectAttributes redirectAttributes
    ) {
        try {
            accessManagementService.grantEnvironment(new AdminAccessManagementService.GrantEnvironmentCommand(
                    userId,
                    projectKey,
                    environmentKey,
                    grantType
            ));
            redirectAttributes.addFlashAttribute("successMessage", "环境授权已创建");
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/grants";
    }

    /**
     * 撤销环境授权。
     *
     * @param grantId            授权主键
     * @param redirectAttributes 重定向属性
     * @return 授权页重定向
     */
    @PostMapping("/admin/grants/{grantId}/revoke")
    public String revokeGrant(@PathVariable Long grantId, RedirectAttributes redirectAttributes) {
        accessManagementService.revokeGrant(grantId);
        redirectAttributes.addFlashAttribute("successMessage", "环境授权已撤销");
        return "redirect:/admin/grants";
    }

    /**
     * 显示 Token 管理页。
     *
     * @param model 页面模型
     * @return Token 管理页模板
     */
    @GetMapping("/admin/tokens")
    public String tokens(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            Model model
    ) {
        addCommonModel(model, "tokens", "route=/admin/tokens · template=admin/tokens.html · fragment=tokenFilterBar + tokenTable + tokenIssueModal + revokeModal");
        model.addAttribute("tokens", accessManagementService.listTokens(
                new AdminAccessManagementService.TokenFilter(username, status)
        ));
        model.addAttribute("userOptions", accessManagementService.listActiveUserOptions());
        return "admin/tokens";
    }

    /**
     * 颁发 Token。
     *
     * @param userId             用户主键
     * @param expiresInDays      有效天数
     * @param redirectAttributes 重定向属性
     * @return Token 管理页重定向
     */
    @PostMapping("/admin/tokens")
    public String issueToken(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer expiresInDays,
            RedirectAttributes redirectAttributes
    ) {
        try {
            redirectAttributes.addFlashAttribute("issuedToken", accessManagementService.issueToken(
                    new AdminAccessManagementService.IssueTokenCommand(userId, expiresInDays)
            ));
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/tokens";
    }

    /**
     * 吊销 Token。
     *
     * @param tokenId            Token 主键
     * @param redirectAttributes 重定向属性
     * @return Token 管理页重定向
     */
    @PostMapping("/admin/tokens/{tokenId}/revoke")
    public String revokeToken(@PathVariable Long tokenId, RedirectAttributes redirectAttributes) {
        accessManagementService.revokeToken(tokenId);
        redirectAttributes.addFlashAttribute("successMessage", "Token 已吊销");
        return "redirect:/admin/tokens";
    }

    /**
     * 重新颁发 Token。
     *
     * @param userId             用户主键
     * @param expiresInDays      有效天数
     * @param redirectAttributes 重定向属性
     * @return Token 管理页重定向
     */
    @PostMapping("/admin/users/{userId}/tokens/reissue")
    public String reissueToken(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer expiresInDays,
            RedirectAttributes redirectAttributes
    ) {
        try {
            redirectAttributes.addFlashAttribute("issuedToken", accessManagementService.reissueToken(
                    new AdminAccessManagementService.IssueTokenCommand(userId, expiresInDays)
            ));
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/tokens";
    }

    /**
     * 显示配置查看页。
     *
     * @param model 页面模型
     * @return 配置查看页模板
     */
    @GetMapping("/admin/config")
    public String config(Model model) {
        addCommonModel(model, "config", "route=/admin/config · template=admin/config.html · fragment=configTable + sanitizedConfigCell");
        model.addAttribute("configs", configs());
        return "admin/config";
    }

    /**
     * 显示危险策略页。
     *
     * @param model 页面模型
     * @return 危险策略页模板
     */
    @GetMapping("/admin/policies/dangerous")
    public String dangerousPolicies(Model model) {
        addCommonModel(model, "policies", "route=/admin/policies/dangerous · template=admin/policies-dangerous.html · fragment=policyFilterBar + policyTable + reasonDrawer");
        model.addAttribute("policyPage", operationsViewService.dangerousPolicyPage());
        return "admin/policies-dangerous";
    }

    /**
     * 显示审计列表页。
     *
     * @param from      创建时间起点
     * @param to        创建时间终点
     * @param userId    用户主键
     * @param project   项目标识
     * @param env       环境标识
     * @param risk      风险级别
     * @param decision  决策
     * @param sqlHash   SQL hash
     * @param tool      工具名称
     * @param page      页码
     * @param size      每页条数
     * @param sort      排序字段
     * @param direction 排序方向
     * @param model     页面模型
     * @return 审计列表页模板
     */
    @GetMapping("/admin/audit")
    public String audit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String env,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) String sqlHash,
            @RequestParam(required = false) String tool,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction,
            Model model
    ) {
        addCommonModel(model, "audit", "route=/admin/audit · template=admin/audit-list.html · fragment=auditFilterBar + auditTable + pagination");
        model.addAttribute("auditPage", operationsViewService.auditPage(new AuditQueryCriteria(
                from,
                to,
                userId,
                project,
                env,
                risk,
                decision,
                sqlHash,
                tool,
                page,
                size,
                sort,
                direction
        )));
        return "admin/audit-list";
    }

    /**
     * 显示审计详情页。
     *
     * @param eventId 审计事件主键
     * @param model   页面模型
     * @return 审计详情页模板
     */
    @GetMapping("/admin/audit/{eventId}")
    public String auditDetail(@PathVariable Long eventId, Model model) {
        addCommonModel(model, "audit-detail", "route=/admin/audit/{eventId} · template=admin/audit-detail.html · fragment=auditIdentityPanel + sqlPanel + timeline");
        model.addAttribute("audit", operationsViewService.auditDetail(eventId));
        return "admin/audit-detail";
    }

    /**
     * 显示系统健康页。
     *
     * @param model 页面模型
     * @return 系统健康页模板
     */
    @GetMapping("/admin/health")
    public String health(Model model) {
        addCommonModel(model, "health", "route=/admin/health · template=admin/health.html · fragment=healthSummary + healthGrid");
        model.addAttribute("healthPage", operationsViewService.healthPage());
        return "admin/health";
    }

    /**
     * 添加管理端共享模型字段。
     *
     * @param model     页面模型
     * @param activeNav 当前导航标识
     * @param routeHint 路由映射提示
     */
    private void addCommonModel(Model model, String activeNav, String routeHint) {
        model.addAttribute("activeNav", activeNav);
        model.addAttribute("routeHint", routeHint);
        model.addAttribute("adminName", "admin.refinex");
        model.addAttribute("configSource", "Nacos prod");
        model.addAttribute("mcpStatus", "MCP 正常");
    }

    /**
     * 构造总览指标演示数据。
     *
     * @return 总览指标
     */
    private List<MetricCard> metrics() {
        return List.of(
                new MetricCard("SQL 请求", "128", "含 explain / inspect", "neutral"),
                new MetricCard("策略拒绝", "9", "7 条 DROP，2 条 GRANT", "bad"),
                new MetricCard("待确认", "2", "TRUNCATE TTL 内", "warn"),
                new MetricCard("有效 Token", "17", "3 个 7 天内过期", "neutral"),
                new MetricCard("已授权环境", "12", "5 项目 / 4 生产", "neutral"),
                new MetricCard("异常数据源", "1", "risk-lab / dev", "warn")
        );
    }

    /**
     * 构造最近关注事项演示数据。
     *
     * @return 关注事项
     */
    private List<AttentionItem> attentionItems() {
        return List.of(
                new AttentionItem("DROP TABLE 被拒绝", "POLICY_DENIED", "bad", "/admin/policies/dangerous"),
                new AttentionItem("TRUNCATE 等待确认", "REQUIRES_CONFIRMATION", "warn", "/admin/audit"),
                new AttentionItem("risk-lab / dev 数据源连接失败", "DEGRADED", "warn", "/admin/health"),
                new AttentionItem("3 个 Token 7 天内过期", "EXPIRING", "neutral", "/admin/tokens")
        );
    }

    /**
     * 构造用户管理演示数据。
     *
     * @return 用户列表
     */
    private List<UserRow> users() {
        return List.of(
                new UserRow("U-1001", "admin.refinex", "平台管理员", "ADMIN", "ACTIVE", 6, 2, "2026-04-29 15:42"),
                new UserRow("U-1002", "zhang.wei", "研发负责人", "OPERATOR", "ACTIVE", 4, 1, "2026-04-29 14:18"),
                new UserRow("U-1003", "lin.chen", "数据平台", "OPERATOR", "ACTIVE", 3, 1, "2026-04-29 11:03"),
                new UserRow("U-1004", "svc-codex", "Codex 服务账号", "OPERATOR", "DISABLED", 0, 0, "2026-04-18 09:10")
        );
    }

    /**
     * 构造项目环境授权演示数据。
     *
     * @return 授权列表
     */
    private List<GrantRow> grants() {
        return List.of(
                new GrantRow("G-9101", "admin.refinex", "billing-core", "prod", "ACTIVE", "admin.refinex", "2026-04-29 09:20", "HEALTHY"),
                new GrantRow("G-9102", "zhang.wei", "billing-core", "staging", "ACTIVE", "admin.refinex", "2026-04-28 17:46", "HEALTHY"),
                new GrantRow("G-9103", "lin.chen", "risk-lab", "staging", "ACTIVE", "admin.refinex", "2026-04-27 10:31", "HEALTHY"),
                new GrantRow("G-9104", "lin.chen", "risk-lab", "dev", "ACTIVE", "zhang.wei", "2026-04-26 16:12", "DEGRADED")
        );
    }

    /**
     * 构造 Token 管理演示数据。
     *
     * @return Token 列表
     */
    private List<TokenRow> tokens() {
        return List.of(
                new TokenRow("T-7001", "admin.refinex", "dbf_live_A7K2", "ACTIVE", "Codex", "10.18.4.21", "2026-05-29", "2026-04-29 15:42"),
                new TokenRow("T-7002", "zhang.wei", "dbf_live_K9Q4", "ACTIVE", "Claude Desktop", "10.18.4.36", "2026-05-12", "2026-04-29 14:18"),
                new TokenRow("T-7003", "lin.chen", "dbf_live_M2P8", "REVOKED", "OpenCode", "10.18.6.12", "2026-05-01", "2026-04-28 19:21"),
                new TokenRow("T-7004", "svc-codex", "dbf_live_R8X1", "EXPIRED", "Codex", "10.18.9.8", "2026-04-25", "2026-04-20 09:04")
        );
    }

    /**
     * 构造配置查看演示数据。
     *
     * @return 配置列表
     */
    private List<ConfigRow> configs() {
        return List.of(
                new ConfigRow("billing-core", "prod", "mysql8-primary", "mysql", "10.22.8.17", "3306", "billing", "dbflow_runner", "maxRows=1000 timeout=8s fetchSize=200", "2026-04-29 15:30"),
                new ConfigRow("billing-core", "staging", "mysql8-stg", "mysql", "10.22.8.44", "3306", "billing_stg", "dbflow_runner", "maxRows=2000 timeout=12s fetchSize=300", "2026-04-29 15:30"),
                new ConfigRow("risk-lab", "dev", "mysql57-dev", "mysql", "10.23.3.21", "3306", "risk_dev", "dbflow_runner", "maxRows=500 timeout=5s fetchSize=100", "2026-04-29 15:30")
        );
    }

    /**
     * 构造危险策略演示数据。
     *
     * @return 策略列表
     */
    private List<PolicyRow> policies() {
        return List.of(
                new PolicyRow("DROP_TABLE", "CRITICAL", "billing-core", "prod", "DENY", "需要 YAML 白名单", "DROP_TABLE_NOT_WHITELISTED", "2026-04-29 13:12"),
                new PolicyRow("DROP_DATABASE", "CRITICAL", "*", "prod", "DENY", "默认拒绝", "DROP_DATABASE_FORBIDDEN", "2026-04-29 13:12"),
                new PolicyRow("TRUNCATE", "HIGH", "risk-lab", "staging", "CONFIRM", "需要 10 分钟内确认", "CONFIRMATION_REQUIRED", "2026-04-29 13:12"),
                new PolicyRow("TRUNCATE", "HIGH", "billing-core", "prod", "DENY", "生产环境未放行", "PROD_TRUNCATE_BLOCKED", "2026-04-29 13:12")
        );
    }

    /**
     * 构造审计演示数据。
     *
     * @return 审计列表
     */
    private List<AuditRow> auditRows() {
        return List.of(
                new AuditRow("A-20260429-154512", "2026-04-29 15:45:12", "admin.refinex", "billing-core", "prod", "dbflow_execute_sql", "SELECT", "LOW", "EXECUTED", "sha256:3f72a9bd", "48ms", "返回 100 行，已按 maxRows 截断", "SELECT id, status, amount FROM payment_order WHERE status = 'PENDING' ORDER BY created_at DESC LIMIT 100"),
                new AuditRow("A-20260429-151804", "2026-04-29 15:18:04", "zhang.wei", "billing-core", "staging", "dbflow_execute_sql", "UPDATE", "MEDIUM", "EXECUTED", "sha256:a10f08ce", "93ms", "affected_rows=12，warning=0", "UPDATE payment_retry SET retry_state = 'READY' WHERE retry_state = 'FAILED' AND retry_count < 3"),
                new AuditRow("A-20260429-150233", "2026-04-29 15:02:33", "lin.chen", "risk-lab", "staging", "dbflow_execute_sql", "TRUNCATE", "HIGH", "REQUIRES_CONFIRMATION", "sha256:f90219ab", "0ms", "confirmation_id=CFM-6209，expires_in=10m", "TRUNCATE TABLE risk_calc_sandbox"),
                new AuditRow("A-20260429-145511", "2026-04-29 14:55:11", "lin.chen", "billing-core", "prod", "dbflow_execute_sql", "DROP_TABLE", "CRITICAL", "POLICY_DENIED", "sha256:71d0bb31", "0ms", "reason=DROP_TABLE_NOT_WHITELISTED", "DROP TABLE payment_order"),
                new AuditRow("A-20260429-142008", "2026-04-29 14:20:08", "zhang.wei", "billing-core", "prod", "dbflow_explain_sql", "EXPLAIN", "LOW", "EXECUTED", "sha256:91c4e668", "35ms", "type=ALL，rows=98044，建议检查过滤列索引", "EXPLAIN FORMAT=JSON SELECT * FROM payment_order WHERE merchant_id = 3301")
        );
    }

    /**
     * 构造健康状态演示数据。
     *
     * @return 健康状态列表
     */
    private List<HealthItem> healthItems() {
        return List.of(
                new HealthItem("应用进程", "HEALTHY", "v0.1.0-SNAPSHOT，uptime 6h 12m", "JVM 412MB / 1024MB"),
                new HealthItem("MCP Streamable HTTP", "HEALTHY", "/mcp 可用，最近请求 1 分钟前", "p95=96ms"),
                new HealthItem("元数据库", "HEALTHY", "Flyway V1 applied", "Hikari active=2 idle=6"),
                new HealthItem("Nacos Config", "HEALTHY", "namespace=dbflow-prod", "last reload 15:30:00"),
                new HealthItem("billing-core / prod", "HEALTHY", "mysql8-primary", "active=1 idle=9 wait=0"),
                new HealthItem("risk-lab / dev", "DEGRADED", "mysql57-dev connection timeout", "last error 14:00:06")
        );
    }

    /**
     * 总览指标卡。
     */
    public record MetricCard(String label, String value, String hint, String tone) {
    }

    /**
     * 关注事项行。
     */
    public record AttentionItem(String label, String status, String tone, String href) {
    }

    /**
     * 用户表格行。
     */
    public record UserRow(String id, String username, String displayName, String role, String status, int grantCount,
                          int tokenCount, String lastUsedAt) {
    }

    /**
     * 授权表格行。
     */
    public record GrantRow(String id, String username, String project, String env, String status, String grantedBy,
                           String grantedAt, String datasourceStatus) {
    }

    /**
     * Token 表格行。
     */
    public record TokenRow(String id, String username, String prefix, String status, String client, String sourceIp,
                           String expiresAt, String lastUsedAt) {
    }

    /**
     * 配置表格行。
     */
    public record ConfigRow(String project, String env, String datasource, String type, String host, String port,
                            String schema, String username, String limits, String loadedAt) {
    }

    /**
     * 危险策略表格行。
     */
    public record PolicyRow(String operation, String risk, String project, String env, String decision,
                            String requirement, String reason, String updatedAt) {
    }

    /**
     * 审计表格行。
     */
    public record AuditRow(String id, String time, String user, String project, String env, String tool,
                           String operation, String risk, String decision, String sqlHash, String duration,
                           String summary, String sqlText) {
    }

    /**
     * 健康状态项。
     */
    public record HealthItem(String name, String status, String description, String detail) {
    }
}
