package com.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JWT payload 에서 사용자 식별 claim 을 추출합니다.
 *
 * <p>이 클래스는 서명 검증을 수행하지 않습니다. 게이트웨이의 실제 토큰 신뢰 판단은
 * 별도 인증 계층 또는 향후 검증 로직으로 보완되어야 합니다.</p>
 */
public final class JwtUserContextExtractor {
    private final List<String> userIdClaimNames;

    public JwtUserContextExtractor(List<String> userIdClaimNames) {
        this.userIdClaimNames = userIdClaimNames;
    }

    public String extractUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        String[] segments = token.split("\\.", -1);
        if (segments.length != 3) {
            return null;
        }

        String payloadJson = decodePayload(segments[1]);
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }

        for (String claimName : userIdClaimNames) {
            String claimValue = extractClaimValue(payloadJson, claimName);
            if (claimValue != null && !claimValue.isBlank()) {
                return claimValue;
            }
        }
        return null;
    }

    private String decodePayload(String payloadSegment) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payloadSegment);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String extractClaimValue(String payloadJson, String claimName) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(claimName) + "\"\\s*:\\s*(\"([^\"]+)\"|(\\d+))"
        );
        Matcher matcher = pattern.matcher(payloadJson);
        if (!matcher.find()) {
            return null;
        }
        String stringValue = matcher.group(2);
        return stringValue != null ? stringValue : matcher.group(3);
    }
}
