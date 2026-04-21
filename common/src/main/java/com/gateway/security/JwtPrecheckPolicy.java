package com.gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JWT 신뢰 판단이 아니라 게이트웨이에서 수행하는 얕은 선검증입니다.
 * 형식이 명백히 잘못된 토큰과 선택적 exp 만 확인하고, 서명/권한/세션 판단은 downstream 서비스에 맡깁니다.
 */
public final class JwtPrecheckPolicy {
    private static final Pattern JWT_SEGMENT = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern EXP_FIELD = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final boolean expCheckEnabled;
    private final long expClockSkewSeconds;
    private final int maxTokenLength;

    public JwtPrecheckPolicy(boolean expCheckEnabled, long expClockSkewSeconds, int maxTokenLength) {
        this.expCheckEnabled = expCheckEnabled;
        this.expClockSkewSeconds = Math.max(0L, expClockSkewSeconds);
        this.maxTokenLength = Math.max(512, maxTokenLength);
    }

    public Result precheck(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Result.rejected("MISSING_AUTH_HEADER");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            return Result.rejected("INVALID_AUTH_SCHEME");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return Result.rejected("EMPTY_BEARER_TOKEN");
        }
        if (token.length() > maxTokenLength) {
            return Result.rejected("TOKEN_TOO_LONG");
        }

        String[] segments = token.split("\\.", -1);
        if (segments.length != 3) {
            return Result.rejected("INVALID_JWT_PARTS");
        }
        for (String segment : segments) {
            if (segment.isBlank() || !JWT_SEGMENT.matcher(segment).matches()) {
                return Result.rejected("INVALID_JWT_SEGMENT");
            }
        }

        String payloadJson = decodePayload(segments[1]);
        if (payloadJson == null || payloadJson.isBlank()) {
            return Result.rejected("INVALID_JWT_PAYLOAD");
        }

        if (!expCheckEnabled) {
            return Result.accepted("PRECHECK_PASSED");
        }

        Long exp = extractExp(payloadJson);
        if (exp == null) {
            return Result.accepted("PRECHECK_PASSED");
        }

        long nowEpochSeconds = Instant.now().getEpochSecond();
        if (nowEpochSeconds > exp + expClockSkewSeconds) {
            return Result.rejected("TOKEN_EXPIRED_PRECHECK");
        }
        return Result.accepted("PRECHECK_PASSED");
    }

    private String decodePayload(String payloadSegment) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payloadSegment);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Long extractExp(String payloadJson) {
        Matcher matcher = EXP_FIELD.matcher(payloadJson);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record Result(boolean accepted, String outcome) {
        public static Result accepted(String outcome) {
            return new Result(true, outcome);
        }

        public static Result rejected(String outcome) {
            return new Result(false, outcome);
        }
    }
}
