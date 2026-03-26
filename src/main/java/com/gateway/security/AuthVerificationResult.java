package com.gateway.security;

import com.gateway.auth.AuthResult;

/**
 * JWT/세션 검증 결과와 캐시 상태를 함께 담는 DTO입니다.
 */
public final class AuthVerificationResult {
    private final AuthTokenVerifier.Result verifierResult;
    private final AuthResult authResult;

    private AuthVerificationResult(AuthTokenVerifier.Result verifierResult, AuthResult authResult) {
        this.verifierResult = verifierResult;
        this.authResult = authResult;
    }

    public static AuthVerificationResult fromCache(AuthResult authResult, String outcome) {
        return new AuthVerificationResult(AuthTokenVerifier.Result.skipped(outcome), authResult);
    }

    public static AuthVerificationResult fromService(AuthTokenVerifier.Result verifierResult, AuthResult authResult) {
        return new AuthVerificationResult(verifierResult, authResult);
    }

    public static AuthVerificationResult failed(AuthTokenVerifier.Result verifierResult) {
        return new AuthVerificationResult(verifierResult, null);
    }

    public boolean verified() {
        return verifierResult.verified();
    }

    public boolean performed() {
        return verifierResult.performed();
    }

    public String outcome() {
        return verifierResult.outcome();
    }

    public AuthResult authResult() {
        return authResult;
    }
}
