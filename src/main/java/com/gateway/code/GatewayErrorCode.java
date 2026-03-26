package com.gateway.code;

public enum GatewayErrorCode {
    INVALID_REQUEST(400, "INVALID_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(403, "FORBIDDEN", "접근이 허용되지 않습니다."),
    NOT_FOUND(404, "NOT_FOUND", "요청한 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "허용되지 않은 HTTP 메서드입니다."),
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "요청이 너무 많습니다."),
    PAYLOAD_TOO_LARGE(413, "PAYLOAD_TOO_LARGE", "요청 본문이 허용 크기를 초과했습니다."),
    UPSTREAM_FAILURE(502, "UPSTREAM_FAILURE", "업스트림 호출에 실패했습니다."),
    UPSTREAM_TIMEOUT(504, "UPSTREAM_TIMEOUT", "업스트림 응답 시간이 초과되었습니다."),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "게이트웨이 처리 중 오류가 발생했습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;

    /**
     * 생성자
     * @param httpStatus http 상태코드
     * @param code 커스텀 상태코드 (API-gateway 서버 전용 1000 ~ 1999)
     * @param message 추가 메시지
     */
    GatewayErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() {return httpStatus;}
    public String getCode() {return code;}
    public String getMessage() {return message;}
}