package com.gateway.proxy;

import java.util.List;
import java.util.Map;

/**
 * 업스트림 서비스로부터 받은 응답 모델입니다.
 */
public final class ProxyResponse {
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final byte[] body;

    /**
     * @param statusCode 업스트림 HTTP 상태 코드
     * @param headers 전달 가능한 응답 헤더
     * @param body 응답 바디
     */
    public ProxyResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    /** @return 업스트림 HTTP 상태 코드 */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return 전달 가능한 응답 헤더 */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /** @return 응답 바디 */
    public byte[] getBody() {
        return body;
    }
}
