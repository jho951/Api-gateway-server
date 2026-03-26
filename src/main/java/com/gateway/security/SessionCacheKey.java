package com.gateway.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 내부 캐시 키로 사용할 SHA-256 해시를 생성합니다.
 */
public final class SessionCacheKey {
    private SessionCacheKey() {}

    public static String fromToken(String token) {
        if (token == null) {
            token = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for session cache keys", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int hi = (b >> 4) & 0xF;
            int lo = b & 0xF;
            builder.append(Character.forDigit(hi, 16));
            builder.append(Character.forDigit(lo, 16));
        }
        return builder.toString();
    }
}
