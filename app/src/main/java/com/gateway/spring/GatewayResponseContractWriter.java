package com.gateway.spring;

import com.gateway.exception.ResponseSpec;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public final class GatewayResponseContractWriter {
    public Mono<Void> write(ServerWebExchange exchange, ResponseSpec responseSpec) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(responseSpec.getHttpStatus()));
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
        exchange.getResponse().getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");
        byte[] bytes = responseSpec.getJsonBody().getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public Mono<ServerResponse> write(ResponseSpec responseSpec) {
        return ServerResponse.status(responseSpec.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(BodyInserters.fromValue(responseSpec.getJsonBody()));
    }
}
