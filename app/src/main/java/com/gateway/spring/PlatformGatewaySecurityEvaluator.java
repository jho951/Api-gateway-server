package com.gateway.spring;

import io.github.jho951.platform.security.api.SecurityContext;
import io.github.jho951.platform.security.api.SecurityEvaluationResult;
import io.github.jho951.platform.security.api.SecurityRequest;
import io.github.jho951.platform.security.hybrid.HybridSecurityRuntime;

public final class PlatformGatewaySecurityEvaluator implements GatewaySecurityEvaluator {
    private final HybridSecurityRuntime delegate;

    public PlatformGatewaySecurityEvaluator(HybridSecurityRuntime delegate) {
        this.delegate = delegate;
    }

    @Override
    public SecurityRequest withResolvedBoundary(SecurityRequest request) {
        return delegate.withResolvedBoundary(request);
    }

    @Override
    public SecurityEvaluationResult evaluateResult(SecurityRequest request, SecurityContext context) {
        return delegate.evaluateResult(request, context);
    }
}
