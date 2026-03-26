package com.gateway.contract.internal.path;

/**
 * Gateway가 내부 서비스에 호출하는 내부 API 경로
 *
 * <p>gateway가 upstream 서비스(auth, permission 등)와 통신할 때 사용하는
 * 구체적인 내부 endpoint 계약만 관리합니다.</p>
 */
public final class ServicePaths {
    private ServicePaths() {}

    /** auth-service 내부 호출 경로 */
    public static final class Auth {
        private Auth() {}

        /** 세션/인증 상태 유효성 검증 */
        public static final String SESSION_VALIDATE = "/auth/internal/session/validate";
    }


    /** permission-service 내부 호출 경로 */
    public static final class Permission {
        private Permission() {}

        /** 관리자 권한 보유 여부 검증 */
        public static final String ADMIN_VERIFY = "/permissions/internal/admin/verify";
    }
}