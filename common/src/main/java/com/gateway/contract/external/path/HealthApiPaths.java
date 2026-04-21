package com.gateway.contract.external.path;

/** 서버 상태 체크 */
public final class HealthApiPaths {
    private HealthApiPaths() {}

    public static final String READY = "/v1/ready";
    public static final String HEALTH = "/v1/health";
}
