package com.gateway.routing;

import java.net.URI;
import java.util.Comparator;

/**
 * 게이트웨이의 정적 라우팅 규칙
 *
 * @param pathPattern 매칭할 경로 패턴. {@code /foo/**} 또는 exact path
 * @param routeType 공개/보호/관리자/내부 경로 구분
 * @param upstreamName 로깅과 운영 식별용 업스트림 이름
 * @param targetBaseUri 업스트림 서비스 기본 URI
 * @param stripPrefix 업스트림에 전달하기 전에 제거할 외부 공통 prefix
 */
public record RouteDefinition(
        String pathPattern,
        RouteType routeType,
        String upstreamName,
        URI targetBaseUri,
        String stripPrefix
) {
    public static final Comparator<RouteDefinition> MOST_SPECIFIC_FIRST =
            Comparator.comparingInt((RouteDefinition route) -> route.pathPattern().length()).reversed();

    public RouteDefinition(String pathPattern, RouteType routeType, String upstreamName, URI targetBaseUri) {
        this(pathPattern, routeType, upstreamName, targetBaseUri, "");
    }

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

    public String rewritePath(String requestPath) {
        if (stripPrefix == null || stripPrefix.isBlank()) {
            return requestPath;
        }
        if (!requestPath.startsWith(stripPrefix)) {
            return requestPath;
        }

        String rewrittenPath = requestPath.substring(stripPrefix.length());
        return rewrittenPath.isBlank() ? "/" : rewrittenPath;
    }
}
