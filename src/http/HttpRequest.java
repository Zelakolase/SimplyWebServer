package http;

import http.exceptions.HttpRequestException;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static http.config.ServerConfig.MAX_RESPONSE_SIZE_BYTES;
/*
 * This class represents a single HTTP Request
 * @author Omar M. K. and Morad A.
 * @version 1.0
 */
public class HttpRequest {
    /* The path of the HTTP Request */
    private final String path;
    private final ByteBuffer body;
    /* The HTTP Request Method can be GET, POST, etc.. */
    private final String httpRequestMethod;
    private final HashMap<String, String> headers = new HashMap<>();

    /* The size of the request header, in bytes */
    private int headerSize = 0;

    /*
     * Default Constructor
     * @param rawRequest The raw HTTP Request in a ByteBuffer
     */
    public HttpRequest(ByteBuffer rawRequest) throws HttpRequestException {
        String request = new String(rawRequest.array(), 0, rawRequest.limit(), StandardCharsets.US_ASCII);
        String[] lines = request.split("\r\n");
        if (lines.length == 0) {
            throw new HttpRequestException("malformed http request");
        }

        headerSize += lines[0].length() + 2;
        String[] tokens = lines[0].split("\\s");

        httpRequestMethod = tokens[0];
        path = tokens[1];

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
            this.body = buffer;
        } catch (BufferOverflowException ignored) {
            throw new HttpRequestException("http request too big");
        }
    }

    /*
     * Append a buffer (ByteBuffer) to the request body
     */
    public void appendBuffer(ByteBuffer buffer) {
        body.position(body.limit());
        body.limit(body.capacity());
        body.put(buffer);
        body.flip();
    }

    /*
     * Append a buffer (Byte Array) to the request body
     */
    public void appendBuffer(byte[] buffer) {
        body.position(body.limit());
        body.limit(body.capacity());
        body.put(buffer);
        body.flip();
    }

    /*
     * Get the path of the request
     */
    public String getPath() {
        return path;
    }

    /*
     * Get the body (ByteBuffer) of the request
     */
    public ByteBuffer getBody() {
        return body;
    }

    /*
     * Get the body (String) of the request
     */
    public String getBodyAsString() { return new String(body.array(), 0, body.limit()); }

    public byte[] getBodyAsByteArray() {
        byte[] array = new byte[body.limit()];
        body.get(array);
        body.flip();
        return array;
    }

    /*
     * Get the size of the body, in bytes
     */
    public int getBodySize() {
        return body.limit();
    }

    /*
     * Get the size of the header, in bytes
     */
    public int getHeaderSize() {
        return headerSize;
    }

    /*
     * Get the headers. Key: Header name, Value: Header Value
     */
    public HashMap<String, String> getHeaders() {
        return headers;
    }

    /*
     * Get the HTTP Request Method
     */
    public String getHttpRequestMethod() {
        return httpRequestMethod;
    }
}
