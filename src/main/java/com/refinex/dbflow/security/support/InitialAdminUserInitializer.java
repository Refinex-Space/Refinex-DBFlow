package com.refinex.dbflow.security.support;

import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import com.refinex.dbflow.security.properties.AdminSecurityProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 初始化管理员账号创建器，从外部配置读取账号和密码材料。
 *
 * @author refinex
 */
@Component
public class InitialAdminUserInitializer implements ApplicationRunner {

    /**
     * 管理端安全配置属性。
     */
    private final AdminSecurityProperties properties;

    /**
     * 用户 repository。
     */
    private final DbfUserRepository userRepository;

    /**
     * 密码编码器。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建初始化管理员账号创建器。
     *
     * @param properties      管理端安全配置属性
     * @param userRepository  用户 repository
     * @param passwordEncoder 密码编码器
     */
    public InitialAdminUserInitializer(
            AdminSecurityProperties properties,
            DbfUserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 应用启动后按需创建初始化管理员账号。
     *
     * @param args 应用启动参数
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || userRepository.findByUsername(properties.getUsername()).isPresent()) {
            return;
        }
        userRepository.save(DbfUser.create(
                properties.getUsername(),
                properties.getDisplayName(),
                resolvePasswordHash()
        ));
    }

    /**
     * 解析最终入库的 BCrypt 密码 hash。
     *
     * @return BCrypt 密码 hash
     */
    private String resolvePasswordHash() {
        if (properties.getPasswordHash() != null && !properties.getPasswordHash().isBlank()) {
            return properties.getPasswordHash();
        }
        return passwordEncoder.encode(properties.getPassword());
    }
}
