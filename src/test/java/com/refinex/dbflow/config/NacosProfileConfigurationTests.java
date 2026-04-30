package com.refinex.dbflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nacos 默认配置资源加载测试。
 *
 * @author refinex
 */
class NacosProfileConfigurationTests {

    /**
     * YAML 配置加载器。
     */
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    /**
     * 验证默认配置直接启用 Nacos Config 与 Discovery。
     *
     * @throws IOException 读取配置资源失败时抛出
     */
    @Test
    void shouldEnableNacosByDefaultAndImportApplicationDbflowDataId() throws IOException {
        List<PropertySource<?>> propertySources = loadMainYaml("application.yml");

        assertThat(findProperty(propertySources, "spring.config.import[0]"))
                .isEqualTo("optional:nacos:application-dbflow.yml?group=DBFLOW_GROUP&refreshEnabled=true");
        assertThat(findProperty(propertySources, "spring.config.import[1]")).isNull();
        assertThat(findProperty(propertySources, "spring.cloud.nacos.config.enabled")).isEqualTo(true);
        assertThat(findProperty(propertySources, "spring.cloud.nacos.discovery.enabled")).isEqualTo(true);
        assertThat(findProperty(propertySources, "spring.cloud.service-registry.auto-registration.enabled"))
                .isEqualTo(true);
        assertThat(findProperty(propertySources, "spring.cloud.nacos.server-addr"))
                .isEqualTo("${DBFLOW_NACOS_SERVER_ADDR:127.0.0.1:8848}");
        assertThat(findProperty(propertySources, "spring.cloud.nacos.config.namespace"))
                .isEqualTo("${DBFLOW_NACOS_NAMESPACE:}");
        assertThat(findProperty(propertySources, "spring.cloud.nacos.discovery.group")).isEqualTo("DBFLOW_GROUP");
    }

    /**
     * 验证默认应用配置不提交 Nacos 凭据默认值。
     *
     * @throws IOException 读取配置资源失败时抛出
     */
    @Test
    void shouldNotCommitNacosCredentialDefaults() throws IOException {
        String yaml = readMainYaml("application.yml");

        assertThat(yaml).contains("${DBFLOW_NACOS_USERNAME:}");
        assertThat(yaml).contains("${DBFLOW_NACOS_PASSWORD:}");
        assertThat(yaml).doesNotContain("username: nacos");
        assertThat(yaml).doesNotContain("password: nacos");
    }

    /**
     * 验证 jar 内默认配置不再承载运行期 DBFlow 密钥和本地元数据库。
     *
     * @throws IOException 读取配置资源失败时抛出
     */
    @Test
    void shouldNotKeepDbflowRuntimeConfigInApplicationYaml() throws IOException {
        String yaml = readMainYaml("application.yml");

        assertThat(yaml).doesNotContain("\ndbflow:");
        assertThat(yaml).doesNotContain("dev-only-change-me");
        assertThat(yaml).doesNotContain("password: admin");
        assertThat(yaml).doesNotContain("jdbc:h2:mem:dbflow_metadata");
    }

    /**
     * 加载 src/main/resources 下的 YAML 配置资源，避免被 src/test/resources 覆盖。
     *
     * @param resourceName 资源名称
     * @return 配置属性源列表
     * @throws IOException 读取配置资源失败时抛出
     */
    private List<PropertySource<?>> loadMainYaml(String resourceName) throws IOException {
        return loader.load(resourceName,
                new FileSystemResource(Path.of("src", "main", "resources", resourceName)));
    }

    /**
     * 读取 src/main/resources 下的 YAML 文本，避免被 src/test/resources 覆盖。
     *
     * @param resourceName 资源名称
     * @return YAML 文本
     * @throws IOException 读取配置资源失败时抛出
     */
    private String readMainYaml(String resourceName) throws IOException {
        return new FileSystemResource(Path.of("src", "main", "resources", resourceName))
                .getContentAsString(StandardCharsets.UTF_8);
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
