package com.gateway.spring;

import io.github.jho951.platform.security.api.SecurityAuditEvent;
import io.github.jho951.platform.security.api.SecurityAuditPublisher;
import io.github.jho951.platform.security.api.SecurityEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public final class PlatformSecurityAuditAdapter implements GatewaySecurityAuditPort {
    private final SecurityAuditPublisher securityAuditPublisher;

    public PlatformSecurityAuditAdapter(SecurityAuditPublisher securityAuditPublisher) {
        this.securityAuditPublisher = securityAuditPublisher;
    }

    @Override
    public void publish(SecurityEvaluationResult evaluationResult) {
        securityAuditPublisher.publish(SecurityAuditEvent.from(evaluationResult));
    }
}
