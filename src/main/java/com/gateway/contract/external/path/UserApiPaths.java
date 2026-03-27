package com.gateway.contract.external.path;

/** 사용자 및 데이터 서비스 */
public final class UserApiPaths {
    private UserApiPaths() {}

    public static final String SIGNUP = "/v1/users/signup";
    public static final String ME = "/v1/users/me";
    public static final String INTERNAL_USERS_ALL = "/v1/internal/users/**";
}
