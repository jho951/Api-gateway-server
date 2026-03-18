package com.gateway.auth;

import com.gateway.api.InternalServiceApi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** 관리자 경로에 대한 추가 권한 확인을 Permission Service에 위임합니다. */
public final class PermissionServiceClient {
    private final HttpClient client;
    private final Duration timeout;

    public PermissionServiceClient(Duration timeout) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.timeout = timeout;
    }

    public boolean verifyAdminAccess(
            URI verifyUri,
            String method,
            String path,
            String requestId,
            String correlationId,
            AuthResult authResult
    ) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(verifyUri)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.noBody())
                .header(InternalServiceApi.Headers.ORIGINAL_METHOD, method)
                .header(InternalServiceApi.Headers.ORIGINAL_PATH, path)
                .header(InternalServiceApi.Headers.REQUEST_ID, requestId)
                .header(InternalServiceApi.Headers.CORRELATION_ID, correlationId)
                .header(InternalServiceApi.Headers.USER_ID, authResult.getUserId())
                .header(InternalServiceApi.Headers.USER_ROLE, authResult.getRole())
                .header(InternalServiceApi.Headers.SESSION_ID, authResult.getSessionId())
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() == 200;
    }
}
