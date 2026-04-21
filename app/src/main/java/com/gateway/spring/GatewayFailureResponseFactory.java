package com.gateway.spring;

import com.gateway.code.GatewayErrorCode;
import com.gateway.exception.GatewayException;
import com.gateway.exception.GatewayExceptionHandler;
import com.gateway.exception.ResponseSpec;
import com.gateway.routing.RouteMatch;
import com.gateway.routing.RouteType;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.github.jho951.platform.security.web.SecurityFailureResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

@Component
public final class GatewayFailureResponseFactory {
    public ResponseSpec fromSecurityFailure(ServerWebExchange exchange, SecurityFailureResponse failureResponse) {
        GatewayErrorCode errorCode = resolveSecurityFailureErrorCode(exchange, failureResponse);
        return fromGatewayErrorCode(
                errorCode,
                exchange.getRequest().getPath().pathWithinApplication().value(),
                resolveRequestId(exchange)
        );
    }

    public ResponseSpec fromGatewayErrorCode(GatewayErrorCode errorCode, String path, String requestId) {
        return GatewayExceptionHandler.fromErrorCode(errorCode, path, requestId);
    }

    public ResponseSpec fromThrowable(Throwable error, String path, String requestId) {
        GatewayErrorCode upstreamError = detectUpstreamError(error);
        if (upstreamError != null) {
            return fromGatewayErrorCode(upstreamError, path, requestId);
        }
        if (error instanceof GatewayException gatewayException) {
            return GatewayExceptionHandler.handleGatewayException(gatewayException, path, requestId);
        }
        if (error instanceof IllegalArgumentException illegalArgumentException) {
            return GatewayExceptionHandler.handleIllegalArgumentException(illegalArgumentException, path, requestId);
        }
        if (error instanceof ResponseStatusException responseStatusException) {
            return fromGatewayErrorCode(fromStatus(responseStatusException.getStatusCode().value()), path, requestId);
        }
        return GatewayExceptionHandler.handleException(new Exception(error), path, requestId);
    }

    private GatewayErrorCode resolveSecurityFailureErrorCode(ServerWebExchange exchange, SecurityFailureResponse failureResponse) {
        GatewayErrorCode explicit = exchange.getAttribute(GatewaySecurityExchangeAttributes.FAILURE_ERROR_CODE);
        if (explicit != null) {
            return explicit;
        }
        RouteMatch match = exchange.getAttribute(GatewaySecurityExchangeAttributes.ROUTE_MATCH);
        if (match != null
                && match.route().routeType() == RouteType.INTERNAL
                && failureResponse.status() == HttpStatus.UNAUTHORIZED.value()) {
            return GatewayErrorCode.FORBIDDEN;
        }
        if (failureResponse.status() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return GatewayErrorCode.TOO_MANY_REQUESTS;
        }
        if (failureResponse.status() == HttpStatus.UNAUTHORIZED.value()) {
            return GatewayErrorCode.MISSING_AUTH_CREDENTIALS;
        }
        return GatewayErrorCode.FORBIDDEN;
    }

    private GatewayErrorCode detectUpstreamError(Throwable error) {
        if (hasCause(error, ReadTimeoutException.class) || hasCause(error, TimeoutException.class)) {
            return GatewayErrorCode.UPSTREAM_TIMEOUT;
        }
        if (hasCause(error, ConnectException.class)
                || hasCause(error, ConnectTimeoutException.class)
                || hasCause(error, UnknownHostException.class)) {
            return GatewayErrorCode.UPSTREAM_FAILURE;
        }
        return null;
    }

    private GatewayErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> GatewayErrorCode.INVALID_REQUEST;
            case 401 -> GatewayErrorCode.UNAUTHORIZED;
            case 403 -> GatewayErrorCode.FORBIDDEN;
            case 404 -> GatewayErrorCode.NOT_FOUND;
            case 405 -> GatewayErrorCode.METHOD_NOT_ALLOWED;
            case 413 -> GatewayErrorCode.PAYLOAD_TOO_LARGE;
            case 429 -> GatewayErrorCode.TOO_MANY_REQUESTS;
            case 502 -> GatewayErrorCode.UPSTREAM_FAILURE;
            case 504 -> GatewayErrorCode.UPSTREAM_TIMEOUT;
            default -> GatewayErrorCode.INTERNAL_ERROR;
        };
    }

    private String resolveRequestId(ServerWebExchange exchange) {
        Object attribute = exchange.getAttribute(GatewayCommonWebFilter.REQUEST_ID_ATTR);
        if (attribute instanceof String value && !value.isBlank()) {
            return value;
        }
        String headerValue = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue;
        }
        return exchange.getRequest().getId();
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
