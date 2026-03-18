package com.gateway.http;

import com.gateway.api.InternalServiceApi;

import java.util.Set;

/**
 * 게이트웨이가 재주입하는 trusted header 이름 모음입니다.
 */
public final class TrustedHeaderNames {
    public static final Set<String> ALL = Set.of(
            InternalServiceApi.Headers.USER_ID.toLowerCase(),
            InternalServiceApi.Headers.USER_ROLE.toLowerCase(),
            InternalServiceApi.Headers.SESSION_ID.toLowerCase(),
            InternalServiceApi.Headers.REQUEST_ID.toLowerCase(),
            InternalServiceApi.Headers.CORRELATION_ID.toLowerCase(),
            "x-auth-user-id",
            "x-auth-session-id",
            "x-auth-roles",
            "x-auth-subject",
            "x-auth-authenticated"
    );

    private TrustedHeaderNames() {
    }
}
