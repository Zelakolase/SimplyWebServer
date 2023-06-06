package http;

import http.config.HttpStatusCode;
import lib.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static http.config.ServerConfig.MAX_RESPONSE_SIZE_BYTES;
import static lib.Network.compress;

/*
 * HTTP Buffer Response Class. This class represents a single HTTP Response. It is used to send data,
 * not file content.
 * @see HttpFileResponse
 * @see HttpResponse
 * @author Omar M. K. and Morad A.
 * @version 1.0
 */
public class HttpBufferResponse extends HttpResponse {
    /* The response body, as a byte array stream. The stream is used for dynamic array growth. */
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    /* Check if a response is present, it is triggered off in HttpBufferResponse#getResponse(). */
    private boolean hasResponse = true;
    /* The header size, the default size is 64 for the first line only. */
    private int headerSize = 64;

    /*
     * Default Constructor, default HttpStatusCode is 200-OK
     * @see HttpBufferResponse#HttpBufferResponse(HttpStatusCode httpStatusCode)
     */
    public HttpBufferResponse() {
        this(HttpStatusCode.OK);
    }

    /*
     * A constructor for setting a custom HttpStatusCode, default content type is 'text/html'
     * @see HttpBufferResponse#HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType)
     */
    public HttpBufferResponse(HttpStatusCode httpStatusCode) {
        this(httpStatusCode, "text/html");
    }

    /*
     * A constructor for setting a custom HttpStatusCode and Content Type, default GZip status is 'false'
     * @see HttpBufferResponse#HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType, boolean useGzip)
     */
    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType) {
        this(httpStatusCode, httpContentType, false);
    }

    /*
     * A constructor for setting a custom HttpStatusCode and Content Type and GZip status, default headers are none.
     * @see HttpBufferResponse#HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType, boolean useGzip, HashMap<Object, Object> headers)
     */
    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType, boolean useGzip) {
        this(httpStatusCode, httpContentType, useGzip, new HashMap<>());
    }

    /*
     * A full customization constructor
     */
    public HttpBufferResponse(HttpStatusCode httpStatusCode, String httpContentType, boolean useGzip, HashMap<Object, Object> headers) {
        this.httpStatusCode = httpStatusCode;
        this.useGzip = useGzip;
        this.httpContentType = httpContentType;
        this.headerSize += httpContentType.length();
        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    /*
     * Check if its a file response, always false
     */
    @Override
    public boolean isFileResponse() {
        return false;
    }

    /*
     * Returns if the class has a response
     */
    @Override
    public boolean hasResponse() {
        return hasResponse;
    }

    /*
     * Get the response.
     * @throws BufferOverflowException If the total size of the request is higher than MAX_RESPONSE_SIZE_BYTES
     * @see http.config.ServerConfig#MAX_RESPONSE_SIZE_BYTES
     * @returns A ByteBuffer with the total response body (Header + Body)
     */
    @Override
    public ByteBuffer getResponse() throws BufferOverflowException {
        hasResponse = false;
        if (headerSize + body.size() > MAX_RESPONSE_SIZE_BYTES) throw new BufferOverflowException();

        ByteBuffer response = ByteBuffer.allocate(headerSize + body.size());
        setBufferWithHeader(response, httpStatusCode, httpContentType, headers);
        try {
            if (useGzip) {
                byte[] compressedBody = compress(this.body.toByteArray());
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
            // If it fails to compress the request, send the request with no GZip compression
            log.e("failed to compress response body");
            response.put("content-length: ".getBytes());
            response.put(String.valueOf(this.body.size()).getBytes());
            response.put("\r\n\r\n".getBytes());
            response.put(this.body.toByteArray());
        } catch (BufferOverflowException e) {
            // If the response is bigger than MAX_RESPONSE_SIZE_BYTES, return Internal Server Error
            this.httpStatusCode = HttpStatusCode.INTERNAL_SERVER_ERROR;
            response.position(headerSize); // equals the default value of 64
            response.flip();
            return response;
        }

        response.flip();
        return response;
    }

    /*
     * Getter for the Content Type
     */
    public String getHttpContentType() {
        return httpContentType;
    }

    /*
     * Set a certain Content Type
     */
    void setHttpContentType(String httpContentType) {
        this.headerSize -= this.httpContentType.length(); // Decreases the previous content type value
        this.httpContentType = httpContentType;
        this.headerSize += httpContentType.length(); // Updates/increases the new content type value
    }

    /*
     * Getter for the Status Code
     */
    public HttpStatusCode getHttpStatusCode() {
        return this.httpStatusCode;
    }

    /*
     * Sets the Response body (in String), does not override. For overriding, see appendToBuffer(..).
     * @see #setBody(byte[] body)
     */
    public void setBody(String body) throws IOException {
        setBody(body.getBytes());
    }

    /*
     * Sets the response body (in Byte array), does not override. For overriding, see appendToBuffer(..).
     */
    public void setBody(byte[] body) throws IOException {
        this.body.reset(); // Resets the array
        this.body.write(body);
    }

    /*
     * Appends new String data to the existing buffer.
     * @see #appendToBuffer(byte[] buffer)
     */
    public void appendToBuffer(String buffer) throws IOException {
        appendToBuffer(buffer.getBytes());
    }

    /*
     * Appends new Byte array to the existing buffer.
     */
    public void appendToBuffer(byte[] buffer) throws IOException {
        this.body.write(buffer);
    }

    /*
     * Add a header to the response
     */
    public void addHeader(String header, String value) {
        // headerSize = header.length() + ": " + value.length() + "\r\n" /// DON'T UNCOMMENT THIS
        headerSize += header.length() + 2 + value.length() + 2;
        headers.put(header, value);
    }

    /*
     * Removes the header based on its key
     */
    public void deleteHeader(String header) {
        if (headers.containsKey(header)) {
            headerSize -= header.length() + headers.get(header).length();
            headers.remove(header);
        }
    }
}
