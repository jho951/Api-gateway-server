package com.gateway.spring;

import com.gateway.auth.AuthResult;
import com.gateway.auth.AuthzServiceClient;
import com.gateway.cache.RedisAuthzCache;
import com.gateway.config.GatewayConfig;
import com.gateway.security.SessionCacheKey;
import io.github.jho951.platform.security.api.SecurityContext;
import io.github.jho951.platform.security.api.SecurityPolicyService;
import io.github.jho951.platform.security.api.SecurityRequest;
import io.github.jho951.platform.security.api.SecurityVerdict;

import java.io.IOException;

public final class GatewayPlatformSecurityPolicyService implements SecurityPolicyService {
    private final SecurityPolicyService delegate;
    private final GatewayConfig config;
    private final AuthzServiceClient authzServiceClient;
    private final RedisAuthzCache authzCache;

    public GatewayPlatformSecurityPolicyService(
            SecurityPolicyService delegate,
            GatewayConfig config,
            AuthzServiceClient authzServiceClient,
            RedisAuthzCache authzCache
    ) {
        this.delegate = delegate;
        this.config = config;
        this.authzServiceClient = authzServiceClient;
        this.authzCache = authzCache;
    }

    @Override
    public SecurityVerdict evaluate(SecurityRequest request, SecurityContext context) {
        SecurityVerdict baseVerdict = delegate.evaluate(request, context);
        if (!baseVerdict.allowed()) {
            return baseVerdict;
        }
        if (!"ADMIN".equals(request.attributes().get(GatewayPlatformSecurityConfiguration.ATTR_BOUNDARY))) {
            return baseVerdict;
        }
        return evaluateAdminAuthz(request, context);
    }

    private SecurityVerdict evaluateAdminAuthz(SecurityRequest request, SecurityContext context) {
        if (!context.authenticated() || context.principal() == null || context.principal().isBlank()) {
            return SecurityVerdict.deny("authz", "authenticated admin context required");
        }
        if (!config.authzAdminCheckEnabled() || config.authzAdminVerifyUri() == null) {
            return SecurityVerdict.deny("authz", "admin authorization is not configured");
        }

        String requestMethod = request.attributes().getOrDefault(
                GatewayPlatformSecurityConfiguration.ATTR_ORIGINAL_METHOD,
                request.action()
        );
        String requestPath = request.attributes().getOrDefault(
                GatewayPlatformSecurityConfiguration.ATTR_ORIGINAL_PATH,
                request.path()
        );
        String requestId = request.attributes().getOrDefault(
                GatewayPlatformSecurityConfiguration.ATTR_REQUEST_ID,
                ""
        );
        String correlationId = request.attributes().getOrDefault(
                GatewayPlatformSecurityConfiguration.ATTR_CORRELATION_ID,
                ""
        );
        String sessionId = context.attributes().getOrDefault("auth.sessionId", "");
        String role = context.roles().stream().findFirst().orElse("");

        String cacheKey = SessionCacheKey.fromToken(String.join(
                "|",
                safeCachePart(context.principal()),
                safeCachePart(role),
                safeCachePart(requestMethod),
                safeCachePart(requestPath)
        ));

        Boolean cached = authzCache.get(cacheKey);
        if (cached != null) {
            return cached
                    ? SecurityVerdict.allow("authz", "admin authorization granted from cache")
                    : SecurityVerdict.deny("authz", "admin authorization denied from cache");
        }

        boolean allowed;
        try {
            allowed = authzServiceClient.verifyAdminAccess(
                    config.authzAdminVerifyUri(),
                    requestMethod,
                    requestPath,
                    requestId,
                    correlationId,
                    new AuthResult(200, true, context.principal(), role, "", sessionId),
                    config.internalRequestSecret()
            );
        } catch (IOException ex) {
            return SecurityVerdict.deny("authz", "admin authorization unavailable");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return SecurityVerdict.deny("authz", "admin authorization interrupted");
        }

        authzCache.put(cacheKey, allowed);
        return allowed
                ? SecurityVerdict.allow("authz", "admin authorization granted")
                : SecurityVerdict.deny("authz", "admin authorization denied");
    }

    private String safeCachePart(String value) {
        return value == null ? "" : value.trim();
    }
}
