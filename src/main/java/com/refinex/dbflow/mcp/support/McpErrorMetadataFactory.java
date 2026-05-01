package com.refinex.dbflow.mcp.support;

import com.refinex.dbflow.common.util.SensitiveTextSanitizer;
import com.refinex.dbflow.common.util.TextUtils;
import com.refinex.dbflow.executor.dto.SqlExecutionResult;

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
}
