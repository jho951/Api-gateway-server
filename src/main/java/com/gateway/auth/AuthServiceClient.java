package com.gateway.auth;

import com.gateway.api.InternalServiceApi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 보호 경로 진입 전에 Auth Service에 인증 검증을 위임합니다.
 */
public final class AuthServiceClient {
    private static final Pattern BOOLEAN_FIELD = Pattern.compile("\"authenticated\"\\s*:\\s*(true|false)");
    private static final Pattern USER_ID_FIELD = Pattern.compile("\"userId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ROLE_FIELD = Pattern.compile("\"role\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SESSION_ID_FIELD = Pattern.compile("\"sessionId\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient client;
    private final Duration timeout;

    public AuthServiceClient(Duration timeout) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.timeout = timeout;
    }

    public AuthResult validateSession(
            URI authServiceBaseUri,
            String authorizationHeader,
            String requestId,
            String correlationId
    ) throws IOException, InterruptedException {
        URI targetUri = authServiceBaseUri.resolve(InternalServiceApi.Auth.SESSION_VALIDATE);

        HttpRequest request = HttpRequest.newBuilder(targetUri)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Authorization", authorizationHeader)
                .header(InternalServiceApi.Headers.REQUEST_ID, requestId)
                .header(InternalServiceApi.Headers.CORRELATION_ID, correlationId)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body() == null ? "" : response.body();
        String userId = firstJsonField(responseBody, USER_ID_FIELD);
        if (userId == null || userId.isBlank()) {
            userId = firstHeader(response, InternalServiceApi.Headers.USER_ID);
        }

        String role = firstJsonField(responseBody, ROLE_FIELD);
        if (role == null || role.isBlank()) {
            role = firstHeader(response, InternalServiceApi.Headers.USER_ROLE);
        }

        String sessionId = firstJsonField(responseBody, SESSION_ID_FIELD);
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = firstHeader(response, InternalServiceApi.Headers.SESSION_ID);
        }

        boolean authenticated = response.statusCode() == 200
                && isAuthenticated(responseBody)
                && userId != null
                && !userId.isBlank();

        return new AuthResult(response.statusCode(), authenticated, userId, role, sessionId);
    }

    private String firstHeader(HttpResponse<?> response, String headerName) {
        Optional<String> header = response.headers().firstValue(headerName);
        return header.orElse(null);
    }

    private boolean isAuthenticated(String responseBody) {
        Matcher matcher = BOOLEAN_FIELD.matcher(responseBody);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private String firstJsonField(String responseBody, Pattern pattern) {
        Matcher matcher = pattern.matcher(responseBody);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}
