package com.refinex.dbflow.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP Token 安全配置属性。
 *
 * @author refinex
 */
@ConfigurationProperties(prefix = "dbflow.security.mcp-token")
public class McpTokenProperties {

    /**
     * Token hash pepper，必须来自环境变量或安全配置。
     */
    private String pepper;

    /**
     * 返回 Token hash pepper。
     *
     * @return Token hash pepper
     */
    public String getPepper() {
        return pepper;
    }

    /**
     * 设置 Token hash pepper。
     *
     * @param pepper Token hash pepper
     */
    public void setPepper(String pepper) {
        this.pepper = pepper;
    }
}
