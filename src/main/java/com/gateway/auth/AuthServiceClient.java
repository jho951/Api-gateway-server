package com.gateway.auth;

import com.gateway.api.InternalServiceApi;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 게이트웨이의 인증 판단을 내부 Auth 서비스에 위임하는 클라이언트입니다. */
public final class AuthServiceClient {
    private static final Set<String> FORWARDED_HEADERS = Set.of(
            "authorization",
            "cookie",
            InternalServiceApi.Headers.SSO_TICKET.toLowerCase(),
            "user-agent",
            "origin"
    );
    private static final Pattern BOOLEAN_FIELD = Pattern.compile("\"authenticated\"\\s*:\\s*(true|false)");
    private static final Pattern USER_ID_FIELD = Pattern.compile("\"userId\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern ROLE_FIELD = Pattern.compile("\"role\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern SESSION_ID_FIELD = Pattern.compile("\"sessionId\"\\s*:\\s*\"([^\"]*)\"");

    private final HttpClient client;
    private final Duration timeout;

    public AuthServiceClient(Duration timeout) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.timeout = timeout;
    }

    public AuthResult validate(
            URI validateUri,
            String method,
            URI uri,
            Headers headers,
            String clientIp,
            String requestId,
            String correlationId
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(validateUri)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.noBody())
                .header(InternalServiceApi.Headers.ORIGINAL_METHOD, method)
                .header(InternalServiceApi.Headers.ORIGINAL_URI, uri.toString())
                .header(InternalServiceApi.Headers.ORIGINAL_PATH, uri.getPath())
                .header(InternalServiceApi.Headers.CLIENT_IP, clientIp)
                .header(InternalServiceApi.Headers.REQUEST_ID, requestId)
                .header(InternalServiceApi.Headers.CORRELATION_ID, correlationId);

        headers.forEach((name, values) -> {
            if (FORWARDED_HEADERS.contains(name.toLowerCase())) {
                for (String value : values) {
                    builder.header(name, value);
                }
            }
        });

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return parse(response.statusCode(), response.body());
    }

    private AuthResult parse(int statusCode, String body) {
        boolean authenticated = parseBoolean(body);
        String userId = parseString(body, USER_ID_FIELD);
        String role = parseString(body, ROLE_FIELD);
        String sessionId = parseString(body, SESSION_ID_FIELD);
        return new AuthResult(statusCode, authenticated && statusCode == 200, userId, role, sessionId);
    }

    private boolean parseBoolean(String body) {
        Matcher matcher = BOOLEAN_FIELD.matcher(body == null ? "" : body);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private String parseString(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        return matcher.find() ? matcher.group(1) : "";
    }
}
