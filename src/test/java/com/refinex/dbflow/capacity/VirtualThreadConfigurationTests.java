package com.refinex.dbflow.capacity;

import com.refinex.dbflow.capacity.model.CapacityRequest;
import com.refinex.dbflow.capacity.model.CapacityStatus;
import com.refinex.dbflow.capacity.model.McpToolClass;
import com.refinex.dbflow.capacity.properties.CapacityProperties;
import com.refinex.dbflow.capacity.service.CapacityGuardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JDK 21 虚拟线程配置覆盖测试。
 *
 * @author refinex
 */
@SpringBootTest(properties = {
        "spring.threads.virtual.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:virtual_thread_configuration_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration",
        "dbflow.admin.initial-user.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "dbflow.capacity.rate-limit.per-token.max-requests=1"
})
class VirtualThreadConfigurationTests {

    /**
     * Spring 环境属性。
     */
    @Autowired
    private Environment environment;

    /**
     * 容量治理配置。
     */
    @Autowired
    private CapacityProperties capacityProperties;

    /**
     * 容量治理服务。
     */
    @Autowired
    private CapacityGuardService capacityGuardService;

    /**
     * 验证虚拟线程启用时容量治理仍然生效。
     */
    @Test
    void shouldKeepCapacityGuardActiveWhenVirtualThreadsEnabled() {
        assertThat(environment.getProperty("spring.threads.virtual.enabled", Boolean.class)).isTrue();
        assertThat(capacityProperties.isEnabled()).isTrue();

        capacityGuardService.evaluate(request("req-vt-1")).permit().close();

        assertThat(capacityGuardService.evaluate(request("req-vt-2")).status())
                .isEqualTo(CapacityStatus.REJECTED);
    }

    /**
     * 创建容量请求。
     *
     * @param requestId 请求标识
     * @return 容量请求
     */
    private CapacityRequest request(String requestId) {
        return new CapacityRequest(
                requestId,
                1L,
                100L,
                "dbflow_execute_sql",
                McpToolClass.EXECUTE,
                null,
                null
        );
    }
}
