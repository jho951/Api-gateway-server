package com.gateway.http;

import com.gateway.dto.GlobalResponse;

import java.util.Iterator;
import java.util.Map;

public final class Jsons {
    /** 인스턴스 방지 */
    private Jsons() {}

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escape(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> mapValue) {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = mapValue.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                builder.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":")
                        .append(toJsonValue(entry.getValue()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return builder.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    public static String toJson(GlobalResponse<?> response) {
        StringBuilder builder = new StringBuilder();
        builder.append('{')
                .append("\"httpStatus\":").append(response.getHttpStatus()).append(',')
                .append("\"success\":").append(response.isSuccess()).append(',')
                .append("\"message\":\"").append(escape(response.getMessage())).append("\",")
                .append("\"code\":").append(response.getCode()).append(',')
                .append("\"data\":").append(toJsonValue(response.getData()))
                .append('}');
        return builder.toString();
    }

}
