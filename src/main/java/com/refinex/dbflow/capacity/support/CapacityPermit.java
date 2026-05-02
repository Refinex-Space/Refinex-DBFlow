package com.refinex.dbflow.capacity.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 容量治理 permit 聚合对象，负责在受保护操作结束后释放已获取的并发舱壁。
 *
 * @author refinex
 */
public class CapacityPermit implements AutoCloseable {

    /**
     * 空 permit，不持有任何资源。
     */
    private static final CapacityPermit NONE = new CapacityPermit(List.of());

    /**
     * permit 释放动作，按获取顺序保存。
     */
    private final List<Runnable> releasers;

    /**
     * 是否已经释放，避免异常路径重复释放。
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 创建容量 permit。
     *
     * @param releasers permit 释放动作
     */
    private CapacityPermit(List<Runnable> releasers) {
        this.releasers = List.copyOf(releasers);
    }

    /**
     * 返回不持有资源的空 permit。
     *
     * @return 空 permit
     */
    public static CapacityPermit none() {
        return NONE;
    }

    /**
     * 创建持有一组释放动作的 permit。
     *
     * @param releasers permit 释放动作
     * @return 容量 permit
     */
    public static CapacityPermit of(List<Runnable> releasers) {
        if (releasers == null || releasers.isEmpty()) {
            return none();
        }
        return new CapacityPermit(releasersCopy(releasers));
    }

    /**
     * 复制释放动作列表，避免调用方后续修改。
     *
     * @param releasers 原始释放动作
     * @return 复制后的释放动作
     */
    private static List<Runnable> releasersCopy(List<Runnable> releasers) {
        return new ArrayList<>(releasers);
    }

    /**
     * 释放所有已持有 permit，按获取顺序的反向释放。
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        List<Runnable> reversed = new ArrayList<>(releasers);
        Collections.reverse(reversed);
        for (Runnable releaser : reversed) {
            releaser.run();
        }
    }
}
