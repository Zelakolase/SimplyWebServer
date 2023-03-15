package http;

import http.config.HttpStatusCode;
import lib.log;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static http.config.ServerConfig.HTTP_PROTO_VERSION;
import static http.config.ServerConfig.MAX_RESPONSE_SIZE_BYTES;
import static lib.Network.compress;

public class HttpBufferResponse extends HttpResponse {
    private final ByteBuffer buffer = ByteBuffer.allocate(MAX_RESPONSE_SIZE_BYTES);
    private final String httpContentType = "text/html";

    public HttpBufferResponse() {
        this(HttpStatusCode.OK);
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode) {
        this(httpStatusCode, false);
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode, boolean useGzip) {
        this(httpStatusCode, useGzip, new HashMap<>());
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode, boolean useGzip, HashMap<Object, Object> headers) {
        this.httpStatusCode = httpStatusCode;
        this.isFile = false;
        this.useGzip = useGzip;
        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    @Override
    public ByteBuffer getResponse() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_RESPONSE_SIZE_BYTES);
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
        buffer.put(this.httpContentType.toString().getBytes());
        buffer.put("\r\n".getBytes());

        this.buffer.flip();
        try {
            if (useGzip) {
                ByteBuffer compressedBody = compress(this.buffer);
                buffer.put("content-encoding: gzip\r\n".getBytes());
                buffer.put("content-length: ".getBytes());
                buffer.put(String.valueOf(compressedBody.limit()).getBytes());
                buffer.put("\r\n\r\n".getBytes());
                buffer.put(compressedBody);
            } else {
                buffer.put("content-length: ".getBytes());
                buffer.put(String.valueOf(this.buffer.limit()).getBytes());
                buffer.put("\r\n\r\n".getBytes());
                buffer.put(this.buffer);
            }
        } catch (IOException ignored) {
            log.e("failed to compress response body");
            buffer.put("content-length: ".getBytes());
            buffer.put(String.valueOf(this.buffer.limit()).getBytes());
            buffer.put("\r\n\r\n".getBytes());
            buffer.put(this.buffer);
        } catch (BufferOverflowException e) {
            this.httpStatusCode = HttpStatusCode.INTERNAL_SERVER_ERROR;
            buffer.position(headers.size());
            return buffer;
        }

        buffer.flip();
        return buffer;
    }

    public String getHttpContentType() {
        return httpContentType;
    }

    public HttpStatusCode getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public void setBuffer(String buffer) {
        setBuffer(buffer.getBytes());
    }
    public void setBuffer(byte[] buffer) {
        this.buffer.clear().put(buffer);
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer.clear().put(buffer);
    }

    public void appendToBuffer(String buffer) {
        appendToBuffer(buffer.getBytes());
    }

    public void appendToBuffer(byte[] buffer) {
        this.buffer.put(buffer);
    }

    public void appendToBuffer(ByteBuffer buffer) {
        this.buffer.put(buffer);
    }

    public int getBufferSize() {
        return buffer.position();
    }
}
