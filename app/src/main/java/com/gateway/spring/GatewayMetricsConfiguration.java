package com.gateway.spring;

import com.gateway.metrics.GatewayMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayMetricsConfiguration {
    @Bean
    public GatewayMetrics gatewayMetrics() {
        return new GatewayMetrics();
    }
}
