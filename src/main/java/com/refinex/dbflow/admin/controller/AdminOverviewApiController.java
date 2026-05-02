package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.service.AdminOverviewViewService;
import com.refinex.dbflow.admin.view.OverviewPageView;
import com.refinex.dbflow.common.ApiResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * React 管理端总览只读 API 控制器。
 *
 * @author refinex
 */
@RestController
@RequestMapping(value = "/admin/api/overview", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminOverviewApiController {

    /**
     * 管理端总览页视图服务。
     */
    private final AdminOverviewViewService overviewViewService;

    /**
     * 创建 React 管理端总览只读 API 控制器。
     *
     * @param overviewViewService 管理端总览页视图服务
     */
    public AdminOverviewApiController(AdminOverviewViewService overviewViewService) {
        this.overviewViewService = overviewViewService;
    }

    /**
     * 查询管理端总览。
     *
     * @return 总览页只读视图
     */
    @GetMapping
    public ApiResult<OverviewPageView> overview() {
        return ApiResult.ok(overviewViewService.overview());
    }
}
