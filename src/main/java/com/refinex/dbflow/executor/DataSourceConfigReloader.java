package com.refinex.dbflow.executor;

import com.refinex.dbflow.config.DbflowProperties;
import com.refinex.dbflow.observability.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 数据源配置重载器，负责候选配置校验、候选连接池预热和注册表原子替换。
 *
 * @author refinex
 */
@Service
public class DataSourceConfigReloader {

    /**
     * 运维日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfigReloader.class);

    /**
     * 目标数据源注册表。
     */
    private final HikariDataSourceRegistry registry;

    /**
     * 创建数据源配置重载器。
     *
     * @param registry 目标数据源注册表
     */
    public DataSourceConfigReloader(HikariDataSourceRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    /**
     * 重载目标数据源配置；候选配置失败时保留旧连接池。
     *
     * @param candidateProperties 候选 DBFlow 配置属性
     * @return 重载结果
     */
    public DataSourceReloadResult reload(DbflowProperties candidateProperties) {
        try (LogContext.Scope ignored = LogContext.ensureCorrelation("config-reload")) {
            LOGGER.info("config.reload.started");
            if (!validateCandidateProperties(candidateProperties)) {
                return DataSourceReloadResult.failure("候选数据源配置校验失败");
            }
            try {
                DataSourceReloadResult result = registry.replaceWithCandidate(candidateProperties);
                LOGGER.info("config.reload.completed status=success targetCount={}", result.targetCount());
                return result;
            } catch (RuntimeException exception) {
                String message = sanitizeReloadFailure(exception);
                LOGGER.warn("config.reload.completed status=failure reason={}", message);
                return DataSourceReloadResult.failure(message);
            }
        }
    }

    /**
     * 校验候选 DBFlow 配置属性。
     *
     * @param candidateProperties 候选 DBFlow 配置属性
     * @return 校验通过时返回 true
     */
    private boolean validateCandidateProperties(DbflowProperties candidateProperties) {
        try {
            Objects.requireNonNull(candidateProperties, "candidateProperties").afterPropertiesSet();
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("config.reload.validation.failed reason={}", exception.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 生成不包含 JDBC URL 和密码的重载失败信息。
     *
     * @param exception 重载异常
     * @return 脱敏失败信息
     */
    private String sanitizeReloadFailure(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "目标数据源配置重载失败";
        }
        if (message.startsWith("目标数据源")) {
            return message;
        }
        return "目标数据源配置重载失败";
    }
}
