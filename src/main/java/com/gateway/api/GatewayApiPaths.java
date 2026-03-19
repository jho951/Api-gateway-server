package com.gateway.api;

/** Gateway에서 사용하는 모든 접속 경로를 한곳에 모아둔 상수 클래스입니다. */
public final class GatewayApiPaths {
    /** 인스턴스화 방지 */
    private GatewayApiPaths() {}

    /** 서버 상태 체크 */
    public static final String HEALTH = "/health";
    public static final String READY = "/ready";

    /** 인증 및 로그인 */
    public static final String AUTH_LOGIN = "/auth/login";
    public static final String AUTH_LOGIN_GITHUB = "/auth/login/github";
    public static final String AUTH_OAUTH2_AUTHORIZE_ALL = "/auth/oauth2/authorize/**";
    public static final String AUTH_OAUTH_GITHUB_CALLBACK = "/auth/oauth/github/callback";
    public static final String AUTH_SESSION = "/auth/session";
    public static final String AUTH_SSO_START = "/auth/sso/start";
    public static final String AUTH_EXCHANGE = "/auth/exchange";
    public static final String AUTH_ME = "/auth/me";
    public static final String AUTH_REFRESH = "/auth/refresh";
    public static final String AUTH_LOGOUT = "/auth/logout";
    public static final String AUTH_INTERNAL_ALL = "/auth/internal/**";
    public static final String OAUTH2_AUTHORIZATION_ALL = "/oauth2/**";
    public static final String LOGIN_OAUTH2_CALLBACK_ALL = "/login/oauth2/**";

    /** 사용자 및 데이터 서비스 */
    public static final String USERS_SIGNUP = "/users/signup";
    public static final String USERS_ME = "/users/me";
    public static final String BLOCKS_ALL = "/blocks/**";
    public static final String PERMISSIONS_ALL = "/permissions/**";

    /** 관리자 전용 */
    public static final String ADMIN_USERS_ALL = "/admin/users/**";
    public static final String ADMIN_BLOCKS_ALL = "/admin/blocks/**";
    public static final String ADMIN_PERMISSIONS_ALL = "/admin/permissions/**";

    /** 서버 내부 전용 */
    public static final String INTERNAL_ALL = "/internal/**";
}
