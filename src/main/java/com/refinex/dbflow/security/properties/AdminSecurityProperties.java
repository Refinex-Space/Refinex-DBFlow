package com.refinex.dbflow.security.properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 管理端安全配置属性，承载初始化管理员账号的外部配置。
 *
 * @author refinex
 */
@Validated
@ConfigurationProperties(prefix = "dbflow.admin.initial-user")
public class AdminSecurityProperties implements InitializingBean {

    /**
     * 是否启用初始化管理员账号。
     */
    private boolean enabled;

    /**
     * 初始化管理员用户名。
     */
    private String username;

    /**
     * 初始化管理员展示名称。
     */
    private String displayName = "DBFlow Administrator";

    /**
     * 初始化管理员明文密码，仅允许来自环境变量或本地 profile。
     */
    private String password;

    /**
     * 初始化管理员 BCrypt 密码 hash，适合由外部密钥系统提供。
     */
    private String passwordHash;

    /**
     * 判断字符串是否为空白。
     *
     * @param value 待判断字符串
     * @return 空白时返回 true
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 返回是否启用初始化管理员账号。
     *
     * @return 启用时返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用初始化管理员账号。
     *
     * @param enabled 是否启用初始化管理员账号
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回初始化管理员用户名。
     *
     * @return 初始化管理员用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置初始化管理员用户名。
     *
     * @param username 初始化管理员用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 返回初始化管理员展示名称。
     *
     * @return 初始化管理员展示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置初始化管理员展示名称。
     *
     * @param displayName 初始化管理员展示名称
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 返回初始化管理员明文密码。
     *
     * @return 初始化管理员明文密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置初始化管理员明文密码。
     *
     * @param password 初始化管理员明文密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 返回初始化管理员 BCrypt 密码 hash。
     *
     * @return 初始化管理员 BCrypt 密码 hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置初始化管理员 BCrypt 密码 hash。
     *
     * @param passwordHash 初始化管理员 BCrypt 密码 hash
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 绑定完成后校验初始化账号配置完整性。
     */
    @Override
    public void afterPropertiesSet() {
        if (!enabled) {
            return;
        }
        if (isBlank(username)) {
            throw new IllegalStateException("dbflow.admin.initial-user.username 不能为空");
        }
        if (isBlank(password) && isBlank(passwordHash)) {
            throw new IllegalStateException("dbflow.admin.initial-user.password 或 password-hash 必须配置其一");
        }
    }
}
