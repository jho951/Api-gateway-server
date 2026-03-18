package com.gateway.routing;

import java.net.URI;
import java.util.Comparator;

/**
 * 게이트웨이의 정적 라우팅 규칙입니다.
 *
 * @param pathPattern 매칭할 경로 패턴. {@code /foo/**} 또는 exact path
 * @param routeType 공개/보호/관리자/내부 경로 구분
 * @param upstreamName 로깅과 운영 식별용 업스트림 이름
 * @param targetBaseUri 업스트림 서비스 기본 URI
 */
public record RouteDefinition(
        String pathPattern,
        RouteType routeType,
        String upstreamName,
        URI targetBaseUri
) {
    public static final Comparator<RouteDefinition> MOST_SPECIFIC_FIRST =
            Comparator.comparingInt((RouteDefinition route) -> route.pathPattern().length()).reversed();

    /**
     * 요청 경로가 현재 규칙과 일치하는지 검사합니다.
     *
     * @param requestPath 요청 경로
     * @return 일치하면 {@code true}
     */
    public boolean matches(String requestPath) {
        if (pathPattern.endsWith("/**")) {
            String prefix = pathPattern.substring(0, pathPattern.length() - 3);
            return requestPath.equals(prefix) || requestPath.startsWith(prefix + "/");
        }
        return requestPath.equals(pathPattern);
    }
}
