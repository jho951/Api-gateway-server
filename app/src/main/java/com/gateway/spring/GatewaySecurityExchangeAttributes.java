package com.gateway.spring;

public final class GatewaySecurityExchangeAttributes {
    public static final String ROUTE_MATCH = GatewaySecurityExchangeAttributes.class.getName() + ".routeMatch";
    public static final String AUTH_RESULT = GatewaySecurityExchangeAttributes.class.getName() + ".authResult";
    public static final String AUTH_OUTCOME = GatewaySecurityExchangeAttributes.class.getName() + ".authOutcome";
    public static final String REQUEST_CHANNEL = GatewaySecurityExchangeAttributes.class.getName() + ".requestChannel";
    public static final String SECURITY_EVALUATION_RESULT = GatewaySecurityExchangeAttributes.class.getName() + ".securityEvaluationResult";
    public static final String FAILURE_ERROR_CODE = GatewaySecurityExchangeAttributes.class.getName() + ".failureErrorCode";

    private GatewaySecurityExchangeAttributes() {
    }
}
