package com.gateway.cache;

import com.gateway.auth.AuthResult;

import java.io.IOException;

/**
 * Redis에 공통 세션 검증 결과를 저장/조회하는 L2 캐시입니다.
 */
public final class RedisSessionCache {
    private static final char DELIMITER = '\u0001';

    private final boolean enabled;
    private final String host;
    private final int port;
    private final int timeoutMs;
    private final int ttlSeconds;
    private final String keyPrefix;

    public RedisSessionCache(boolean enabled, String host, int port, int timeoutMs, int ttlSeconds, String keyPrefix) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
        this.ttlSeconds = ttlSeconds;
        this.keyPrefix = keyPrefix;
    }

    public boolean enabled() {
        return enabled && ttlSeconds > 0;
    }

    public AuthResult get(String cacheKey) throws IOException {
        if (!enabled()) {
            return null;
        }
        try (RedisConnection connection = new RedisConnection(host, port, timeoutMs)) {
            String raw = connection.get(keyPrefix + cacheKey);
            return decode(raw);
        }
    }

    public void put(String cacheKey, AuthResult authResult) throws IOException {
        if (!enabled() || authResult == null || !authResult.isAuthenticated()) {
            return;
        }
        try (RedisConnection connection = new RedisConnection(host, port, timeoutMs)) {
            connection.setEx(keyPrefix + cacheKey, ttlSeconds, encode(authResult));
        }
    }

    private static String encode(AuthResult authResult) {
        return safe(authResult.getUserId())
                + DELIMITER
                + safe(authResult.getRole())
                + DELIMITER
                + safe(authResult.getSessionId());
    }

    private static AuthResult decode(String payload) {
        if (payload == null) {
            return null;
        }
        String[] parts = payload.split(String.valueOf(DELIMITER), -1);
        if (parts.length != 3) {
            return null;
        }
        String userId = emptyToNull(parts[0]);
        if (userId == null || userId.isBlank()) {
            return null;
        }
        String role = emptyToNull(parts[1]);
        String sessionId = emptyToNull(parts[2]);
        return new AuthResult(200, true, userId, role, sessionId);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
