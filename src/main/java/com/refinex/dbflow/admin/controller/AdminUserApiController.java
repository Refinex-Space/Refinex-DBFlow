package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.command.CreateUserCommand;
import com.refinex.dbflow.admin.dto.AdminCreateUserRequest;
import com.refinex.dbflow.admin.dto.AdminResetPasswordRequest;
import com.refinex.dbflow.admin.service.AdminAccessManagementService;
import com.refinex.dbflow.admin.view.UserFilter;
import com.refinex.dbflow.admin.view.UserRow;
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
 * React 管理端用户管理 JSON API 控制器。
 *
 * @author refinex
 */
@RestController
@RequestMapping(value = "/admin/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminUserApiController {

    /**
     * 管理端访问管理服务。
     */
    private final AdminAccessManagementService accessManagementService;

    /**
     * 创建 React 管理端用户管理 JSON API 控制器。
     *
     * @param accessManagementService 管理端访问管理服务
     */
    public AdminUserApiController(AdminAccessManagementService accessManagementService) {
        this.accessManagementService = accessManagementService;
    }

    /**
     * 查询用户列表。
     *
     * @param username 用户名筛选
     * @param status   用户状态筛选
     * @return 用户行列表
     */
    @GetMapping
    public ApiResult<List<UserRow>> users(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(accessManagementService.listUsers(new UserFilter(username, status)));
    }

    /**
     * 创建用户。
     *
     * @param request 创建用户请求
     * @return 创建后的安全用户行
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<UserRow> createUser(@RequestBody AdminCreateUserRequest request) {
        UserRow createdUser = accessManagementService.createUser(
                new CreateUserCommand(request.username(), request.displayName(), request.password()));
        return ApiResult.ok(createdUser);
    }

    /**
     * 禁用用户。
     *
     * @param userId 用户主键
     * @return 操作结果
     */
    @PostMapping("/{userId}/disable")
    public ApiResult<Map<String, Boolean>> disableUser(@PathVariable Long userId) {
        accessManagementService.disableUser(userId);
        return ApiResult.ok(Map.of("disabled", true));
    }

    /**
     * 启用用户。
     *
     * @param userId 用户主键
     * @return 操作结果
     */
    @PostMapping("/{userId}/enable")
    public ApiResult<Map<String, Boolean>> enableUser(@PathVariable Long userId) {
        accessManagementService.enableUser(userId);
        return ApiResult.ok(Map.of("enabled", true));
    }

    /**
     * 重置用户管理端密码。
     *
     * @param userId  用户主键
     * @param request 重置密码请求
     * @return 操作结果
     */
    @PostMapping(value = "/{userId}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Map<String, Boolean>> resetPassword(
            @PathVariable Long userId,
            @RequestBody AdminResetPasswordRequest request) {
        accessManagementService.resetPassword(userId, request.newPassword());
        return ApiResult.ok(Map.of("reset", true));
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
