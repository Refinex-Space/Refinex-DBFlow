package com.refinex.dbflow.mcp.support;

import com.refinex.dbflow.capacity.model.CapacityDecision;
import com.refinex.dbflow.common.util.SensitiveTextSanitizer;
import com.refinex.dbflow.common.util.TextUtils;
import com.refinex.dbflow.executor.dto.SqlExecutionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.refinex.dbflow.mcp.support.McpResponseBuilder.data;

/**
 * MCP 错误和提示元数据工厂，统一脱敏与稳定错误码映射。
 *
 * @author refinex
 */
public final class McpErrorMetadataFactory {

    /**
     * 工具类不允许实例化。
     */
    private McpErrorMetadataFactory() {
    }

    /**
     * 创建统一错误数据。
     *
     * @param code    错误码
     * @param message 错误摘要
     * @return 错误数据；无错误码时返回空 Map
     */
    public static Map<String, Object> errorData(String code, String message) {
        if (!TextUtils.hasText(code)) {
            return Collections.emptyMap();
        }
        return data(
                "code", sanitize(code),
                "message", sanitize(message),
                "stackTraceIncluded", false
        );
    }

    /**
     * 创建 SQL 执行结果错误数据。
     *
     * @param result SQL 执行结果
     * @return 错误数据；成功或确认挑战时返回空 Map
     */
    public static Map<String, Object> executionError(SqlExecutionResult result) {
        if ("DENIED".equalsIgnoreCase(result.status())) {
            return errorData("POLICY_DENIED", result.statementSummary());
        }
        if ("FAILED".equalsIgnoreCase(result.status())) {
            return errorData("SQL_EXECUTION_FAILED", result.statementSummary());
        }
        return Collections.emptyMap();
    }

    /**
     * 创建容量治理错误数据。
     *
     * @param decision 容量治理决策
     * @return 错误数据；容量允许时返回空 Map
     */
    public static Map<String, Object> capacityError(CapacityDecision decision) {
        if (decision == null || decision.allowed()) {
            return Collections.emptyMap();
        }
        return data(
                "code", "CAPACITY_REJECTED",
                "message", capacityMessage(decision),
                "reasonCode", decision.reasonCode().name(),
                "retryAfterMillis", retryAfterMillis(decision),
                "stackTraceIncluded", false
        );
    }

    /**
     * 创建容量治理元数据。
     *
     * @param decision 容量治理决策
     * @return 容量治理元数据
     */
    public static Map<String, Object> capacityData(CapacityDecision decision) {
        if (decision == null) {
            return Collections.emptyMap();
        }
        return data(
                "status", decision.status().name(),
                "reasonCode", decision.reasonCode().name(),
                "degraded", decision.degraded(),
                "retryAfterMillis", retryAfterMillis(decision),
                "maxItemsOverride", decision.maxItemsOverride()
        );
    }

    /**
     * 创建结果提示列表。
     *
     * @param truncated 结果是否截断
     * @return 提示列表
     */
    public static List<Map<String, Object>> notices(boolean truncated) {
        if (!truncated) {
            return List.of();
        }
        return List.of(data(
                "code", "RESULT_TRUNCATED",
                "message", "结果已按服务端上限截断，未返回完整结果集"
        ));
    }

    /**
     * 创建包含容量治理提示的结果提示列表。
     *
     * @param truncated 结果是否截断
     * @param decision  容量治理决策
     * @return 提示列表
     */
    public static List<Map<String, Object>> notices(boolean truncated, CapacityDecision decision) {
        List<Map<String, Object>> values = new ArrayList<>(notices(truncated));
        if (decision == null) {
            return List.copyOf(values);
        }
        for (String notice : decision.notices()) {
            values.add(data(
                    "code", decision.reasonCode().name(),
                    "message", sanitize(notice)
            ));
        }
        return List.copyOf(values);
    }

    /**
     * 从确认异常消息映射稳定错误码。
     *
     * @param message 异常消息
     * @return 稳定错误码
     */
    public static String confirmationErrorCode(String message) {
        if (TextUtils.hasText(message) && message.contains("过期")) {
            return "CONFIRMATION_EXPIRED";
        }
        return "CONFIRMATION_DENIED";
    }

    /**
     * 对 MCP 客户端展示文本做基础脱敏。
     *
     * @param value 原始文本
     * @return 脱敏文本
     */
    public static String sanitize(String value) {
        return SensitiveTextSanitizer.sanitize(value);
    }

    /**
     * 创建容量拒绝摘要。
     *
     * @param decision 容量治理决策
     * @return 拒绝摘要
     */
    private static String capacityMessage(CapacityDecision decision) {
        return switch (decision.reasonCode()) {
            case LOCAL_PRESSURE -> "DBFlow 单实例处于本地压力态，请稍后重试";
            case TARGET_PRESSURE -> "目标库连接池处于压力态，请稍后重试";
            case TOKEN_RATE_LIMITED -> "当前 Token 调用频率超过服务端上限";
            case USER_RATE_LIMITED -> "当前用户调用频率超过服务端上限";
            case TOOL_RATE_LIMITED -> "当前 MCP 能力调用频率超过服务端上限";
            case TARGET_RATE_LIMITED -> "当前目标项目环境调用频率超过服务端上限";
            case GLOBAL_BULKHEAD_FULL -> "DBFlow 单实例全局并发已满";
            case TOOL_BULKHEAD_FULL -> "当前 MCP 能力并发已满";
            case TOKEN_BULKHEAD_FULL -> "当前 Token 并发已满";
            case USER_BULKHEAD_FULL -> "当前用户并发已满";
            case TARGET_BULKHEAD_FULL -> "当前目标项目环境并发已满";
            default -> "DBFlow 容量治理拒绝本次请求";
        };
    }

    /**
     * 转换建议重试等待时间。
     *
     * @param decision 容量治理决策
     * @return 建议等待毫秒数
     */
    private static Long retryAfterMillis(CapacityDecision decision) {
        if (decision.retryAfter() == null) {
            return null;
        }
        return decision.retryAfter().toMillis();
    }
}
