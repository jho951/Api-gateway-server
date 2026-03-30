package com.gateway.cache;

import java.io.IOException;

/**
 * 관리자 권한 판정 결과를 Redis에 짧게 저장하는 최소 구현입니다.
 * <p>
 * 외부 Redis 클라이언트 의존성을 추가하지 않기 위해
 * RESP 일부만 직접 사용합니다.
 * </p>
 */
public final class RedisPermissionCache {
    private final boolean enabled;
    private final String host;
    private final int port;
    private final int timeoutMs;
    private final int ttlSeconds;
    private final String keyPrefix;

    /**
     * 생성자
     * @param enabled
     * @param host
     * @param port
     * @param timeoutMs
     * @param ttlSeconds
     * @param keyPrefix
     */
    public RedisPermissionCache(boolean enabled, String host, int port, int timeoutMs, int ttlSeconds, String keyPrefix) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
        this.ttlSeconds = ttlSeconds;
        this.keyPrefix = keyPrefix;
    }

    public Boolean get(String cacheKey) {
        if (!enabled) return null;
        try (RedisConnection connection = new RedisConnection(host, port, timeoutMs)) {
            String value = connection.get(keyPrefix + cacheKey);
            if (value == null) return null;
            return "ALLOW".equalsIgnoreCase(value);
        } catch (IOException ex) {
            return null;
        }
    }

    public void put(String cacheKey, boolean allowed) {
        if (!enabled) return;
        try (RedisConnection connection = new RedisConnection(host, port, timeoutMs)) {
            connection.setEx(keyPrefix + cacheKey, ttlSeconds, allowed ? "ALLOW" : "DENY");
        } catch (IOException ignored) {

        }
    }
}
