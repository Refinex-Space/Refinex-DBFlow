package com.refinex.dbflow.security;

import com.refinex.dbflow.access.entity.DbfUser;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于元数据库用户表的管理端用户详情服务。
 *
 * @author refinex
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    /**
     * 用户 repository。
     */
    private final DbfUserRepository userRepository;

    /**
     * 创建管理端用户详情服务。
     *
     * @param userRepository 用户 repository
     */
    public AdminUserDetailsService(DbfUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 按用户名加载管理端用户。
     *
     * @param username 用户名
     * @return Spring Security 用户详情
     * @throws UsernameNotFoundException 用户不存在、状态不可用或没有密码 hash 时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        DbfUser user = userRepository.findByUsername(username)
                .filter(this::isLoginEnabled)
                .orElseThrow(() -> new UsernameNotFoundException("管理端用户不存在或不可登录"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles("ADMIN")
                .build();
    }

    /**
     * 判断用户是否允许管理端登录。
     *
     * @param user 用户实体
     * @return 允许登录时返回 true
     */
    private boolean isLoginEnabled(DbfUser user) {
        return "ACTIVE".equals(user.getStatus())
                && user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank();
    }
}
