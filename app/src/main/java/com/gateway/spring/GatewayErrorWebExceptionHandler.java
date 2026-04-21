package com.gateway.spring;

import com.gateway.exception.ResponseSpec;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public final class GatewayErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
    private final GatewayFailureResponseFactory failureResponseFactory;
    private final GatewayResponseContractWriter responseContractWriter;

    public GatewayErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer,
            GatewayFailureResponseFactory failureResponseFactory,
            GatewayResponseContractWriter responseContractWriter
    ) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.failureResponseFactory = failureResponseFactory;
        this.responseContractWriter = responseContractWriter;
        setMessageWriters(serverCodecConfigurer.getWriters());
        setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        ResponseSpec responseSpec = failureResponseFactory.fromThrowable(error, request.path(), resolveRequestId(request));
        return responseContractWriter.write(responseSpec);
    }

    private String resolveRequestId(ServerRequest request) {
        Object attribute = request.exchange().getAttribute(GatewayCommonWebFilter.REQUEST_ID_ATTR);
        if (attribute instanceof String value && !value.isBlank()) {
            return value;
        }
        String headerValue = request.headers().firstHeader("X-Request-Id");
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue;
        }
        return request.exchange().getRequest().getId();
    }
}
