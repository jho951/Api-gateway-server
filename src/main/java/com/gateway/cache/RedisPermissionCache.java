package com.gateway.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 관리자 권한 판정 결과를 Redis에 짧게 저장하는 최소 구현입니다.
 *
 * <p>외부 Redis 클라이언트 의존성을 추가하지 않기 위해 RESP 일부만 직접 사용합니다.</p>
 */
public final class RedisPermissionCache {
    private final boolean enabled;
    private final String host;
    private final int port;
    private final int timeoutMs;
    private final int ttlSeconds;
    private final String keyPrefix;

    public RedisPermissionCache(boolean enabled, String host, int port, int timeoutMs, int ttlSeconds, String keyPrefix) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
        this.ttlSeconds = ttlSeconds;
        this.keyPrefix = keyPrefix;
    }

    public Boolean get(String cacheKey) {
        if (!enabled) {
            return null;
        }
        try (Connection connection = new Connection(host, port, timeoutMs)) {
            String value = connection.get(keyPrefix + cacheKey);
            if (value == null) {
                return null;
            }
            return "ALLOW".equalsIgnoreCase(value);
        } catch (IOException ex) {
            return null;
        }
    }

    public void put(String cacheKey, boolean allowed) {
        if (!enabled) {
            return;
        }
        try (Connection connection = new Connection(host, port, timeoutMs)) {
            connection.setEx(keyPrefix + cacheKey, ttlSeconds, allowed ? "ALLOW" : "DENY");
        } catch (IOException ignored) {
        }
    }

    private static final class Connection implements AutoCloseable {
        private final Socket socket;
        private final BufferedInputStream in;
        private final BufferedOutputStream out;

        private Connection(String host, int port, int timeoutMs) throws IOException {
            this.socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            this.in = new BufferedInputStream(socket.getInputStream());
            this.out = new BufferedOutputStream(socket.getOutputStream());
        }

        private String get(String key) throws IOException {
            writeArray("GET", key);
            out.flush();
            return readBulkString();
        }

        private void setEx(String key, int ttlSeconds, String value) throws IOException {
            writeArray("SETEX", key, String.valueOf(ttlSeconds), value);
            out.flush();
            readSimpleString();
        }

        private void writeArray(String... values) throws IOException {
            out.write(("*" + values.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                out.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(bytes);
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        }

        private String readBulkString() throws IOException {
            int type = in.read();
            if (type == '$') {
                int length = Integer.parseInt(readLine());
                if (length < 0) {
                    return null;
                }
                byte[] payload = in.readNBytes(length);
                readCrlf();
                return new String(payload, StandardCharsets.UTF_8);
            }
            if (type == '-') {
                throw new IOException(readLine());
            }
            throw new IOException("Unexpected Redis response type: " + (char) type);
        }

        private String readSimpleString() throws IOException {
            int type = in.read();
            if (type == '+') {
                return readLine();
            }
            if (type == '-') {
                throw new IOException(readLine());
            }
            throw new IOException("Unexpected Redis response type: " + (char) type);
        }

        private String readLine() throws IOException {
            StringBuilder builder = new StringBuilder();
            int current;
            while ((current = in.read()) != -1) {
                if (current == '\r') {
                    int next = in.read();
                    if (next == '\n') {
                        break;
                    }
                } else {
                    builder.append((char) current);
                }
            }
            return builder.toString();
        }

        private void readCrlf() throws IOException {
            if (in.read() != '\r' || in.read() != '\n') {
                throw new IOException("Invalid Redis CRLF");
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
