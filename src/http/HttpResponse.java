package http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static http.ServerConfig.HTTP_PROTO_VERSION;
import static http.ServerConfig.MAX_RESPONSE_SIZE_BYTES;
import static lib.Network.compress;

public class HttpResponse {
    private final ByteBuffer buffer = ByteBuffer.allocate(MAX_RESPONSE_SIZE_BYTES);
    private final HashMap<String, String> headers = new HashMap<>();
    private HttpStatusCode httpStatusCode;
    private HttpContentType httpContentType;

    public HttpResponse() {
        this(HttpContentType.TEXT_HTML);
    }

    public HttpResponse(HttpContentType httpContentType) {
        this(HttpStatusCode.OK, httpContentType);
    }

    public HttpResponse(HttpStatusCode httpStatusCode, HttpContentType httpContentType) {
        this(httpStatusCode, httpContentType, new HashMap<>());
    }

    public HttpResponse(HttpStatusCode httpStatusCode, HttpContentType httpContentType, HashMap<Object, Object> headers) {
        this.httpStatusCode = httpStatusCode;
        this.httpContentType = httpContentType;
        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    public void addHeader(Object key, Object value) {
        headers.put(key.toString(), value.toString());
    }

    public void deleteHeader(Object key) {
        headers.remove(key.toString());
    }

    public ByteBuffer getHttpResponseBuffer(boolean useGzip) {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_RESPONSE_SIZE_BYTES);
        buffer.put(HTTP_PROTO_VERSION.getBytes());
        buffer.put((byte) ' ');
        buffer.put(String.valueOf(httpStatusCode.getHtmlCode()).getBytes());
        buffer.put((byte) ' ');
        buffer.put(httpStatusCode.toString().getBytes());
        buffer.put("\r\n".getBytes());
        headers.forEach((key, value) -> {
            buffer.put(key.toLowerCase().getBytes());
            buffer.put(": ".getBytes());
            buffer.put(value.toLowerCase().getBytes());
            buffer.put("\r\n".getBytes());
        });
        buffer.put("content-type: ".getBytes());
        buffer.put(httpContentType.toString().getBytes());
        buffer.put("\r\n".getBytes());

        try {
            this.buffer.flip();
            if (useGzip) {
                ByteBuffer compressedBody = compress(this.buffer);
                buffer.put("content-encoding: gzip\r\n".getBytes());
                buffer.put("content-length: ".getBytes());
                buffer.put(String.valueOf(compressedBody.position()).getBytes());
                buffer.put("\r\n\r\n".getBytes());
                buffer.put(compressedBody);
            } else {
                buffer.put("content-length: ".getBytes());
                buffer.put(String.valueOf(buffer.position()).getBytes());
                buffer.put("\r\n\r\n".getBytes());
                buffer.put(this.buffer);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        buffer.flip();
        return buffer;
    }

    public HttpContentType getHttpContentType() {
        return httpContentType;
    }

    public void setHttpContentType(HttpContentType httpContentType) {
        this.httpContentType = httpContentType;
    }

    public HttpStatusCode getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public void setHttpStatusCode(HttpStatusCode httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(String buffer) {
        this.buffer.clear().put(buffer.getBytes());
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }
}
