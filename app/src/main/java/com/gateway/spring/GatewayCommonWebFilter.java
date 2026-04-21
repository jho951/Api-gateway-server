package com.gateway.spring;

import com.gateway.contract.internal.header.TraceHeaders;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public final class GatewayCommonWebFilter implements WebFilter, Ordered {
    static final String REQUEST_ID_ATTR = GatewayCommonWebFilter.class.getName() + ".requestId";
    static final String CORRELATION_ID_ATTR = GatewayCommonWebFilter.class.getName() + ".correlationId";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = resolveOrCreate(exchange.getRequest().getHeaders().getFirst(TraceHeaders.REQUEST_ID));
        String correlationId = resolveOrCreate(exchange.getRequest().getHeaders().getFirst(TraceHeaders.CORRELATION_ID));

        exchange.getAttributes().put(REQUEST_ID_ATTR, requestId);
        exchange.getAttributes().put(CORRELATION_ID_ATTR, correlationId);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(TraceHeaders.REQUEST_ID, requestId);
                    headers.set(TraceHeaders.CORRELATION_ID, correlationId);
                })
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        mutatedExchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = mutatedExchange.getResponse().getHeaders();
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("Referrer-Policy", "no-referrer");
            headers.set("Cache-Control", "no-store");
            headers.set(TraceHeaders.REQUEST_ID, requestId);
            headers.set(TraceHeaders.CORRELATION_ID, correlationId);
            return Mono.empty();
        });

        return chain.filter(mutatedExchange);
    }

    private static String resolveOrCreate(String value) {
        return (value == null || value.isBlank()) ? UUID.randomUUID().toString() : value;
    }
}
