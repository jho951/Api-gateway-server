package com.gateway.exception;

import com.gateway.code.GatewayErrorCode;

public class GatewayException extends RuntimeException {
    private final GatewayErrorCode gatewayErrorCode;

    /**
     * 생성자
     * @param gatewayErrorCode 에러 코드
     */
    public GatewayException(GatewayErrorCode gatewayErrorCode) {
        super(gatewayErrorCode.getMessage());
        this.gatewayErrorCode = gatewayErrorCode;
    }

    public GatewayErrorCode getErrorCode() {
        return gatewayErrorCode;
    }
}
