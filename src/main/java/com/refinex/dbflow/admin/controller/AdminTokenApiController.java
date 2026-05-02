package com.refinex.dbflow.admin.controller;

import com.refinex.dbflow.admin.command.IssueTokenCommand;
import com.refinex.dbflow.admin.dto.AdminTokenRowResponse;
import com.refinex.dbflow.admin.dto.IssueTokenRequest;
import com.refinex.dbflow.admin.dto.ReissueTokenRequest;
import com.refinex.dbflow.admin.dto.TokenOptionsResponse;
import com.refinex.dbflow.admin.service.AdminAccessManagementService;
import com.refinex.dbflow.admin.view.IssuedTokenView;
import com.refinex.dbflow.admin.view.TokenFilter;
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
 * React 管理端 MCP Token 管理 JSON API 控制器。
 *
 * @author refinex
 */
@RestController
@RequestMapping(value = "/admin/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminTokenApiController {

    /**
     * 管理端访问管理服务。
     */
    private final AdminAccessManagementService accessManagementService;

    /**
     * 创建 React 管理端 MCP Token 管理 JSON API 控制器。
     *
     * @param accessManagementService 管理端访问管理服务
     */
    public AdminTokenApiController(AdminAccessManagementService accessManagementService) {
        this.accessManagementService = accessManagementService;
    }

    /**
     * 查询 Token 列表。
     *
     * @param username 用户名筛选
     * @param status   Token 状态筛选
     * @return 不包含明文和 hash 的 Token 列表
     */
    @GetMapping("/tokens")
    public ApiResult<List<AdminTokenRowResponse>> tokens(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status) {
        List<AdminTokenRowResponse> rows = accessManagementService.listTokens(new TokenFilter(username, status))
                .stream()
                .map(AdminTokenRowResponse::from)
                .toList();
        return ApiResult.ok(rows);
    }

    /**
     * 查询 Token 表单选项。
     *
     * @return active 用户选项
     */
    @GetMapping("/tokens/options")
    public ApiResult<TokenOptionsResponse> options() {
        return ApiResult.ok(new TokenOptionsResponse(accessManagementService.listActiveUserOptions()));
    }

    /**
     * 颁发 Token。
     *
     * @param request Token 颁发请求
     * @return 只在本次响应展示明文的 Token 视图
     */
    @PostMapping(value = "/tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<IssuedTokenView> issueToken(@RequestBody IssueTokenRequest request) {
        IssuedTokenView issuedToken = accessManagementService.issueToken(
                new IssueTokenCommand(request.userId(), request.expiresInDays()));
        return ApiResult.ok(issuedToken);
    }

    /**
     * 撤销 Token。
     *
     * @param tokenId Token 主键
     * @return 操作结果
     */
    @PostMapping("/tokens/{tokenId}/revoke")
    public ApiResult<Map<String, Boolean>> revokeToken(@PathVariable Long tokenId) {
        accessManagementService.revokeToken(tokenId);
        return ApiResult.ok(Map.of("revoked", true));
    }

    /**
     * 为用户重新颁发 Token。
     *
     * @param userId  用户主键
     * @param request Token 重新颁发请求
     * @return 只在本次响应展示明文的 Token 视图
     */
    @PostMapping(value = "/users/{userId}/tokens/reissue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<IssuedTokenView> reissueToken(
            @PathVariable Long userId,
            @RequestBody ReissueTokenRequest request) {
        IssuedTokenView issuedToken = accessManagementService.reissueToken(
                new IssueTokenCommand(userId, request.expiresInDays()));
        return ApiResult.ok(issuedToken);
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
