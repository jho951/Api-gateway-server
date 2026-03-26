package com.gateway.exception;

public final class GatewayErrorResponse {
    private final String code;
    private final String message;
    private final String path;
    private final String requestId;

    public GatewayErrorResponse(String code, String message, String path, String requestId) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
    }

    public String getCode() {return code;}
    public String getMessage() {return message;}
    public String getPath() {return path;}
    public String getRequestId() {return requestId;}
}