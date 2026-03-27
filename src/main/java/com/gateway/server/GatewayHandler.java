package com.gateway.server;

import com.gateway.auth.AuthResult;
import com.gateway.auth.AuthServiceClient;
import com.gateway.cache.LocalSessionCache;
import com.gateway.cache.RedisSessionCache;
import com.gateway.contract.InternalServiceApi;
import com.gateway.contract.external.path.AuthApiPaths;
import com.gateway.contract.external.path.HealthApiPaths;
import com.gateway.code.GatewayErrorCode;
import com.gateway.config.GatewayConfig;
import com.gateway.contract.internal.header.TraceHeaders;
import com.gateway.exception.GatewayException;
import com.gateway.exception.GatewayExceptionHandler;
import com.gateway.http.ExchangeAdapter;
import com.gateway.http.Jsons;
import com.gateway.http.TrustedHeaderNames;
import com.gateway.policy.CorsPolicy;
import com.gateway.policy.RequestWindowRateLimiter;
import com.gateway.policy.SecurityHeadersPolicy;
import com.gateway.proxy.ProxyRequest;
import com.gateway.proxy.ProxyResponse;
import com.gateway.proxy.ReverseProxyClient;
import com.gateway.routing.RouteMatch;
import com.gateway.routing.RouteResolver;
import com.gateway.routing.RouteType;
import com.gateway.security.AuthSessionValidator;
import com.gateway.security.AuthTokenVerifier;
import com.gateway.security.AuthVerificationResult;
import com.gateway.security.JwtPrecheckPolicy;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 문서 설계 기준의 정책형 API Gateway 진입 핸들러입니다. */
public final class GatewayHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(GatewayHandler.class.getName());

    private final GatewayConfig config;
    private final RouteResolver routeResolver;
    private final ReverseProxyClient proxyClient;
    private final CorsPolicy corsPolicy;
    private final SecurityHeadersPolicy securityHeadersPolicy;
    private final RequestWindowRateLimiter loginRateLimiter;
    private final JwtPrecheckPolicy jwtPrecheckPolicy;
    private final AuthSessionValidator sessionValidator;

    public GatewayHandler(GatewayConfig config) {
        this.config = config;
        this.routeResolver = new RouteResolver(config.routes());
        this.proxyClient = new ReverseProxyClient(config.requestTimeout());
        this.corsPolicy = new CorsPolicy(config.allowedOrigins());
        this.securityHeadersPolicy = new SecurityHeadersPolicy();
        this.loginRateLimiter = new RequestWindowRateLimiter(config.loginRateLimitPerMinute(), 60_000);
        this.jwtPrecheckPolicy = new JwtPrecheckPolicy(
                config.jwtPrecheckExpEnabled(),
                config.jwtPrecheckExpClockSkewSeconds(),
                config.jwtPrecheckMaxTokenLength()
        );
        AuthTokenVerifier tokenVerifier = new AuthTokenVerifier(
                config.authJwtVerifyEnabled(),
                config.authJwtPublicKeyPem(),
                config.authJwtSharedSecret(),
                config.authJwtKeyId(),
                config.authJwtAlgorithm(),
                config.authJwtIssuer(),
                config.authJwtAudience(),
                config.authJwtClockSkewSeconds()
        );
        this.sessionValidator = new AuthSessionValidator(
                config.authServiceUri(),
                tokenVerifier,
                new AuthServiceClient(config.requestTimeout()),
                new LocalSessionCache(config.sessionLocalCacheTtlSeconds()),
                new RedisSessionCache(
                        config.sessionCacheEnabled(),
                        config.redisHost(),
                        config.redisPort(),
                        config.redisTimeoutMs(),
                        config.sessionCacheTtlSeconds(),
                        config.sessionCacheKeyPrefix()
                )
        );
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startedAt = System.currentTimeMillis();
        ExchangeAdapter adapter = new ExchangeAdapter(exchange);
        String requestId = resolveOrCreate(exchange.getRequestHeaders().getFirst(TraceHeaders.REQUEST_ID));
        String correlationId = resolveOrCreate(exchange.getRequestHeaders().getFirst(TraceHeaders.CORRELATION_ID));
        applyResponsePolicies(exchange, requestId, correlationId);

        String authOutcome = "FORWARDED";
        String resolvedUserId = "";

        try {
            if (!isAllowedOrigin(exchange.getRequestHeaders().getFirst("Origin"))) {
                throw new GatewayException(GatewayErrorCode.FORBIDDEN);
            }

            if ("OPTIONS".equalsIgnoreCase(adapter.method())) {adapter.sendEmpty(204);
                return;
            }

            InetAddress clientAddress = adapter.remoteAddress().getAddress();
            String clientIp = clientAddress.getHostAddress();

            String path = normalizePath(adapter.uri().getPath());
            if (isHealthPath(path)) {
                adapter.sendJson(200, Jsons.toJson(Map.of("status", "UP")));
                return;
            }

            RouteMatch match = routeResolver.resolve(path, adapter.uri().getRawQuery());
            if (match == null) throw new GatewayException(GatewayErrorCode.NOT_FOUND);

            RouteType routeType = match.route().routeType();

            if (routeType == RouteType.INTERNAL && !config.internalIpPolicy().allows(clientIp)) throw new GatewayException(GatewayErrorCode.FORBIDDEN);
            if (shouldApplyGatewayIpGuard(routeType) && !config.adminIpPolicy().allows(clientIp)) throw new GatewayException(GatewayErrorCode.FORBIDDEN);
            if (isLoginPath(path) && !loginRateLimiter.allow(clientIp)) throw new GatewayException(GatewayErrorCode.TOO_MANY_REQUESTS);

            if (requiresAuthorizationPrecheck(match.route(), path)) {
                String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
                JwtPrecheckPolicy.Result precheckResult =
                        jwtPrecheckPolicy.precheck(authorizationHeader);
                authOutcome = precheckResult.outcome();
                if (!precheckResult.accepted()) {
                    throw new GatewayException(GatewayErrorCode.UNAUTHORIZED);
                }
                AuthVerificationResult verificationResult = sessionValidator.verify(authorizationHeader, requestId, correlationId);
                authOutcome = verificationResult.outcome();
                if (!verificationResult.verified()) {
                    throw new GatewayException(GatewayErrorCode.UNAUTHORIZED);
                }
                AuthResult authResult = verificationResult.authResult();
                if (authResult != null && authResult.getUserId() != null && !authResult.getUserId().isBlank()) {
                    resolvedUserId = authResult.getUserId();
                }
            }

            enforceBodySize(exchange);

            Map<String, List<String>> proxiedHeaders = sanitizeInboundHeaders(exchange, match.route());
            proxiedHeaders.put(TraceHeaders.REQUEST_ID, List.of(requestId));
            proxiedHeaders.put(TraceHeaders.CORRELATION_ID, List.of(correlationId));
            injectTrustedContext(proxiedHeaders, resolvedUserId);

            byte[] requestBody = adapter.readBody();
            ProxyRequest proxyRequest = new ProxyRequest(
                    adapter.method(),
                    match.targetUri(),
                    proxiedHeaders,
                    requestBody,
                    clientIp
            );

            ProxyResponse proxyResponse = proxyClient.forward(proxyRequest);
            if ("FORWARDED".equals(authOutcome)) {
                authOutcome = "PRECHECK_BYPASSED";
            }
            applyResponsePolicies(exchange, requestId, correlationId);
            adapter.sendStream(proxyResponse.getStatusCode(), proxyResponse.getHeaders(), proxyResponse.getBody());
            logRequest(requestId, correlationId, match.route().upstreamName(), path, adapter.method(), clientIp,
                    proxyResponse.getStatusCode(), authOutcome, resolvedUserId, false, startedAt);
        } catch (GatewayException ex) {
            GatewayExceptionHandler.ResponseSpec responseSpec = GatewayExceptionHandler.handleGlobalException(ex);
            adapter.sendJson(responseSpec.httpStatus(), responseSpec.jsonBody());
        } catch (IllegalStateException ex) {
            GatewayExceptionHandler.ResponseSpec responseSpec = GatewayExceptionHandler.fromErrorCode(GatewayErrorCode.PAYLOAD_TOO_LARGE);
            adapter.sendJson(responseSpec.httpStatus(), responseSpec.jsonBody());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            GatewayExceptionHandler.ResponseSpec responseSpec = GatewayExceptionHandler.fromErrorCode(GatewayErrorCode.UPSTREAM_TIMEOUT);
            adapter.sendJson(responseSpec.httpStatus(), responseSpec.jsonBody());
        } catch (IOException ex) {
            log.log(Level.WARNING, "requestId=" + requestId + " upstream_failure=" + ex.getMessage());
            GatewayExceptionHandler.ResponseSpec responseSpec = GatewayExceptionHandler.fromErrorCode(GatewayErrorCode.UPSTREAM_FAILURE);
            adapter.sendJson(responseSpec.httpStatus(), responseSpec.jsonBody());
        } catch (IllegalArgumentException ex) {
            GatewayExceptionHandler.ResponseSpec responseSpec = GatewayExceptionHandler.handleIllegalArgumentException(ex);
            adapter.sendJson(responseSpec.httpStatus(), responseSpec.jsonBody());
        } catch (java.lang.Exception ex) {
            log.log(Level.SEVERE, "requestId=" + requestId + " gateway_error=" + ex.getMessage(), ex);
            GatewayExceptionHandler.ResponseSpec responseSpec = GatewayExceptionHandler.handleException(ex);
            adapter.sendJson(responseSpec.httpStatus(), responseSpec.jsonBody());
        } finally {
            adapter.close();
        }
    }

    private void enforceBodySize(HttpExchange exchange) {
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength == null || contentLength.isBlank()) {
            return;
        }
        try {
            long size = Long.parseLong(contentLength.trim());
            if (size > config.maxBodyBytes()) {
                throw new IllegalStateException("Request body exceeds gateway limit");
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void applyResponsePolicies(HttpExchange exchange, String requestId, String correlationId) {
        corsPolicy.apply(exchange.getRequestHeaders().getFirst("Origin"), exchange.getResponseHeaders());
        securityHeadersPolicy.apply(exchange.getResponseHeaders());
        exchange.getResponseHeaders().set(TraceHeaders.REQUEST_ID, requestId);
        exchange.getResponseHeaders().set(TraceHeaders.CORRELATION_ID, correlationId);
    }

    private boolean isAllowedOrigin(String origin) {
        return origin == null || origin.isBlank() || corsPolicy.isOriginAllowed(origin);
    }

    private boolean isHealthPath(String path) {
        return HealthApiPaths.HEALTH.equals(path) || HealthApiPaths.READY.equals(path);
    }

    private boolean isLoginPath(String path) {
        return AuthApiPaths.LOGIN.equals(path)
                || AuthApiPaths.LOGIN_GITHUB.equals(path)
                || AuthApiPaths.SSO_START.equals(path)
                || path.startsWith("/v1/auth/oauth2/authorize/")
                || path.startsWith("/v1/oauth2/authorization/");
    }

    private boolean shouldApplyGatewayIpGuard(RouteType routeType) {
        return routeType == RouteType.ADMIN;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private String resolveOrCreate(String headerValue) {
        return (headerValue == null || headerValue.isBlank()) ? UUID.randomUUID().toString() : headerValue;
    }

    private boolean requiresAuthorizationPrecheck(com.gateway.routing.RouteDefinition route, String path) {
        return route.routeType() == RouteType.PROTECTED || route.routeType() == RouteType.ADMIN;
    }

    private Map<String, List<String>> sanitizeInboundHeaders(HttpExchange exchange, com.gateway.routing.RouteDefinition route) {
        Map<String, List<String>> sanitized = exchange.getRequestHeaders().entrySet().stream()
                .filter(entry -> !TrustedHeaderNames.ALL.contains(entry.getKey().toLowerCase()))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ArrayList<>(entry.getValue())
                ));
        if (!shouldForwardAuthorizationHeader(route)) {
            sanitized.entrySet().removeIf(entry -> "authorization".equalsIgnoreCase(entry.getKey()));
        }
        return sanitized;
    }

    private boolean shouldForwardAuthorizationHeader(com.gateway.routing.RouteDefinition route) {
        if ("user".equals(route.upstreamName()) || "auth".equals(route.upstreamName())) {
            return true;
        }
        return config.forwardAuthorizationHeader();
    }

    private void injectTrustedContext(Map<String, List<String>> proxiedHeaders, String resolvedUserId) {
        if (resolvedUserId == null || resolvedUserId.isBlank()) {
            return;
        }
        proxiedHeaders.put(InternalServiceApi.Headers.USER_ID, List.of(resolvedUserId));
    }

    private void logRequest(
            String requestId,
            String correlationId,
            String upstreamName,
            String path,
            String method,
            String clientIp,
            int status,
            String authOutcome,
            String userId,
            boolean adminRequest,
            long startedAt
    ) {
        long latency = System.currentTimeMillis() - startedAt;
        String line = "requestId=" + requestId
                + " correlationId=" + correlationId
                + " method=" + method
                + " path=" + path
                + " clientIp=" + clientIp
                + " upstream=" + upstreamName
                + " status=" + status
                + " latencyMs=" + latency
                + " auth=" + authOutcome
                + " userId=" + (userId == null ? "" : userId)
                + " admin=" + adminRequest;
        if (adminRequest) {
            log.warning("audit " + line);
            return;
        }
        log.info(line);
    }
}
