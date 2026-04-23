package com.gateway.spring;

import com.gateway.auth.AuthResult;
import com.gateway.auth.AuthzServiceClient;
import com.gateway.cache.RedisAuthzCache;
import com.gateway.config.GatewayConfig;
import com.gateway.security.SessionCacheKey;
import io.github.jho951.platform.security.api.SecurityContext;
import io.github.jho951.platform.security.api.SecurityPolicy;
import io.github.jho951.platform.security.api.SecurityRequest;
import io.github.jho951.platform.security.api.SecurityVerdict;
import io.github.jho951.platform.security.policy.SecurityAttributes;

import java.io.IOException;
import java.util.Objects;

final class GatewayAdminAuthorizationPolicy implements SecurityPolicy {
    private final GatewayConfig config;
    private final AuthzServiceClient authzServiceClient;
    private final RedisAuthzCache authzCache;

    GatewayAdminAuthorizationPolicy(
            GatewayConfig config,
            AuthzServiceClient authzServiceClient,
            RedisAuthzCache authzCache
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.authzServiceClient = Objects.requireNonNull(authzServiceClient, "authzServiceClient");
        this.authzCache = Objects.requireNonNull(authzCache, "authzCache");
    }

    @Override
    public String name() {
        return "gateway-admin-authz";
    }

    @Override
    public SecurityVerdict evaluate(SecurityRequest request, SecurityContext context) {
        String boundary = request.attributes().get(SecurityAttributes.BOUNDARY);
        if (!"ADMIN".equalsIgnoreCase(boundary)) {
            return SecurityVerdict.allow(name(), "non-admin boundary");
        }
        if (!context.authenticated() || context.principal() == null || context.principal().isBlank()) {
            return SecurityVerdict.deny(name(), "authenticated admin context required");
        }
        if (!config.authzAdminCheckEnabled() || config.authzAdminVerifyUri() == null) {
            return SecurityVerdict.deny(name(), "admin authorization is not configured");
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
                    ? SecurityVerdict.allow(name(), "admin authorization granted from cache")
                    : SecurityVerdict.deny(name(), "admin authorization denied from cache");
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
        } catch (IOException exception) {
            return SecurityVerdict.deny(name(), "admin authorization unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return SecurityVerdict.deny(name(), "admin authorization interrupted");
        }

        authzCache.put(cacheKey, allowed);
        return allowed
                ? SecurityVerdict.allow(name(), "admin authorization granted")
                : SecurityVerdict.deny(name(), "admin authorization denied");
    }

    private String safeCachePart(String value) {
        return value == null ? "" : value.trim();
    }
}
