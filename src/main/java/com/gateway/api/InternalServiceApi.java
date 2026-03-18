package com.gateway.api;

/** Gateway와 내부 서버 사이의 약속을 정의한 통신 규격 상수*/
public final class InternalServiceApi {
    /** 인스턴스화 방지 */
    private InternalServiceApi() {}

    /** 유효한 세션 여부 확인 통로 */
    public static final class Auth {
        /** 인스턴스화 방지 */
        private Auth() {}
        public static final String SESSION_VALIDATE = "/auth/internal/session/validate";
    }

    /** 관리자 권한 여부 확인 통로 */
    public static final class Permission {
        /** 인스턴스화 방지 */
        private Permission() {}
        public static final String ADMIN_VERIFY = "/permissions/internal/admin/verify";
    }

    /** HTTP 통신 시 주고받는 커스텀 헤더 명칭 상수 집합*/
    public static final class Headers {
        /** 인스턴스화 방지 */
        private Headers() {}
        /** 로그인한 사용자의 고유 식별자(ID) */
        public static final String USER_ID = "X-User-Id";
        /** 사용자의 권한 */
        public static final String USER_ROLE = "X-User-Role";
        /** 통합 로그인(SSO) 환경에서 사용하는 인증 토큰 */
        public static final String SESSION_ID = "X-Session-Id";

        /** 단일 요청에 부여된 고유 번호 */
        public static final String REQUEST_ID = "X-Request-Id";
        /** 전체 트랜잭션을 묶어주는 식별자 */
        public static final String CORRELATION_ID = "X-Correlation-Id";

        /** 클라이언트가 처음에 보낸 HTTP 메서드 */
        public static final String ORIGINAL_METHOD = "X-Original-Method";
        /** 클라이언트가 실제로 호출한 원본 주소 */
        public static final String ORIGINAL_URI = "X-Original-Uri";
        /** 프록시나 게이트웨이를 거치기 전, 사용자의 실제 접속 IP 주소 */
        public static final String ORIGINAL_PATH = "X-Original-Path";

        /** 사용자의 현재 브라우저 세션 ID */
        public static final String CLIENT_IP = "X-Client-Ip";
        /** 사용자가 이미 인증되었음을 증명하는 티켓 */
        public static final String SSO_TICKET = "X-SSO-Ticket";
    }
}
