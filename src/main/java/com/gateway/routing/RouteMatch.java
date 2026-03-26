package com.gateway.routing;

import java.net.URI;

/**
 * 특정 요청 경로를 해석
 *
 * @param route 매칭된 라우트 정의
 * @param targetUri 실제 프록시 대상이 되는 최종 URI
 */
public record RouteMatch(RouteDefinition route, URI targetUri) {
}
