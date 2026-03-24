package com.gateway.policy;

import com.sun.net.httpserver.Headers;

import java.util.List;

/**
 * CORS 응답 정책을 적용하는 컴포넌트입니다.
 *
 * <p>현재 정책은 단순 allow-list 방식이며, 프리플라이트 응답에 필요한 최소
 * 헤더를 설정합니다.</p>
 */
public final class CorsPolicy {
    private final List<String> allowedOrigins;

    /**
     * @param allowedOrigins 허용 Origin 목록. {@code *} 는 전체 허용을 의미합니다
     */
    public CorsPolicy(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * 요청 Origin 에 따라 응답 헤더를 설정합니다.
     *
     * @param requestOrigin 요청 헤더의 Origin 값
     * @param responseHeaders 응답 헤더 객체
     */
    public void apply(String requestOrigin, Headers responseHeaders) {
        String allowOrigin = resolveOrigin(requestOrigin);
        if (allowOrigin == null) {
            return;
        }

        responseHeaders.set("Access-Control-Allow-Origin", allowOrigin);
        responseHeaders.set("Vary", "Origin");
        responseHeaders.set("Access-Control-Allow-Credentials", "true");
        responseHeaders.set(
                "Access-Control-Allow-Headers",
                "Authorization, Content-Type, X-Request-Id"
        );
        responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        responseHeaders.set("Access-Control-Max-Age", "600");
    }

    /**
     * @param requestOrigin 검사할 Origin
     * @return 현재 정책상 허용되는 Origin 이면 {@code true}
     */
    public boolean isOriginAllowed(String requestOrigin) {
        return resolveOrigin(requestOrigin) != null;
    }

    private String resolveOrigin(String requestOrigin) {
        if (allowedOrigins.contains("*")) {
            return requestOrigin == null || requestOrigin.isBlank() ? null : requestOrigin;
        }
        if (requestOrigin == null || requestOrigin.isBlank()) {
            return null;
        }
        return allowedOrigins.contains(requestOrigin) ? requestOrigin : null;
    }
}
