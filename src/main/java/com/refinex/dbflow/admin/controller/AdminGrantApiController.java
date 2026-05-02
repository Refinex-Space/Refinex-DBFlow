package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.command.GrantEnvironmentCommand;
import com.refinex.dbflow.admin.command.UpdateProjectGrantsCommand;
import com.refinex.dbflow.admin.dto.GrantEnvironmentRequest;
import com.refinex.dbflow.admin.dto.GrantOptionsResponse;
import com.refinex.dbflow.admin.dto.UpdateProjectGrantsRequest;
import com.refinex.dbflow.admin.service.AdminAccessManagementService;
import com.refinex.dbflow.admin.view.GrantFilter;
import com.refinex.dbflow.admin.view.GrantGroupRow;
import com.refinex.dbflow.admin.view.GrantRow;
import com.refinex.dbflow.common.ApiResult;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * React 管理端项目环境授权 JSON API 控制器。
 *
 * @author refinex
 */
@RestController
@RequestMapping(value = "/admin/api/grants", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminGrantApiController {

    /**
     * 管理端访问管理服务。
     */
    private final AdminAccessManagementService accessManagementService;

    /**
     * 创建 React 管理端项目环境授权 JSON API 控制器。
     *
     * @param accessManagementService 管理端访问管理服务
     */
    public AdminGrantApiController(AdminAccessManagementService accessManagementService) {
        this.accessManagementService = accessManagementService;
    }

    /**
     * 查询项目环境授权分组列表。
     *
     * @param username       用户名筛选
     * @param projectKey     项目标识筛选
     * @param environmentKey 环境标识筛选
     * @param status         授权状态筛选
     * @return 授权分组列表
     */
    @GetMapping
    public ApiResult<List<GrantGroupRow>> grants(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String projectKey,
            @RequestParam(required = false) String environmentKey,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(accessManagementService.listGrantGroups(
                new GrantFilter(username, projectKey, environmentKey, status)));
    }

    /**
     * 查询创建授权所需选项。
     *
     * @return active 用户和可授权环境选项
     */
    @GetMapping("/options")
    public ApiResult<GrantOptionsResponse> options() {
        return ApiResult.ok(new GrantOptionsResponse(
                accessManagementService.listActiveUserOptions(),
                accessManagementService.listEnvironmentOptions()));
    }

    /**
     * 创建环境授权。
     *
     * @param request 创建环境授权请求
     * @return 创建后的授权行
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<GrantRow> grantEnvironment(@RequestBody GrantEnvironmentRequest request) {
        GrantRow createdGrant = accessManagementService.grantEnvironment(new GrantEnvironmentCommand(
                request.userId(),
                request.projectKey(),
                request.environmentKey(),
                request.grantType()));
        return ApiResult.ok(createdGrant);
    }

    /**
     * 更新用户在某项目下的环境授权列表。
     *
     * @param request 更新项目授权请求
     * @return 操作结果
     */
    @PostMapping(value = "/update-project", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Map<String, Boolean>> updateProjectGrants(@RequestBody UpdateProjectGrantsRequest request) {
        accessManagementService.updateUserProjectGrants(new UpdateProjectGrantsCommand(
                request.userId(),
                request.projectKey(),
                request.environmentKeys(),
                request.grantType()));
        return ApiResult.ok(Map.of("updated", true));
    }

    /**
     * 撤销环境授权。
     *
     * @param grantId 授权主键
     * @return 操作结果
     */
    @PostMapping("/{grantId}/revoke")
    public ApiResult<Map<String, Boolean>> revokeGrant(@PathVariable Long grantId) {
        accessManagementService.revokeGrant(grantId);
        return ApiResult.ok(Map.of("revoked", true));
    }

    /**
     * 将业务校验失败转换为稳定 JSON 4xx 响应。
     *
     * @param exception 业务或参数异常
     * @return 统一失败响应
     */
    @ExceptionHandler({DbflowException.class, IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResult<Void>> handleBadRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.failed(ErrorCode.INVALID_REQUEST));
    }
}
