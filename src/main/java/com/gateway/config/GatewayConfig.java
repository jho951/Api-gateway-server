package com.gateway.config;

import com.gateway.api.GatewayApiPaths;
import com.gateway.routing.RouteDefinition;
import com.gateway.routing.RouteType;
import com.gateway.security.IpGuardPolicy;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API Gateway 런타임 설정의 불변 표현입니다.
 */
public final class GatewayConfig {
    private final InetSocketAddress bindAddress;
    private final Duration requestTimeout;
    private final URI authServiceUri;
    private final URI userServiceUri;
    private final URI blockServiceUri;
    private final URI permissionServiceUri;
    private final URI adminPermissionVerifyUri;
    private final List<String> allowedOrigins;
    private final IpGuardPolicy ipPolicy;
    private final IpGuardPolicy adminIpPolicy;
    private final int loginRateLimitPerMinute;
    private final int maxBodyBytes;
    private final boolean jwtPrecheckExpEnabled;
    private final int jwtPrecheckExpClockSkewSeconds;
    private final int jwtPrecheckMaxTokenLength;
    private final boolean advancedRoutePoliciesEnabled;
    private final boolean adminPermissionCheckEnabled;
    private final boolean permissionCacheEnabled;
    private final String redisHost;
    private final int redisPort;
    private final int redisTimeoutMs;
    private final int permissionCacheTtlSeconds;
    private final String permissionCacheKeyPrefix;
    private final List<RouteDefinition> routes;

    private GatewayConfig(
            InetSocketAddress bindAddress,
            Duration requestTimeout,
            URI authServiceUri,
            URI userServiceUri,
            URI blockServiceUri,
            URI permissionServiceUri,
            URI adminPermissionVerifyUri,
            List<String> allowedOrigins,
            IpGuardPolicy ipPolicy,
            IpGuardPolicy adminIpPolicy,
            int loginRateLimitPerMinute,
            int maxBodyBytes,
            boolean jwtPrecheckExpEnabled,
            int jwtPrecheckExpClockSkewSeconds,
            int jwtPrecheckMaxTokenLength,
            boolean advancedRoutePoliciesEnabled,
            boolean adminPermissionCheckEnabled,
            boolean permissionCacheEnabled,
            String redisHost,
            int redisPort,
            int redisTimeoutMs,
            int permissionCacheTtlSeconds,
            String permissionCacheKeyPrefix,
            List<RouteDefinition> routes
    ) {
        this.bindAddress = bindAddress;
        this.requestTimeout = requestTimeout;
        this.authServiceUri = authServiceUri;
        this.userServiceUri = userServiceUri;
        this.blockServiceUri = blockServiceUri;
        this.permissionServiceUri = permissionServiceUri;
        this.adminPermissionVerifyUri = adminPermissionVerifyUri;
        this.allowedOrigins = allowedOrigins;
        this.ipPolicy = ipPolicy;
        this.adminIpPolicy = adminIpPolicy;
        this.loginRateLimitPerMinute = loginRateLimitPerMinute;
        this.maxBodyBytes = maxBodyBytes;
        this.jwtPrecheckExpEnabled = jwtPrecheckExpEnabled;
        this.jwtPrecheckExpClockSkewSeconds = jwtPrecheckExpClockSkewSeconds;
        this.jwtPrecheckMaxTokenLength = jwtPrecheckMaxTokenLength;
        this.advancedRoutePoliciesEnabled = advancedRoutePoliciesEnabled;
        this.adminPermissionCheckEnabled = adminPermissionCheckEnabled;
        this.permissionCacheEnabled = permissionCacheEnabled;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisTimeoutMs = redisTimeoutMs;
        this.permissionCacheTtlSeconds = permissionCacheTtlSeconds;
        this.permissionCacheKeyPrefix = permissionCacheKeyPrefix;
        this.routes = routes;
    }

    /**
     * 환경 변수 맵으로부터 게이트웨이 설정을 생성합니다.
     *
     * @param env 시스템 환경 변수 맵
     * @return 검증이 끝난 불변 설정 객체
     */
    public static GatewayConfig fromEnv(Map<String, String> env) {
        String host = env.getOrDefault("GATEWAY_BIND", "0.0.0.0");
        int port = parseInt(env.get("GATEWAY_PORT"), 8080, "GATEWAY_PORT");
        int requestTimeoutMs = parseInt(env.get("GATEWAY_REQUEST_TIMEOUT_MS"), 10_000, "GATEWAY_REQUEST_TIMEOUT_MS");
        int loginRateLimitPerMinute = parseInt(env.get("GATEWAY_LOGIN_RATE_LIMIT_PER_MINUTE"), 20, "GATEWAY_LOGIN_RATE_LIMIT_PER_MINUTE");
        int maxBodyBytes = parseInt(env.get("GATEWAY_MAX_BODY_BYTES"), 1_048_576, "GATEWAY_MAX_BODY_BYTES");
        boolean jwtPrecheckExpEnabled = parseBoolean(env.get("GATEWAY_JWT_PRECHECK_EXP_ENABLED"), false);
        int jwtPrecheckExpClockSkewSeconds = parseInt(env.get("GATEWAY_JWT_PRECHECK_EXP_CLOCK_SKEW_SECONDS"), 30, "GATEWAY_JWT_PRECHECK_EXP_CLOCK_SKEW_SECONDS");
        int jwtPrecheckMaxTokenLength = parseInt(env.get("GATEWAY_JWT_PRECHECK_MAX_TOKEN_LENGTH"), 4096, "GATEWAY_JWT_PRECHECK_MAX_TOKEN_LENGTH");
        boolean advancedRoutePoliciesEnabled = false;
        boolean adminPermissionCheckEnabled = false;
        boolean permissionCacheEnabled = false;
        String redisHost = env.getOrDefault("REDIS_HOST", "127.0.0.1");
        int redisPort = parseInt(env.get("REDIS_PORT"), 6379, "REDIS_PORT");
        int redisTimeoutMs = parseInt(env.get("REDIS_TIMEOUT_MS"), 1000, "REDIS_TIMEOUT_MS");
        int permissionCacheTtlSeconds = parseInt(env.get("GATEWAY_PERMISSION_CACHE_TTL_SECONDS"), 300, "GATEWAY_PERMISSION_CACHE_TTL_SECONDS");
        String permissionCacheKeyPrefix = env.getOrDefault("GATEWAY_PERMISSION_CACHE_PREFIX", "gateway:admin-permission:");

        URI authServiceUri = requiredUri(env.get("AUTH_SERVICE_URL"), "AUTH_SERVICE_URL");
        URI userServiceUri = requiredUri(env.get("USER_SERVICE_URL"), "USER_SERVICE_URL");
        URI blockServiceUri = requiredUri(env.get("BLOCK_SERVICE_URL"), "BLOCK_SERVICE_URL");
        URI permissionServiceUri = optionalUri(env.get("PERMISSION_SERVICE_URL"));

        URI adminPermissionVerifyUri = null;

        List<String> allowedOrigins = EnvParsers.csvOrDefault(env.get("GATEWAY_CORS_ALLOWED_ORIGINS"), List.of("*"));

        IpGuardPolicy ipPolicy = new IpGuardPolicy(
                parseBoolean(env.get("GATEWAY_IP_GUARD_ENABLED"), true),
                EnvParsers.csvOrDefault(env.get("GATEWAY_ALLOWED_IPS"), List.of("*")),
                parseBoolean(env.get("GATEWAY_IP_GUARD_DEFAULT_ALLOW"), false)
        );
        IpGuardPolicy adminIpPolicy = new IpGuardPolicy(false, List.of("*"), true);

        List<RouteDefinition> routes = buildRoutes(authServiceUri, userServiceUri, blockServiceUri, permissionServiceUri);

        return new GatewayConfig(
                new InetSocketAddress(host, port),
                Duration.ofMillis(requestTimeoutMs),
                authServiceUri,
                userServiceUri,
                blockServiceUri,
                permissionServiceUri,
                adminPermissionVerifyUri,
                allowedOrigins,
                ipPolicy,
                adminIpPolicy,
                loginRateLimitPerMinute,
                maxBodyBytes,
                jwtPrecheckExpEnabled,
                jwtPrecheckExpClockSkewSeconds,
                jwtPrecheckMaxTokenLength,
                advancedRoutePoliciesEnabled,
                adminPermissionCheckEnabled,
                permissionCacheEnabled,
                redisHost,
                redisPort,
                redisTimeoutMs,
                permissionCacheTtlSeconds,
                permissionCacheKeyPrefix,
                routes
        );
    }

    private static List<RouteDefinition> buildRoutes(
            URI authServiceUri,
            URI userServiceUri,
            URI blockServiceUri,
            URI permissionServiceUri
    ) {
        List<RouteDefinition> routes = new ArrayList<>();
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_INTERNAL_ALL, RouteType.INTERNAL, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.INTERNAL_ALL, RouteType.INTERNAL, "internal", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_LOGIN, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_LOGIN_GITHUB, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_OAUTH2_AUTHORIZE_ALL, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_OAUTH_GITHUB_CALLBACK, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_SSO_START, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_EXCHANGE, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_REFRESH, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_LOGOUT, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.OAUTH2_AUTHORIZATION_ALL, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.LOGIN_OAUTH2_CALLBACK_ALL, RouteType.PUBLIC, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_SESSION, RouteType.PROTECTED, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.AUTH_ME, RouteType.PROTECTED, "auth", authServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.USERS_SIGNUP, RouteType.PUBLIC, "user", userServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.USERS_ME, RouteType.PROTECTED, "user", userServiceUri));
        routes.add(new RouteDefinition(GatewayApiPaths.BLOCKS_ALL, RouteType.PROTECTED, "block", blockServiceUri));
        if (permissionServiceUri != null) {
            routes.add(new RouteDefinition(GatewayApiPaths.PERMISSIONS_ALL, RouteType.PROTECTED, "permission", permissionServiceUri));
        }
        routes.sort(RouteDefinition.MOST_SPECIFIC_FIRST);
        return List.copyOf(routes);
    }

    private static URI requiredUri(String rawValue, String key) {
        URI uri = optionalUri(rawValue);
        if (uri == null) {
            throw new IllegalArgumentException(key + " must be configured");
        }
        return uri;
    }

    private static URI optionalUri(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        URI uri = URI.create(rawValue.trim());
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("Service URL must be absolute: " + rawValue);
        }
        return uri;
    }

    private static int parseInt(String rawValue, int defaultValue, String key) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a valid integer", ex);
        }
    }

    private static boolean parseBoolean(String rawValue, boolean defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(rawValue.trim());
    }

    public InetSocketAddress bindAddress() {
        return bindAddress;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public URI authServiceUri() {
        return authServiceUri;
    }

    public URI userServiceUri() {
        return userServiceUri;
    }

    public URI blockServiceUri() {
        return blockServiceUri;
    }

    public URI permissionServiceUri() {
        return permissionServiceUri;
    }

    public URI adminPermissionVerifyUri() {
        return adminPermissionVerifyUri;
    }

    public List<String> allowedOrigins() {
        return allowedOrigins;
    }

    public IpGuardPolicy ipPolicy() {
        return ipPolicy;
    }

    public IpGuardPolicy adminIpPolicy() {
        return adminIpPolicy;
    }

    public int loginRateLimitPerMinute() {
        return loginRateLimitPerMinute;
    }

    public int maxBodyBytes() {
        return maxBodyBytes;
    }

    public boolean jwtPrecheckExpEnabled() {
        return jwtPrecheckExpEnabled;
    }

    public int jwtPrecheckExpClockSkewSeconds() {
        return jwtPrecheckExpClockSkewSeconds;
    }

    public int jwtPrecheckMaxTokenLength() {
        return jwtPrecheckMaxTokenLength;
    }

    public boolean permissionCacheEnabled() {
        return permissionCacheEnabled;
    }

    public boolean adminPermissionCheckEnabled() {
        return adminPermissionCheckEnabled;
    }

    public boolean advancedRoutePoliciesEnabled() {
        return advancedRoutePoliciesEnabled;
    }

    public String redisHost() {
        return redisHost;
    }

    public int redisPort() {
        return redisPort;
    }

    public int redisTimeoutMs() {
        return redisTimeoutMs;
    }

    public int permissionCacheTtlSeconds() {
        return permissionCacheTtlSeconds;
    }

    public String permissionCacheKeyPrefix() {
        return permissionCacheKeyPrefix;
    }

    public List<RouteDefinition> routes() {
        return routes;
    }
}
