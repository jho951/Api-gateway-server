package com.gateway.audit;

public interface GatewayOperationalAuditPort {
    void logRequest(
            String method,
            String path,
            String requestId,
            String clientIp,
            String userId,
            String upstream,
            int statusCode,
            String authOutcome,
            String failureReason
    );
}
