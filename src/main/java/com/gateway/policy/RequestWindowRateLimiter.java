package com.gateway.policy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 간단한 고정 윈도우 기반 인메모리 rate limiter 입니다.
 */
public final class RequestWindowRateLimiter {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public RequestWindowRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.startedAt + windowMillis) {
                return new Window(now, new AtomicInteger(1));
            }
            current.counter.incrementAndGet();
            return current;
        });
        return window.counter.get() <= maxRequests;
    }

    private static final class Window {
        private final long startedAt;
        private final AtomicInteger counter;

        private Window(long startedAt, AtomicInteger counter) {
            this.startedAt = startedAt;
            this.counter = counter;
        }
    }
}
