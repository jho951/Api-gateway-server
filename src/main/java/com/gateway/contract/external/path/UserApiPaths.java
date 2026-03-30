package com.gateway.contract.external.path;

/** 사용자 및 데이터 서비스 */
public final class UserApiPaths {
    private UserApiPaths() {}

    public static final String SIGNUP = "/v1/users/signup";
    public static final String ME = "/v1/users/me";
    public static final String INTERNAL_FIND_OR_CREATE_AND_LINK_SOCIAL = "/v1/internal/users/find-or-create-and-link-social";
    public static final String INTERNAL_USERS_ALL = "/v1/internal/users/**";
}
