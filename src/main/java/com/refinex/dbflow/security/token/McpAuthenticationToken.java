package com.refinex.dbflow.security.token;

import com.refinex.dbflow.security.request.McpRequestMetadata;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.io.Serial;
import java.util.Objects;

/**
 * MCP Bearer Token 认证结果。
 *
 * @author refinex
 */
public class McpAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Token 校验结果。
     */
    private final McpTokenValidationResult validationResult;

    /**
     * 创建已认证 MCP authentication。
     *
     * @param validationResult Token 校验结果
     * @param metadata         MCP 请求元信息
     */
    private McpAuthenticationToken(McpTokenValidationResult validationResult, McpRequestMetadata metadata) {
        super(AuthorityUtils.createAuthorityList("ROLE_MCP_USER"));
        this.validationResult = validationResult;
        setDetails(metadata);
        setAuthenticated(true);
    }

    /**
     * 创建已认证 MCP authentication。
     *
     * @param validationResult Token 校验结果
     * @param metadata         MCP 请求元信息
     * @return 已认证 authentication
     */
    public static McpAuthenticationToken authenticated(
            McpTokenValidationResult validationResult,
            McpRequestMetadata metadata
    ) {
        return new McpAuthenticationToken(validationResult, metadata);
    }

    /**
     * 读取认证凭据；已认证后不保留 Token 明文。
     *
     * @return 空凭据
     */
    @Override
    public Object getCredentials() {
        return "";
    }

    /**
     * 读取认证主体。
     *
     * @return 认证主体
     */
    @Override
    public Object getPrincipal() {
        return "dbflow-user-" + validationResult.userId();
    }

    /**
     * 读取 Token 校验结果。
     *
     * @return Token 校验结果
     */
    public McpTokenValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * 比较认证结果是否相同。
     *
     * @param other 待比较对象
     * @return 相同时返回 true
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof McpAuthenticationToken token)) {
            return false;
        }
        return super.equals(other) && Objects.equals(validationResult, token.validationResult);
    }

    /**
     * 计算认证结果 hash。
     *
     * @return hash 值
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), validationResult);
    }
}
