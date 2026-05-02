package com.refinex.dbflow.capacity.model;

import com.refinex.dbflow.capacity.support.CapacityPermit;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 容量治理决策结果，表达请求是否允许继续、是否降级以及拒绝原因。
 *
 * @param allowed          是否允许继续执行受保护操作
 * @param degraded         是否已经执行服务端降级
 * @param status           容量决策状态
 * @param reasonCode       容量决策原因码
 * @param retryAfter       建议客户端等待多久后重试，可为空
 * @param notices          面向客户端的容量或降级提示
 * @param permit           已获取的容量 permit，调用方必须在受保护操作结束后关闭
 * @param maxItemsOverride 重型只读降级后的最大条目数覆盖值，可为空
 * @author refinex
 */
public record CapacityDecision(
        boolean allowed,
        boolean degraded,
        CapacityStatus status,
        CapacityReasonCode reasonCode,
        Duration retryAfter,
        List<String> notices,
        CapacityPermit permit,
        Integer maxItemsOverride
) {

    /**
     * 创建容量决策。
     */
    public CapacityDecision {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reasonCode, "reasonCode");
        notices = notices == null ? List.of() : List.copyOf(notices);
        permit = permit == null ? CapacityPermit.none() : permit;
    }

    /**
     * 创建允许继续执行的决策。
     *
     * @return 允许决策
     */
    public static CapacityDecision allow() {
        return allow(CapacityPermit.none());
    }

    /**
     * 创建带 permit 的允许继续执行决策。
     *
     * @param permit 已获取的容量 permit
     * @return 允许决策
     */
    public static CapacityDecision allow(CapacityPermit permit) {
        return new CapacityDecision(true, false, CapacityStatus.ALLOWED, CapacityReasonCode.ALLOWED, null, List.of(),
                permit, null);
    }

    /**
     * 创建容量治理关闭时的允许决策。
     *
     * @return 容量治理关闭决策
     */
    public static CapacityDecision disabled() {
        return new CapacityDecision(true, false, CapacityStatus.ALLOWED, CapacityReasonCode.CAPACITY_DISABLED, null,
                List.of(), CapacityPermit.none(), null);
    }

    /**
     * 创建降级后允许继续执行的决策。
     *
     * @param reasonCode 降级原因码
     * @param notices    降级提示
     * @return 降级决策
     */
    public static CapacityDecision degraded(CapacityReasonCode reasonCode, List<String> notices) {
        return degraded(reasonCode, notices, CapacityPermit.none(), null);
    }

    /**
     * 创建带 permit 和条目数覆盖的降级决策。
     *
     * @param reasonCode       降级原因码
     * @param notices          降级提示
     * @param permit           已获取的容量 permit
     * @param maxItemsOverride 降级后的最大条目数覆盖值
     * @return 降级决策
     */
    public static CapacityDecision degraded(
            CapacityReasonCode reasonCode,
            List<String> notices,
            CapacityPermit permit,
            Integer maxItemsOverride
    ) {
        return new CapacityDecision(true, true, CapacityStatus.DEGRADED, reasonCode, null, notices, permit,
                maxItemsOverride);
    }

    /**
     * 创建拒绝执行的决策。
     *
     * @param reasonCode 拒绝原因码
     * @param retryAfter 建议重试等待时间
     * @return 拒绝决策
     */
    public static CapacityDecision rejected(CapacityReasonCode reasonCode, Duration retryAfter) {
        return new CapacityDecision(false, false, CapacityStatus.REJECTED, reasonCode, retryAfter, List.of(),
                CapacityPermit.none(), null);
    }
}
