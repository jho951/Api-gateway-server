package com.gateway.spring;

import com.gateway.metrics.GatewayMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GatewayStatusController {
    private final GatewayMetrics metrics;

    public GatewayStatusController(GatewayMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping({"/health", "/ready"})
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/metrics")
    public String metrics() {
        return metrics.scrape();
    }
}
