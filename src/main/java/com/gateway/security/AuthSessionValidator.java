package com.gateway.security;

import com.gateway.auth.AuthResult;
import com.gateway.auth.AuthServiceClient;
import com.gateway.cache.LocalSessionCache;
import com.gateway.cache.RedisSessionCache;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JWT + 세션 검증을 위한 L1/L2 캐시를 관리하고 Auth Service로 fall back 하는 클래스입니다.
 */
public final class AuthSessionValidator {
    private static final Logger log = Logger.getLogger(AuthSessionValidator.class.getName());

    private final URI authServiceUri;
    private final AuthTokenVerifier tokenVerifier;
    private final AuthServiceClient authServiceClient;
    private final LocalSessionCache localCache;
    private final RedisSessionCache redisCache;

    public AuthSessionValidator(
            URI authServiceUri,
            AuthTokenVerifier tokenVerifier,
            AuthServiceClient authServiceClient,
            LocalSessionCache localCache,
            RedisSessionCache redisCache
    ) {
        this.authServiceUri = authServiceUri;
        this.tokenVerifier = tokenVerifier;
        this.authServiceClient = authServiceClient;
        this.localCache = localCache;
        this.redisCache = redisCache;
    }

    public AuthVerificationResult verify(String authorizationHeader, String requestId, String correlationId) throws IOException, InterruptedException {
        String token = extractToken(authorizationHeader);
        if (token.isEmpty()) {
            return AuthVerificationResult.failed(AuthTokenVerifier.Result.rejected("INVALID_AUTH_HEADER"));
        }
        String cacheKey = SessionCacheKey.fromToken(token);

        AuthResult cached = localCache.get(cacheKey);
        if (cached != null) {
            return AuthVerificationResult.fromCache(cached, "SESSION_CACHE_L1");
        }

        if (redisCache != null && redisCache.enabled()) {
            try {
                cached = redisCache.get(cacheKey);
            } catch (IOException ex) {
                log.log(Level.FINE, "redis session cache read failed", ex);
            }
            if (cached != null) {
                localCache.put(cacheKey, cached);
                return AuthVerificationResult.fromCache(cached, "SESSION_CACHE_L2");
            }
        }

        AuthTokenVerifier.Result verificationResult = tokenVerifier.verify(authorizationHeader);
        if (!verificationResult.verified()) {
            return AuthVerificationResult.failed(verificationResult);
        }

        AuthResult authResult = authServiceClient.validateSession(authServiceUri, authorizationHeader, requestId, correlationId);
        if (!authResult.isAuthenticated()) {
            return AuthVerificationResult.failed(verificationResult);
        }

        localCache.put(cacheKey, authResult);
        if (redisCache != null && redisCache.enabled()) {
            try {
                redisCache.put(cacheKey, authResult);
            } catch (IOException ex) {
                log.log(Level.FINE, "redis session cache write failed", ex);
            }
        }

        return AuthVerificationResult.fromService(verificationResult, authResult);
    }

    private static String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return "";
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            return "";
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
