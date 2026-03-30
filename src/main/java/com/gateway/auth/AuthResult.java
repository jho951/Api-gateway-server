package com.gateway.auth;

import com.gateway.contract.internal.header.ServiceHeaders;
import com.gateway.contract.internal.header.TraceHeaders;

import java.util.List;
import java.util.Map;

/** auth-service의  검증 결과를 내부적으로 담아두는 DTO */
public final class AuthResult {
    private final int statusCode;
    private final boolean authenticated;
    private final String userId;
    private final String role;
    private final String sessionId;

    /**
     * 생성자
     * @param statusCode Auth 서비스 응답 코드
     * @param authenticated 인증 성공 여부
     * @param userId 인증된 사용자 ID
     * @param role 사용자 역할
     * @param sessionId 세션 ID
     */
    public AuthResult(int statusCode, boolean authenticated, String userId, String role, String sessionId) {
        this.statusCode = statusCode;
        this.authenticated = authenticated;
        this.userId = userId;
        this.role = role;
        this.sessionId = sessionId;
    }

    public int getStatusCode() {return statusCode;}
    public boolean isAuthenticated() {return authenticated;}
    public String getUserId() {
        return userId;
    }
    public String getRole() {
        return role;
    }
    public String getSessionId() {return sessionId;}
    public boolean isAdmin() {return "ADMIN".equalsIgnoreCase(role);}

    public Map<String, List<String>> toTrustedHeaders(String requestId, String correlationId) {
        return Map.of(
                ServiceHeaders.Trusted.USER_ID, List.of(userId),
                ServiceHeaders.Trusted.USER_ROLE, List.of(role),
                ServiceHeaders.Trusted.SESSION_ID, List.of(sessionId),
                TraceHeaders.REQUEST_ID, List.of(requestId),
                TraceHeaders.CORRELATION_ID, List.of(correlationId)
        );
    }
}
