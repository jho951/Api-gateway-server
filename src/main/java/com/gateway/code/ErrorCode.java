package com.gateway.code;

public enum ErrorCode {
    INVALID_REQUEST(400, 6000, "잘못된 요청입니다."),
    VALIDATION_ERROR(400, 6001, "요청 필드 유효성 검사에 실패했습니다."),
    METHOD_NOT_ALLOWED(405, 6002, "허용되지 않은 HTTP 메서드입니다."),
    NOT_FOUND_URL(404, 6003, "요청하신 URL을 찾을 수 없습니다."),
    UNAUTHORIZED(401, 6004, "인증이 필요합니다."),
    FORBIDDEN(403, 6005, "접근이 허용되지 않습니다."),
    TOO_MANY_REQUESTS(429, 6006, "요청이 너무 많습니다."),
    PAYLOAD_TOO_LARGE(413, 6007, "요청 본문이 허용 크기를 초과했습니다."),
    UPSTREAM_TIMEOUT(504, 6008, "업스트림 응답 시간이 초과되었습니다."),
    UPSTREAM_FAILURE(502, 6009, "업스트림 호출에 실패했습니다."),
    FAIL(500, 6999, "요청 처리 중 오류가 발생했습니다.");

    private final int httpStatus;
    private final int code;
    private final String message;

    ErrorCode(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
