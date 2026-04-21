package com.gateway.spring;

import com.gateway.exception.ResponseSpec;
import io.github.jho951.platform.security.web.ReactiveSecurityFailureResponseWriter;
import io.github.jho951.platform.security.web.SecurityFailureResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public final class GatewayPlatformFailureResponseWriter implements ReactiveSecurityFailureResponseWriter {
    private final GatewayFailureResponseFactory failureResponseFactory;
    private final GatewayResponseContractWriter responseContractWriter;

    public GatewayPlatformFailureResponseWriter(
            GatewayFailureResponseFactory failureResponseFactory,
            GatewayResponseContractWriter responseContractWriter
    ) {
        this.failureResponseFactory = failureResponseFactory;
        this.responseContractWriter = responseContractWriter;
    }

    @Override
    public Mono<Void> write(ServerWebExchange exchange, SecurityFailureResponse failureResponse) {
        ResponseSpec responseSpec = failureResponseFactory.fromSecurityFailure(exchange, failureResponse);
        return responseContractWriter.write(exchange, responseSpec);
    }
}
