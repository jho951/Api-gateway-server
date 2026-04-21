package com.gateway.spring;

import io.github.jho951.platform.security.api.SecurityEvaluationResult;

public interface GatewaySecurityAuditPort {
    void publish(SecurityEvaluationResult evaluationResult);
}
