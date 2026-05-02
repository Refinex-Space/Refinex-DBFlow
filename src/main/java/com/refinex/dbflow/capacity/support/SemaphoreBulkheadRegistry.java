package com.refinex.dbflow.capacity.support;

import com.refinex.dbflow.capacity.model.CapacityReasonCode;
import com.refinex.dbflow.capacity.model.CapacityScope;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 信号量并发舱壁注册表，为全局、工具、Token、用户和目标维度维护独立并发限制。
 *
 * @author refinex
 */
public class SemaphoreBulkheadRegistry {

    /**
     * 并发舱壁 key 到状态的映射。
     */
    private final ConcurrentMap<BulkheadKey, BulkheadState> bulkheads = new ConcurrentHashMap<>();

    /**
     * 按顺序获取一组并发舱壁 permit。
     *
     * @param requests 并发舱壁获取请求
     * @return 获取结果；任一请求失败时会释放已经获取的 permit
     */
    public BulkheadAcquireResult acquireAll(List<BulkheadRequest> requests) {
        List<Runnable> releasers = new ArrayList<>();
        for (BulkheadRequest request : requests) {
            BulkheadAcquireResult result = acquire(request);
            if (!result.acquired()) {
                CapacityPermit.of(releasers).close();
                return result;
            }
            releasers.addAll(result.releasers());
        }
        return BulkheadAcquireResult.acquired(CapacityPermit.of(releasers), releasers);
    }

    /**
     * 获取单个并发舱壁 permit。
     *
     * @param request 并发舱壁获取请求
     * @return 获取结果
     */
    public BulkheadAcquireResult acquire(BulkheadRequest request) {
        Objects.requireNonNull(request, "request");
        BulkheadKey key = new BulkheadKey(request.scope(), request.name());
        BulkheadState state = bulkheads.compute(key, (currentKey, current) -> {
            if (current == null || current.maxConcurrent() != request.maxConcurrent()) {
                return new BulkheadState(request.maxConcurrent());
            }
            return current;
        });
        boolean acquired = tryAcquire(state.semaphore(), request.acquireTimeout());
        if (!acquired) {
            return BulkheadAcquireResult.rejected(request.reasonCode());
        }
        return BulkheadAcquireResult.acquired(CapacityPermit.of(List.of(state.semaphore()::release)),
                List.of(state.semaphore()::release));
    }

    /**
     * 返回指定舱壁剩余 permit 数。
     *
     * @param scope 舱壁范围
     * @param name  舱壁名称
     * @return 剩余 permit 数；舱壁不存在时返回 0
     */
    public int availablePermits(CapacityScope scope, String name) {
        BulkheadState state = bulkheads.get(new BulkheadKey(scope, name));
        return state == null ? 0 : state.semaphore().availablePermits();
    }

    /**
     * 返回指定舱壁已占用 permit 数。
     *
     * @param scope 舱壁范围
     * @param name  舱壁名称
     * @return 已占用 permit 数；舱壁不存在时返回 0
     */
    public int activePermits(CapacityScope scope, String name) {
        BulkheadState state = bulkheads.get(new BulkheadKey(scope, name));
        return state == null ? 0 : state.maxConcurrent() - state.semaphore().availablePermits();
    }

    /**
     * 尝试获取 semaphore permit。
     *
     * @param semaphore      信号量
     * @param acquireTimeout 获取等待时间
     * @return 成功获取时返回 true
     */
    private boolean tryAcquire(Semaphore semaphore, Duration acquireTimeout) {
        try {
            if (acquireTimeout == null || acquireTimeout.isZero()) {
                return semaphore.tryAcquire();
            }
            return semaphore.tryAcquire(acquireTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 并发舱壁获取请求。
     *
     * @param scope          舱壁范围
     * @param name           舱壁名称
     * @param maxConcurrent  最大并发数
     * @param acquireTimeout 获取等待时间
     * @param reasonCode     获取失败时返回的原因码
     */
    public record BulkheadRequest(
            CapacityScope scope,
            String name,
            int maxConcurrent,
            Duration acquireTimeout,
            CapacityReasonCode reasonCode
    ) {

        /**
         * 创建并发舱壁请求。
         */
        public BulkheadRequest {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(reasonCode, "reasonCode");
            if (maxConcurrent <= 0) {
                throw new IllegalArgumentException("maxConcurrent 必须大于 0");
            }
        }
    }

    /**
     * 并发舱壁获取结果。
     *
     * @param acquired   是否成功获取全部请求的 permit
     * @param permit     成功获取时返回的聚合 permit
     * @param reasonCode 失败原因码
     * @param releasers  内部释放动作，仅用于聚合获取
     */
    public record BulkheadAcquireResult(
            boolean acquired,
            CapacityPermit permit,
            CapacityReasonCode reasonCode,
            List<Runnable> releasers
    ) {

        /**
         * 创建获取成功结果。
         *
         * @param permit    聚合 permit
         * @param releasers 释放动作
         * @return 获取成功结果
         */
        private static BulkheadAcquireResult acquired(CapacityPermit permit, List<Runnable> releasers) {
            return new BulkheadAcquireResult(true, permit, CapacityReasonCode.ALLOWED, List.copyOf(releasers));
        }

        /**
         * 创建获取失败结果。
         *
         * @param reasonCode 失败原因码
         * @return 获取失败结果
         */
        private static BulkheadAcquireResult rejected(CapacityReasonCode reasonCode) {
            return new BulkheadAcquireResult(false, CapacityPermit.none(), reasonCode, List.of());
        }
    }

    /**
     * 并发舱壁 key。
     *
     * @param scope 舱壁范围
     * @param name  舱壁名称
     */
    private record BulkheadKey(CapacityScope scope, String name) {
    }

    /**
     * 并发舱壁状态。
     *
     * @param maxConcurrent 最大并发数
     * @param semaphore     信号量
     */
    private record BulkheadState(int maxConcurrent, Semaphore semaphore) {

        /**
         * 创建并发舱壁状态。
         *
         * @param maxConcurrent 最大并发数
         */
        private BulkheadState(int maxConcurrent) {
            this(maxConcurrent, new Semaphore(maxConcurrent, true));
        }
    }
}
