package com.gateway.contract.internal.header;

/**
 * 요청 추적 및 상관관계 식별에 사용하는 헤더
 *
 * <p>여러 내부 서비스 전반에서 공통으로 사용할 수 있는 규약입니다.</p>
 */
public final class TraceHeaders {
    private TraceHeaders() {}

    /** 단일 요청에 부여된 고유 번호 */
    public static final String REQUEST_ID = "X-Request-Id";
    /** 전체 트랜잭션을 묶어주는 식별자 */
    public static final String CORRELATION_ID = "X-Correlation-Id";
}
