package com.gateway.security;

import io.github.jho951.platform.security.api.SecurityRequest;
import io.github.jho951.platform.security.ip.PlatformIpGuardFacade;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * platform-security IP 정책을 감싸는 게이트웨이용 IP 허용 정책 래퍼입니다.
 *
 * <p>규칙 문법 해석과 판정은 platform-security에 위임합니다. 게이트웨이는
 * 규칙 원본을 조합하고 활성화 여부만 관리합니다.</p>
 */
public final class IpGuardPolicy {
    private final boolean enabled;
    private final PlatformIpGuardFacade ipGuard;

    /**
     * @param enabled 정책 활성화 여부
     * @param rules platform-security가 ip-guard 엔진으로 해석할 raw 규칙 목록
     * @param defaultAllow 규칙 미일치 시 기본 허용 여부
     */
    public IpGuardPolicy(boolean enabled, List<String> rules, boolean defaultAllow) {
        this.enabled = enabled;
        if (!enabled) {
            this.ipGuard = null;
            return;
        }

        this.ipGuard = PlatformIpGuardFacade.fromRules(rules, defaultAllow);
    }

    /**
     * 클라이언트 IP가 현재 정책상 허용되는지 검사합니다.
     *
     * @param clientIp 검사할 원격 IP 문자열
     * @return 허용되면 {@code true}
     */
    public boolean allows(String clientIp) {
        if (!enabled) return true;
        if (clientIp == null || clientIp.isBlank()) return false;

        SecurityRequest request = new SecurityRequest(
                null,
                clientIp,
                "/",
                "IP_GUARD",
                Map.of(),
                Instant.now()
        );
        return ipGuard.evaluate(request).allowed();
    }
}
