package sws.http;

import sws.http.exceptions.HttpRequestException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static sws.http.config.ServerConfig.MAX_REQUEST_SIZE_BYTES;

public class HttpRequest {
    private final String path;
    private final String httpRequestMethod;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private final HashMap<String, String> headers = new HashMap<>();
    private int headerSize = 0;

    public HttpRequest(ByteArrayOutputStream rawRequest) throws HttpRequestException {
        String request = rawRequest.toString(StandardCharsets.US_ASCII);
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
            while (idx < lines.length) {
                body.write(lines[idx++].getBytes());
            }
        } catch (IOException ignored) {
            throw new HttpRequestException("couldn't parse http request body");
        }
    }

    public void appendBuffer(ByteArrayOutputStream buffer) throws IOException, HttpRequestException {
        appendBuffer(buffer.toByteArray());
    }

    public void appendBuffer(byte[] buffer) throws IOException, HttpRequestException {
        if (body.size() + buffer.length > MAX_REQUEST_SIZE_BYTES)
            throw new HttpRequestException("request size is more that the configured maximum");
        body.write(buffer);
    }

    public String getPath() {
        return path;
    }

    public ByteArrayOutputStream getBody() {
        return body;
    }

    public String getBodyAsString() {
        return body.toString();
    }

    public byte[] getBodyAsByteArray() {
        return body.toByteArray();
    }

    public int getBodySize() {
        return body.size();
    }

    public int getHeaderSize() {
        return headerSize;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public String getHttpRequestMethod() {
        return httpRequestMethod;
    }
}
