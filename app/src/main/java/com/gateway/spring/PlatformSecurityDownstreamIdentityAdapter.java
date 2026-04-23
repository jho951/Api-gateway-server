package com.gateway.spring;

import io.github.jho951.platform.security.api.SecurityEvaluationResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public final class PlatformSecurityDownstreamIdentityAdapter implements GatewayDownstreamIdentityProjector {
    @Override
    public Map<String, String> asAttributes(SecurityEvaluationResult evaluationResult) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Security-Boundary", evaluationResult.evaluationContext().profile().boundaryType());
        headers.put("X-Security-Client-Type", evaluationResult.evaluationContext().profile().clientType());
        headers.put("X-Security-Auth-Mode", evaluationResult.evaluationContext().profile().authMode());
        String principal = evaluationResult.evaluationContext().securityContext().principal();
        if (principal != null && !principal.isBlank()) {
            headers.put("X-Security-Principal", principal);
        }
        headers.put("X-Security-Decision", evaluationResult.verdict().decision().name());
        headers.put("X-Security-Policy", evaluationResult.verdict().policy());
        String reason = evaluationResult.verdict().reason();
        if (reason != null && !reason.isBlank()) {
            headers.put("X-Security-Reason", reason);
        }
        return Map.copyOf(headers);
    }
}
