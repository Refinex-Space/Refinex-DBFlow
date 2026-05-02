package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.service.AdminOperationsViewService;
import com.refinex.dbflow.admin.view.ConfigPageView;
import com.refinex.dbflow.admin.view.DangerousPolicyPageView;
import com.refinex.dbflow.admin.view.HealthPageView;
import com.refinex.dbflow.common.ApiResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * React 管理端运维只读 API 控制器。
 *
 * @author refinex
 */
@RestController
@RequestMapping(value = "/admin/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminOperationsApiController {

    /**
     * 管理端运维页面视图服务。
     */
    private final AdminOperationsViewService operationsViewService;

    /**
     * 创建 React 管理端运维只读 API 控制器。
     *
     * @param operationsViewService 管理端运维页面视图服务
     */
    public AdminOperationsApiController(AdminOperationsViewService operationsViewService) {
        this.operationsViewService = operationsViewService;
    }

    /**
     * 查询配置查看页只读数据。
     *
     * @return 配置查看页只读视图
     */
    @GetMapping("/config")
    public ApiResult<ConfigPageView> config() {
        return ApiResult.ok(operationsViewService.configPage());
    }

    /**
     * 查询危险策略页只读数据。
     *
     * @return 危险策略页只读视图
     */
    @GetMapping("/policies/dangerous")
    public ApiResult<DangerousPolicyPageView> dangerousPolicies() {
        return ApiResult.ok(operationsViewService.dangerousPolicyPage());
    }

    /**
     * 查询系统健康页只读数据。
     *
     * @return 系统健康页只读视图
     */
    @GetMapping("/health")
    public ApiResult<HealthPageView> health() {
        return ApiResult.ok(operationsViewService.healthPage());
    }
}
