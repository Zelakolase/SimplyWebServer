package sws.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import sws.http.exceptions.HttpRequestException;
import sws.io.Log;

public class HttpRequest {
    private final String path;
    private final String httpRequestMethod;
    private final ByteArrayOutputStream body;
    private final HashMap<String, String> headersMap = new HashMap<>();

    public HttpRequest(String rawRequest) {
        String[] httpRequestSections = rawRequest.split("\r\n\r\n"); // header and body
        String[] headerLines, tokens;

        if (httpRequestSections.length == 0) {
            Log.e("Malformed http request: no header or body");
            Log.e(rawRequest);
            path = null;
            httpRequestMethod = null;
            body = null;
            return;
        } else if (httpRequestSections.length == 1) {
            headerLines = httpRequestSections[0].split("\r\n");
            body = null;
        } else {
            headerLines = httpRequestSections[0].split("\r\n");
            body = new ByteArrayOutputStream(httpRequestSections[1].length());
        }

        if (headerLines.length == 0) {
            Log.e("Malformed http request: no headers");
            Log.e(rawRequest);
            path = null;
            httpRequestMethod = null;
            return;
        }

        tokens = headerLines[0].split("\\s");
        if (tokens.length != 2 && tokens.length != 3) {
            Log.e("Malformed http request: error parsing http method and path");
            Log.e(rawRequest);
            path = null;
            httpRequestMethod = null;
            return;
        }

        path = tokens[1];
        httpRequestMethod = tokens[0];

        int idx = 1;
        for (; idx < headerLines.length; ++idx) {
            tokens = headerLines[idx].split(":", 2);
            if (tokens.length != 2) {
                // TODO: better use warning than error
                Log.e("Malformed http request: failed to parse header line (" + headerLines[idx]
                        + ")");
                continue;
            }
            headersMap.put(tokens[0].toLowerCase().trim(), tokens[1].toLowerCase().trim());
        }

        try {
            body.write(httpRequestSections[1].getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HttpRequest(ByteArrayOutputStream rawRequest) throws HttpRequestException {
        this(rawRequest.toString(StandardCharsets.US_ASCII));
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

    public HashMap<String, String> getHeadersMap() {
        return headersMap;
    }

    public String getHttpRequestMethod() {
        return httpRequestMethod;
    }
}
