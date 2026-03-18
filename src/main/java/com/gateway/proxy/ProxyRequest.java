package com.gateway.proxy;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 업스트림 서비스로 전달할 프록시 요청 모델입니다.
 */
public final class ProxyRequest {
    private final String method;
    private final URI targetUri;
    private final Map<String, List<String>> headers;
    private final byte[] body;
    private final String clientIp;

    /**
     * @param method 원본 HTTP 메서드
     * @param targetUri 라우팅 해석이 끝난 최종 대상 URI
     * @param headers 업스트림으로 전달할 요청 헤더
     * @param body 요청 바디
     * @param clientIp 원본 클라이언트 IP 주소
     */
    public ProxyRequest(
            String method,
            URI targetUri,
            Map<String, List<String>> headers,
            byte[] body,
            String clientIp
    ) {
        this.method = method;
        this.targetUri = targetUri;
        this.headers = headers;
        this.body = body;
        this.clientIp = clientIp;
    }

    /** @return 원본 HTTP 메서드 */
    public String getMethod() {
        return method;
    }

    /** @return 라우팅 해석이 끝난 최종 대상 URI */
    public URI getTargetUri() {
        return targetUri;
    }

    /** @return 업스트림으로 전달할 요청 헤더 */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /** @return 요청 바디 */
    public byte[] getBody() {
        return body;
    }

    /** @return 원본 클라이언트 IP 주소 */
    public String getClientIp() {
        return clientIp;
    }
}
