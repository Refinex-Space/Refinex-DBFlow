package com.refinex.dbflow.observability;

import com.refinex.dbflow.audit.repository.DbfConfirmationChallengeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * DBFlow Micrometer 指标写入服务，集中维护指标名称、标签和边界裁剪。
 *
 * @author refinex
 */
@Service
public class DbflowMetricsService {

    /**
     * 待确认挑战状态。
     */
    private static final String PENDING_STATUS = "PENDING";

    /**
     * 最大标签长度，避免异常 SQL 或客户端输入扩大指标基数。
     */
    private static final int MAX_TAG_LENGTH = 96;

    /**
     * Micrometer 指标注册表。
     */
    private final MeterRegistry meterRegistry;

    /**
     * 创建 DBFlow 指标服务。
     *
     * @param meterRegistry                   Micrometer 指标注册表
     * @param confirmationChallengeRepository 确认挑战 repository
     */
    public DbflowMetricsService(
            MeterRegistry meterRegistry,
            DbfConfirmationChallengeRepository confirmationChallengeRepository
    ) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
        Objects.requireNonNull(confirmationChallengeRepository);
        Gauge.builder("dbflow.confirmation.challenges", confirmationChallengeRepository,
                        repository -> repository.countByStatus(PENDING_STATUS))
                .description("DBFlow pending SQL confirmation challenges.")
                .tag("status", PENDING_STATUS)
                .register(meterRegistry);
    }

    /**
     * 记录 MCP 工具调用次数。
     *
     * @param tool MCP 工具名称
     */
    public void recordMcpCall(String tool) {
        Counter.builder("dbflow.mcp.calls")
                .description("DBFlow MCP tool call count.")
                .tag("tool", tag(tool))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 SQL 风险分布。
     *
     * @param tool      MCP 工具名称
     * @param operation SQL 操作类型
     * @param risk      风险级别
     * @param decision  审计决策
     */
    public void recordSqlRisk(String tool, String operation, String risk, String decision) {
        Counter.builder("dbflow.sql.risk")
                .description("DBFlow SQL risk distribution by operation and decision.")
                .tag("tool", tag(tool))
                .tag("operation", tag(operation))
                .tag("risk", tag(risk))
                .tag("decision", tag(decision))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录拒绝次数。
     *
     * @param tool      MCP 工具名称
     * @param operation SQL 操作类型
     * @param risk      风险级别
     * @param decision  审计决策
     */
    public void recordRejection(String tool, String operation, String risk, String decision) {
        Counter.builder("dbflow.sql.rejections")
                .description("DBFlow SQL rejection count.")
                .tag("tool", tag(tool))
                .tag("operation", tag(operation))
                .tag("risk", tag(risk))
                .tag("decision", tag(decision))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 SQL 执行耗时。
     *
     * @param operation    SQL 操作类型
     * @param risk         风险级别
     * @param status       执行状态
     * @param startedNanos 执行开始时间纳秒
     */
    public void recordSqlExecutionDuration(String operation, String risk, String status, long startedNanos) {
        long durationNanos = Math.max(0L, System.nanoTime() - startedNanos);
        Timer.builder("dbflow.sql.execution.duration")
                .description("DBFlow controlled SQL execution duration.")
                .tag("operation", tag(operation))
                .tag("risk", tag(risk))
                .tag("status", tag(status))
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNanos));
    }

    /**
     * 标准化指标标签值。
     *
     * @param value 原始值
     * @return 标签值
     */
    private String tag(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        if (normalized.length() <= MAX_TAG_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_TAG_LENGTH);
    }
}
