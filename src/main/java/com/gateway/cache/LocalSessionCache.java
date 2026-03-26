package com.gateway.cache;

import com.gateway.auth.AuthResult;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 단일 인스턴스에서 짧은 TTL로 세션 검증 결과를 보관하는 L1 캐시입니다.
 */
public final class LocalSessionCache {
    private final long ttlNanos;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public LocalSessionCache(int ttlSeconds) {
        this.ttlNanos = Math.max(0, ttlSeconds) * 1_000_000_000L;
    }

    public AuthResult get(String tokenKey) {
        if (ttlNanos <= 0) {
            return null;
        }
        CacheEntry entry = cache.get(tokenKey);
        if (entry == null) {
            return null;
        }
        if (System.nanoTime() >= entry.expiresAt) {
            cache.remove(tokenKey, entry);
            return null;
        }
        return entry.authResult;
    }

    public void put(String tokenKey, AuthResult authResult) {
        Objects.requireNonNull(tokenKey, "tokenKey");
        Objects.requireNonNull(authResult, "authResult");
        if (ttlNanos <= 0) {
            return;
        }
        cache.put(tokenKey, new CacheEntry(authResult, System.nanoTime() + ttlNanos));
    }

    private static final class CacheEntry {
        private final AuthResult authResult;
        private final long expiresAt;

        private CacheEntry(AuthResult authResult, long expiresAt) {
            this.authResult = authResult;
            this.expiresAt = expiresAt;
        }
    }
}
