package http;

import http.config.HttpStatusCode;
import lib.log;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static http.config.ServerConfig.MAX_RESPONSE_SIZE_BYTES;
import static lib.Network.compress;

public class HttpBufferResponse extends HttpResponse {
    private boolean hasResponse = true;
    private final ByteBuffer body = ByteBuffer.allocate(MAX_RESPONSE_SIZE_BYTES);

    public HttpBufferResponse() {
        this(HttpStatusCode.OK);
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode) {
        this(httpStatusCode, "text/html");
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType) {
        this(httpStatusCode, httpContentType, false);
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType, boolean useGzip) {
        this(httpStatusCode, httpContentType, useGzip, new HashMap<>());
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType, boolean useGzip,
                              HashMap<Object, Object> headers) {
        this.httpStatusCode = httpStatusCode;
        this.useGzip = useGzip;
        this.httpContentType = httpContentType;
        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    @Override
    public boolean isFileResponse() {
        return false;
    }

    @Override
    public boolean hasResponse() {
        boolean temp = hasResponse;
        hasResponse = false;
        return temp;
    }

    @Override
    public ByteBuffer getResponse() throws BufferOverflowException {
        ByteBuffer response = ByteBuffer.allocate(MAX_RESPONSE_SIZE_BYTES);
        setBufferWithHeader(response, httpStatusCode, httpContentType, headers);
        this.body.flip();
        try {
            if (useGzip) {
                ByteBuffer compressedBody = compress(this.body);
                response.put("content-encoding: gzip\r\n".getBytes());
                response.put("content-length: ".getBytes());
                response.put(String.valueOf(compressedBody.limit()).getBytes());
                response.put("\r\n\r\n".getBytes());
                response.put(compressedBody);
            } else {
                response.put("content-length: ".getBytes());
                response.put(String.valueOf(this.body.limit()).getBytes());
                response.put("\r\n\r\n".getBytes());
                response.put(this.body);
            }
        } catch (IOException ignored) {
            log.e("failed to compress response body");
            response.put("content-length: ".getBytes());
            response.put(String.valueOf(this.body.limit()).getBytes());
            response.put("\r\n\r\n".getBytes());
            response.put(this.body);
        } catch (BufferOverflowException e) {
            this.httpStatusCode = HttpStatusCode.INTERNAL_SERVER_ERROR;
            response.position(headers.size());
            response.flip();
            return response;
        }

        response.flip();
        return response;
    }

    public String getHttpContentType() {
        return httpContentType;
    }

    public HttpStatusCode getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public void setBody(String body) {
        this.body.clear().put(body.getBytes());
    }

    public void setBuffer(ByteBuffer buffer) {
        this.body.clear().put(buffer);
    }

    public void appendToBuffer(String buffer) {
        this.body.put(buffer.getBytes());
    }

    public void appendToBuffer(ByteBuffer buffer) {
        this.body.put(buffer);
    }
}
