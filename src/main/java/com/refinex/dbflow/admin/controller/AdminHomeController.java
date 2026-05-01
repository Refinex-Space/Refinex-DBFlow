package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.command.CreateUserCommand;
import com.refinex.dbflow.admin.command.GrantEnvironmentCommand;
import com.refinex.dbflow.admin.command.IssueTokenCommand;
import com.refinex.dbflow.admin.command.UpdateProjectGrantsCommand;
import com.refinex.dbflow.admin.service.AdminAccessManagementService;
import com.refinex.dbflow.admin.service.AdminOperationsViewService;
import com.refinex.dbflow.admin.service.AdminOverviewViewService;
import com.refinex.dbflow.admin.service.AdminShellViewService;
import com.refinex.dbflow.admin.view.GrantFilter;
import com.refinex.dbflow.admin.view.TokenFilter;
import com.refinex.dbflow.admin.view.UserFilter;
import com.refinex.dbflow.audit.dto.AuditQueryCriteria;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.security.properties.AdminSecurityProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
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
     * 管理端安全配置属性（用于读取初始化管理员用户名）。
     */
    private final AdminSecurityProperties adminSecurityProperties;

    /**
     * 管理端总览页视图服务。
     */
    private final AdminOverviewViewService overviewViewService;

    /**
     * 管理端运维页面视图服务。
     */
    private final AdminOperationsViewService operationsViewService;

    /**
     * 管理端共享 shell 视图服务。
     */
    private final AdminShellViewService shellViewService;

    /**
     * 创建管理端 Thymeleaf 页面控制器。
     *
     * @param accessManagementService 管理端访问管理服务
     * @param overviewViewService     管理端总览页视图服务
     * @param operationsViewService   管理端运维页面视图服务
     * @param shellViewService        管理端共享 shell 视图服务
     */
    public AdminHomeController(
            AdminAccessManagementService accessManagementService,
            AdminSecurityProperties adminSecurityProperties,
            AdminOverviewViewService overviewViewService,
            AdminOperationsViewService operationsViewService,
            AdminShellViewService shellViewService) {
        this.accessManagementService = accessManagementService;
        this.adminSecurityProperties = adminSecurityProperties;
        this.overviewViewService = overviewViewService;
        this.operationsViewService = operationsViewService;
        this.shellViewService = shellViewService;
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
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @return 总览页模板
     */
    @GetMapping("/admin")
    public String overview(Model model, Authentication authentication) {
        addCommonModel(model, authentication, "overview",
                "route=/admin · template=admin/overview.html · fragment=dashboardSummary + recentAuditTable + attentionList");
        model.addAttribute("overview", overviewViewService.overview());
        return "admin/overview";
    }

    /**
     * 显示用户管理页。
     *
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @return 用户管理页模板
     */
    @GetMapping("/admin/users")
    public String users(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            Model model,
            Authentication authentication) {
        addCommonModel(model, authentication, "users",
                "route=/admin/users · template=admin/users.html · fragment=filterBar + userTable");
        model.addAttribute("users", accessManagementService.listUsers(
                new UserFilter(username, status)));
        model.addAttribute("initialAdminUsername", adminSecurityProperties.getUsername());
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
            RedirectAttributes redirectAttributes) {
        try {
            accessManagementService.createUser(
                    new CreateUserCommand(username, displayName, password));
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
     * 启用用户。
     *
     * @param userId             用户主键
     * @param redirectAttributes 重定向属性
     * @return 用户管理页重定向
     */
    @PostMapping("/admin/users/{userId}/enable")
    public String enableUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            accessManagementService.enableUser(userId);
            redirectAttributes.addFlashAttribute("successMessage", "用户已启用");
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * 重置用户密码。
     *
     * @param userId             用户主键
     * @param newPassword        新明文密码
     * @param redirectAttributes 重定向属性
     * @return 用户管理页重定向
     */
    @PostMapping("/admin/users/{userId}/reset-password")
    public String resetPassword(
            @PathVariable Long userId,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes) {
        try {
            accessManagementService.resetPassword(userId, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "密码已重置");
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * 显示项目环境授权页。
     *
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @return 项目环境授权页模板
     */
    @GetMapping("/admin/grants")
    public String grants(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String projectKey,
            @RequestParam(required = false) String environmentKey,
            @RequestParam(required = false) String status,
            Model model,
            Authentication authentication) {
        addCommonModel(model, authentication, "grants",
                "route=/admin/grants · template=admin/grants.html · fragment=grantFilterBar + grantTable + grantModal");
        model.addAttribute("grants", accessManagementService.listGrantGroups(
                new GrantFilter(username, projectKey, environmentKey, status)));
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
            RedirectAttributes redirectAttributes) {
        try {
            accessManagementService.grantEnvironment(new GrantEnvironmentCommand(
                    userId,
                    projectKey,
                    environmentKey,
                    grantType));
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
     * 更新某用户在某项目下已授权的环境列表（勾选式编辑）。
     *
     * @param userId             用户主键
     * @param projectKey         项目标识
     * @param environmentKeys    选中的环境标识列表（可为空）
     * @param grantType          统一授权类型
     * @param redirectAttributes 重定向属性
     * @return 授权页重定向
     */
    @PostMapping("/admin/grants/update-project")
    public String updateProjectGrants(
            @RequestParam Long userId,
            @RequestParam String projectKey,
            @RequestParam(required = false) List<String> environmentKeys,
            @RequestParam String grantType,
            RedirectAttributes redirectAttributes) {
        try {
            accessManagementService.updateUserProjectGrants(
                    new UpdateProjectGrantsCommand(
                            userId, projectKey, environmentKeys, grantType));
            redirectAttributes.addFlashAttribute("successMessage", "环境授权已更新");
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/grants";
    }

    /**
     * 显示 Token 管理页。
     *
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @return Token 管理页模板
     */
    @GetMapping("/admin/tokens")
    public String tokens(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            Model model,
            Authentication authentication) {
        addCommonModel(model, authentication, "tokens",
                "route=/admin/tokens · template=admin/tokens.html · fragment=tokenFilterBar + tokenTable + tokenIssueModal + revokeModal");
        model.addAttribute("tokens", accessManagementService.listTokens(
                new TokenFilter(username, status)));
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
            RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("issuedToken", accessManagementService.issueToken(
                    new IssueTokenCommand(userId, expiresInDays)));
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
            RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("issuedToken", accessManagementService.reissueToken(
                    new IssueTokenCommand(userId, expiresInDays)));
        } catch (DbflowException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/tokens";
    }

    /**
     * 显示配置查看页。
     *
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @return 配置查看页模板
     */
    @GetMapping("/admin/config")
    public String config(Model model, Authentication authentication) {
        addCommonModel(model, authentication, "config",
                "route=/admin/config · template=admin/config.html · fragment=configTable + sanitizedConfigCell");
        model.addAttribute("configPage", operationsViewService.configPage());
        return "admin/config";
    }

    /**
     * 显示危险策略页。
     *
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @return 危险策略页模板
     */
    @GetMapping("/admin/policies/dangerous")
    public String dangerousPolicies(Model model, Authentication authentication) {
        addCommonModel(model, authentication, "policies",
                "route=/admin/policies/dangerous · template=admin/policies-dangerous.html · fragment=policyFilterBar + policyTable + reasonDrawer");
        model.addAttribute("policyPage", operationsViewService.dangerousPolicyPage());
        return "admin/policies-dangerous";
    }

    /**
     * 显示审计列表页。
     *
     * @param from           创建时间起点
     * @param to             创建时间终点
     * @param userId         用户主键
     * @param project        项目标识
     * @param env            环境标识
     * @param risk           风险级别
     * @param decision       决策
     * @param sqlHash        SQL hash
     * @param tool           工具名称
     * @param page           页码
     * @param size           每页条数
     * @param sort           排序字段
     * @param direction      排序方向
     * @param model          页面模型
     * @param authentication 当前认证信息
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
            Model model,
            Authentication authentication) {
        addCommonModel(model, authentication, "audit",
                "route=/admin/audit · template=admin/audit-list.html · fragment=auditFilterBar + auditTable + pagination");
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
                direction)));
        return "admin/audit-list";
    }

    /**
     * 显示审计详情页。
     *
     * @param eventId        审计事件主键
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @return 审计详情页模板
     */
    @GetMapping("/admin/audit/{eventId}")
    public String auditDetail(@PathVariable Long eventId, Model model, Authentication authentication) {
        addCommonModel(model, authentication, "audit-detail",
                "route=/admin/audit/{eventId} · template=admin/audit-detail.html · fragment=auditIdentityPanel + sqlPanel + timeline");
        model.addAttribute("audit", operationsViewService.auditDetail(eventId));
        return "admin/audit-detail";
    }

    /**
     * 显示系统健康页。
     *
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @return 系统健康页模板
     */
    @GetMapping("/admin/health")
    public String health(Model model, Authentication authentication) {
        addCommonModel(model, authentication, "health",
                "route=/admin/health · template=admin/health.html · fragment=healthSummary + healthGrid");
        model.addAttribute("healthPage", operationsViewService.healthPage());
        return "admin/health";
    }

    /**
     * 添加管理端共享模型字段。
     *
     * @param model          页面模型
     * @param authentication 当前认证信息
     * @param activeNav      当前导航标识
     * @param routeHint      路由映射提示
     */
    private void addCommonModel(Model model, Authentication authentication, String activeNav, String routeHint) {
        model.addAttribute("activeNav", activeNav);
        model.addAttribute("routeHint", routeHint);
        model.addAttribute("shell", shellViewService.shell(authentication));
    }
}
