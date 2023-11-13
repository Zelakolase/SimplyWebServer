package sws.http;

import static sws.http.config.ServerConfig.MAX_RESPONSE_SIZE_BYTES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import sws.http.config.HttpStatusCode;
import sws.io.Log;
import sws.utils.Utils;

public class HttpBufferResponse extends HttpResponse {
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private boolean hasResponse = true;
    private int headerSize = 64; // enough to handle protocol version & status code

    public HttpBufferResponse() {
        this(HttpStatusCode.OK);
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode) {
        this(httpStatusCode, "text/html");
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType) {
        this(httpStatusCode, httpContentType, false);
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType,
            boolean useGzip) {
        this(httpStatusCode, httpContentType, useGzip, new HashMap<>());
    }

    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType,
            boolean useGzip, HashMap<Object, Object> headers) {
        this.httpStatusCode = httpStatusCode;
        this.useGzip = useGzip;
        this.httpContentType = httpContentType;
        this.headerSize += httpContentType.length();
        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    @Override
    public boolean isFileResponse() {
        return false;
    }

    @Override
    public boolean hasResponse() {
        return hasResponse;
    }

    @Override
    public ByteBuffer getResponse() {
        hasResponse = false;
        if (headerSize + body.size() > MAX_RESPONSE_SIZE_BYTES)
            return null;

        ByteBuffer response = ByteBuffer.allocate(headerSize + body.size());
        setBufferWithHeader(response, httpStatusCode, httpContentType, headers);
        try {
            if (useGzip) {
                byte[] compressedBody = Utils.compress(this.body.toByteArray());
                response.put("content-encoding: gzip\r\n".getBytes());
                response.put("content-length: ".getBytes());
                response.put(String.valueOf(compressedBody.length).getBytes());
                response.put("\r\n\r\n".getBytes());
                response.put(compressedBody);
            } else {
                response.put("content-length: ".getBytes());
                response.put(String.valueOf(this.body.size()).getBytes());
                response.put("\r\n\r\n".getBytes());
                response.put(this.body.toByteArray());
            }
        } catch (IOException ignored) {
            Log.e("failed to compress response body");
            response.put("content-length: ".getBytes());
            response.put(String.valueOf(this.body.size()).getBytes());
            response.put("\r\n\r\n".getBytes());
            response.put(this.body.toByteArray());
        } catch (BufferOverflowException e) {
            this.httpStatusCode = HttpStatusCode.INTERNAL_SERVER_ERROR;
            response.position(headerSize);
            response.flip();
            return response;
        } catch (Exception ignored) {
            return null;
        }

        response.flip();
        return response;
    }

    public String getHttpContentType() {
        return httpContentType;
    }

    void setHttpContentType(String httpContentType) {
        this.headerSize -= this.httpContentType.length();
        this.httpContentType = httpContentType;
        this.headerSize += httpContentType.length();
    }

    public HttpStatusCode getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public void setBody(String body) throws IOException {
        setBody(body.getBytes());
    }

    public void setBody(byte[] body) throws IOException {
        this.body.reset();
        this.body.write(body);
    }

    public void appendToBuffer(String buffer) throws IOException {
        appendToBuffer(buffer.getBytes());
    }

    public void appendToBuffer(byte[] buffer) throws IOException {
        this.body.write(buffer);
    }

    public void addHeader(String header, String value) {
        // headerSize = header.length() + ": " + value.length() + "\r\n"
        headerSize += header.length() + 2 + value.length() + 2;
        headers.put(header, value);
    }

    public void deleteHeader(String header) {
        if (headers.containsKey(header)) {
            headerSize -= header.length() + headers.get(header).length();
            headers.remove(header);
        }
    }
}
