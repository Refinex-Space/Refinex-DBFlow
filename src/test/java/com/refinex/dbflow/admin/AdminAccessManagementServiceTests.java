package com.refinex.dbflow.admin;

import com.refinex.dbflow.access.repository.DbfApiTokenRepository;
import com.refinex.dbflow.access.repository.DbfUserEnvGrantRepository;
import com.refinex.dbflow.access.repository.DbfUserRepository;
import com.refinex.dbflow.admin.command.CreateUserCommand;
import com.refinex.dbflow.admin.command.GrantEnvironmentCommand;
import com.refinex.dbflow.admin.command.IssueTokenCommand;
import com.refinex.dbflow.admin.service.AdminAccessManagementService;
import com.refinex.dbflow.admin.view.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理端访问管理服务测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_access_management_service_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "dbflow.security.mcp-token.pepper=admin-service-test-pepper",
        "dbflow.datasource-defaults.driver-class-name=org.h2.Driver",
        "dbflow.projects[0].key=billing-core",
        "dbflow.projects[0].name=Billing Core",
        "dbflow.projects[0].environments[0].key=staging",
        "dbflow.projects[0].environments[0].name=Staging",
        "dbflow.projects[0].environments[0].jdbc-url=jdbc:h2:mem:target_admin_service_staging;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class AdminAccessManagementServiceTests {

    /**
     * 管理端访问管理服务。
     */
    @Autowired
    private AdminAccessManagementService managementService;

    /**
     * 用户 repository。
     */
    @Autowired
    private DbfUserRepository userRepository;

    /**
     * Token repository。
     */
    @Autowired
    private DbfApiTokenRepository tokenRepository;

    /**
     * 授权 repository。
     */
    @Autowired
    private DbfUserEnvGrantRepository grantRepository;

    /**
     * 验证用户创建、禁用、授权、撤销、Token 颁发、吊销和重新颁发闭环。
     */
    @Test
    void shouldManageUsersTokensAndEnvironmentGrants() {
        UserRow user = managementService.createUser(
                new CreateUserCommand("ops.alice", "Ops Alice", "Admin123456!")
        );

        GrantRow grant = managementService.grantEnvironment(
                new GrantEnvironmentCommand(
                        user.id(),
                        "billing-core",
                        "staging",
                        "WRITE"
                )
        );
        IssuedTokenView issuedToken = managementService.issueToken(
                new IssueTokenCommand(user.id(), 7)
        );

        assertThat(userRepository.findByUsername("ops.alice")).isPresent();
        assertThat(grant.projectKey()).isEqualTo("billing-core");
        assertThat(grant.environmentKey()).isEqualTo("staging");
        assertThat(issuedToken.plaintextToken()).startsWith("dbf_");
        assertThat(tokenRepository.findById(issuedToken.tokenId()).orElseThrow().getTokenHash())
                .isNotEqualTo(issuedToken.plaintextToken());

        managementService.revokeGrant(grant.id());
        managementService.revokeToken(issuedToken.tokenId());
        IssuedTokenView reissuedToken = managementService.reissueToken(
                new IssueTokenCommand(user.id(), 14)
        );
        managementService.disableUser(user.id());

        List<TokenRow> tokens = managementService.listTokens(
                new TokenFilter(null, null)
        );
        assertThat(reissuedToken.plaintextToken()).startsWith("dbf_").isNotEqualTo(issuedToken.plaintextToken());
        assertThat(tokens).extracting(TokenRow::tokenHash).containsOnlyNulls();
        assertThat(grantRepository.findById(grant.id())).isEmpty();
        assertThat(userRepository.findByUsername("ops.alice").orElseThrow().getStatus()).isEqualTo("DISABLED");
    }
}
