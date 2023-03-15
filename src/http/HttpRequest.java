package http;

import http.exceptions.HttpRequestException;
import lib.PathFilter;
import lib.log;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static http.ServerConfig.MAX_RESPONSE_SIZE_BYTES;

public class HttpRequest {
    private final String path;
    private final ByteBuffer buffer;
    private final HttpRequestMethod httpRequestMethod;
    private final HashMap<String, String> headers = new HashMap<>();

    private int bodySize = 0, headerSize = 0;

    public HttpRequest(ByteBuffer rawRequest) throws HttpRequestException {
        String request = new String(rawRequest.array(), 0, rawRequest.limit(), StandardCharsets.US_ASCII);
        String[] lines = request.split("\r\n");
        if (lines.length == 0) {
            throw new HttpRequestException("malformed http request");
        }

        headerSize += lines[0].length() + 2;
        String[] tokens = lines[0].split("\\s");

        if (tokens[0].equalsIgnoreCase("get")) {
            httpRequestMethod = HttpRequestMethod.GET;
        } else if (tokens[0].equalsIgnoreCase("post")) {
            httpRequestMethod = HttpRequestMethod.POST;
        } else if (tokens[0].equalsIgnoreCase("delete")) {
            httpRequestMethod = HttpRequestMethod.DELETE;
        } else if (tokens[0].equalsIgnoreCase("update")) {
            httpRequestMethod = HttpRequestMethod.UPDATE;
        } else {
            httpRequestMethod = HttpRequestMethod.CUSTOM;
        }

        this.path = PathFilter.filter(tokens[1]);
        log.i(this.path);

        int idx = 1;
        for (; idx < lines.length && !lines[idx].isBlank(); ++idx) {
            headerSize += lines[idx].length() + 2;
            tokens = lines[idx].split(":", 2);
            if (tokens.length != 2) {
                throw new HttpRequestException("missing colon in http header");
            }
            headers.put(tokens[0].toLowerCase().trim(), tokens[1].toLowerCase().trim());
        }

        headerSize += 2;
        ++idx;

        try {
            ByteBuffer buffer = ByteBuffer.allocate(MAX_RESPONSE_SIZE_BYTES - headerSize);
            while (idx < lines.length) {
                buffer.put(lines[idx++].getBytes());
            }
            buffer.flip();
            this.bodySize = buffer.limit();
            this.buffer = buffer;
        } catch (BufferOverflowException ignored) {
            throw new HttpRequestException("http request too big");
        }
    }

    public void appendBuffer(ByteBuffer buffer) {
        this.buffer.limit(bodySize);
        this.buffer.put(buffer);
        this.buffer.flip();
        bodySize = this.buffer.limit();
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return new String(buffer.array(), 0, buffer.limit());
    }

    public int getBodySize() {
        return bodySize;
    }

    public int getHeaderSize() {
        return headerSize;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public HttpRequestMethod getHttpRequestMethod() {
        return httpRequestMethod;
    }
}
