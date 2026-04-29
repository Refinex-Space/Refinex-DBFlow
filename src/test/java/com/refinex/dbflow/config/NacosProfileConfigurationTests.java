package com.refinex.dbflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nacos profile 配置资源加载测试。
 *
 * @author refinex
 */
class NacosProfileConfigurationTests {

    /**
     * YAML 配置加载器。
     */
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    /**
     * 验证默认本地配置显式关闭 Nacos Config 与 Discovery。
     *
     * @throws IOException 读取配置资源失败时抛出
     */
    @Test
    void shouldDisableNacosByDefaultForLocalStartup() throws IOException {
        List<PropertySource<?>> propertySources = loadYaml("application.yml");

        assertThat(findProperty(propertySources, "spring.cloud.nacos.config.enabled")).isEqualTo(false);
        assertThat(findProperty(propertySources, "spring.cloud.nacos.discovery.enabled")).isEqualTo(false);
        assertThat(findProperty(propertySources, "spring.cloud.service-registry.auto-registration.enabled"))
                .isEqualTo(false);
    }

    /**
     * 验证 nacos profile 提供 Config 与 Discovery 的外部配置入口。
     *
     * @throws IOException 读取配置资源失败时抛出
     */
    @Test
    void shouldDefineNacosProfileImportsAndConnectionPlaceholders() throws IOException {
        List<PropertySource<?>> propertySources = loadYaml("application-nacos.yml");

        assertThat(findProperty(propertySources, "spring.config.import[0]"))
                .isEqualTo("optional:nacos:refinex-dbflow.yml?group=DBFLOW_GROUP&refreshEnabled=true");
        assertThat(findProperty(propertySources, "spring.config.import[1]"))
                .isEqualTo("optional:nacos:refinex-dbflow-${spring.profiles.active}.yml?group=DBFLOW_GROUP&refreshEnabled=true");
        assertThat(findProperty(propertySources, "spring.cloud.nacos.server-addr"))
                .isEqualTo("${DBFLOW_NACOS_SERVER_ADDR:127.0.0.1:8848}");
        assertThat(findProperty(propertySources, "spring.cloud.nacos.config.namespace"))
                .isEqualTo("${DBFLOW_NACOS_NAMESPACE:}");
        assertThat(findProperty(propertySources, "spring.cloud.nacos.discovery.group")).isEqualTo("DBFLOW_GROUP");
    }

    /**
     * 验证 Nacos profile 不提交凭据默认值。
     *
     * @throws IOException 读取配置资源失败时抛出
     */
    @Test
    void shouldNotCommitNacosCredentialDefaults() throws IOException {
        String yaml = new ClassPathResource("application-nacos.yml").getContentAsString(StandardCharsets.UTF_8);

        assertThat(yaml).contains("${DBFLOW_NACOS_USERNAME:}");
        assertThat(yaml).contains("${DBFLOW_NACOS_PASSWORD:}");
        assertThat(yaml).doesNotContain("username: nacos");
        assertThat(yaml).doesNotContain("password: nacos");
    }

    /**
     * 加载 classpath 下的 YAML 配置资源。
     *
     * @param resourceName 资源名称
     * @return 配置属性源列表
     * @throws IOException 读取配置资源失败时抛出
     */
    private List<PropertySource<?>> loadYaml(String resourceName) throws IOException {
        return loader.load(resourceName, new ClassPathResource(resourceName));
    }

    /**
     * 从多个属性源中查找配置值。
     *
     * @param propertySources 属性源列表
     * @param propertyName    配置名称
     * @return 配置值
     */
    private Object findProperty(List<PropertySource<?>> propertySources, String propertyName) {
        return propertySources.stream()
                .map(propertySource -> propertySource.getProperty(propertyName))
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }
}
