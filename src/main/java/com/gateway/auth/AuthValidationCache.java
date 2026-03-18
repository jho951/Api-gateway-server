package com.gateway.auth;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 일반 보호 경로용 짧은 TTL 인증 캐시입니다. */
public final class AuthValidationCache {
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public AuthValidationCache(Duration ttl) {
        this.ttlMillis = ttl.toMillis();
    }

    public AuthResult get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(key);
            return null;
        }
        return entry.result;
    }

    public void put(String key, AuthResult result) {
        store.put(key, new Entry(result, System.currentTimeMillis() + ttlMillis));
    }

    private static final class Entry {
        private final AuthResult result;
        private final long expiresAt;

        private Entry(AuthResult result, long expiresAt) {
            this.result = result;
            this.expiresAt = expiresAt;
        }
    }
}
