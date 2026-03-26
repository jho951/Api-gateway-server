package com.gateway.http;

import com.gateway.exception.GatewayErrorResponse;

import java.util.Iterator;
import java.util.Map;

/**
 * 외부 라이브러리(Jackson, Gson 등) 없이 자바 객체를 JSON 문자열로 변환하는 유틸리티 클래스입니다.
 * 모든 메서드는 정적(static)이며, 인스턴스화를 방지하기 위해 생성자가 제한되어 있습니다.
 */
public final class Jsons {
    private Jsons() {}

    /**
     * 문자열 내의 특수 문자를 JSON 규격에 맞게 이스케이프 처리합니다.
     * <p>
     * JSON 문자열 값 내부에 포함된 역슬래시, 쌍따옴표, 줄바꿈 기호 등은
     * 데이터 파싱 에러를 유발하므로 반드시 역슬래시(\)를 붙여 처리해야 합니다.
     * </p>
     * @param value 변환할 원본 문자열 데이터
     * @return JSON 규격에 맞게 이스케이프된 문자열
     */
    private static String escape(String value) {
        return value
                .replace("\\", "\\\\") // 역슬래시 자체를 표현
                .replace("\"", "\\\"") // 쌍따옴표 중단 방지
                .replace("\n", "\\n")   // 줄바꿈 보존
                .replace("\r", "\\r");  // 캐리지 리턴 보존
    }

    /**
     * 객체의 타입(String, Number, Map 등)을 분석하여 적절한 JSON 값 형태로 변환합니다.
     * <p>
     * Map 타입의 경우 내부 필드들을 재귀적으로 호출하여 JSON 객체({}) 구조를 생성합니다.
     * </p>
     * @param value JSON으로 변환할 객체
     * @return 해당 객체의 JSON 표현식 문자열 (예: "string", 123, true, { ... })
     */
    private static String toJsonValue(Object value) {
        // 1. null 처리: JSON null 리터럴 반환
        if (value == null) return "null";
        // 2. 문자열 처리: 앞뒤에 쌍따옴표를 붙이고 내부 이스케이프 수행
        if (value instanceof String){
            String stringValue = (String) value;
            return "\"" + escape(stringValue) + "\"";
        }
        // 3. 숫자 처리: 값 그대로를 문자열로 반환 (따옴표 없음)
        if (value instanceof Number) return String.valueOf(value);
        // 4. 불리언 처리: 값 그대로를 문자열로 반환 (따옴표 없음)
        if (value instanceof Boolean) return String.valueOf(value);
        // 5. Map 구조 처리: 재귀를 통해 JSON Object 구조 구축
        if (value instanceof Map<?, ?> mapValue) {
            StringBuilder builder = new StringBuilder();
            Iterator<? extends Map.Entry<?, ?>> iterator = mapValue.entrySet().iterator();
            builder.append('{');
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                // Key는 항상 문자열로 취급하여 이스케이프 처리
                builder.append("\"")
                        .append(escape(String.valueOf(entry.getKey())))
                        .append("\":")
                        .append(toJsonValue(entry.getValue())); // Value는 재귀 처리
                // 마지막 요소가 아닐 경우에만 쉼표(,) 추가
                if (iterator.hasNext()) builder.append(',');
            }
            builder.append('}');
            return builder.toString();
        }
        // 6. 기타 타입: toString() 결과를 이스케이프하여 문자열로 처리
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    /**
     * Map 형태의 데이터를 JSON 문자열로 변환합니다.
     * @param body JSON으로 변환할 키-값 쌍의 맵
     * @return 변환된 JSON 전체 문자열
     */
    public static String toJson(Map<String, ?> body) {
        return toJsonValue(body);
    }

    /**
     * 게이트웨이 전용 에러 응답 객체(GatewayErrorResponse)를 JSON 문자열로 변환합니다.
     * <p>
     * 내부 필드(code, message, path, requestId)를 순서대로 직렬화하며,
     * 반복문 없이 직접 필드에 접근하여 Map 방식보다 성능상 이점이 있습니다.
     * </p>
     * @param response 에러 응답 DTO 객체
     * @return 규격화된 에러 응답 JSON 문자열
     */
    public static String toJson(GatewayErrorResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append('{')
                .append("\"code\":").append(toJsonValue(response.getCode())).append(',')
                .append("\"message\":").append(toJsonValue(response.getMessage())).append(',')
                .append("\"path\":").append(toJsonValue(response.getPath())).append(',')
                .append("\"requestId\":").append(toJsonValue(response.getRequestId()))
                .append('}');
        return builder.toString();
    }
}