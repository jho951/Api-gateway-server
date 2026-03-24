package com.gateway.exception;

import com.gateway.code.ErrorCode;
import com.gateway.code.SuccessCode;
import com.gateway.dto.GlobalResponse;
import com.gateway.http.Jsons;

public final class GlobalExceptionHandler {
    private GlobalExceptionHandler() {
    }

    public static ResponseSpec handleGlobalException(GlobalException ex) {
        return fromErrorCode(ex.getErrorCode());
    }

    public static ResponseSpec handleIllegalArgumentException(IllegalArgumentException ex) {
        return fromErrorCode(ErrorCode.INVALID_REQUEST);
    }

    public static ResponseSpec handleException(Exception ex) {
        return fromErrorCode(ErrorCode.FAIL);
    }

    public static ResponseSpec fromErrorCode(ErrorCode errorCode) {
        return new ResponseSpec(errorCode.getHttpStatus(), Jsons.toJson(GlobalResponse.fail(errorCode)));
    }

    public static <T> ResponseSpec fromSuccessCode(SuccessCode successCode, T data) {
        GlobalResponse<T> response = GlobalResponse.ok(successCode, data);
        return new ResponseSpec(response.getHttpStatus(), Jsons.toJson(response));
    }

    public record ResponseSpec(int httpStatus, String jsonBody) {
    }
}
