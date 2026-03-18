package com.gateway.policy;

import com.sun.net.httpserver.Headers;

/**
 * 모든 응답에 공통 보안 헤더를 적용합니다.
 *
 * <p>기본 정책은 컨텐츠 타입 스니핑 방지, 프레임 임베딩 방지,
 * referrer 최소화, 캐시 금지입니다.</p>
 */
public final class SecurityHeadersPolicy {
    /**
     * 공통 보안 헤더를 응답에 씁니다.
     *
     * @param headers 응답 헤더 객체
     */
    public void apply(Headers headers) {
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Cache-Control", "no-store");
    }
}
