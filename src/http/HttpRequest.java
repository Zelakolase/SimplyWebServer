package http;

import server.Server;

import java.util.HashMap;

public class HttpRequest {
    private final String path;
    private final String body;
    private final HttpRequestMethod httpRequestMethod;
    private final HashMap<String, String> headers = new HashMap<>();

    public HttpRequest(String request, Server server) throws Exception {
        String[] lines = request.split("\n");
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
            throw new Exception("Unknown HTTP request method");
        }

        this.path = tokens[1];

        int idx = 1;
        for (; idx < lines.length && !lines[idx].isBlank(); ++idx) {
            tokens = lines[idx].split(":", 2);
            headers.put(tokens[0], tokens[1]);
        }

        ++idx;
        StringBuilder stringBuilder = new StringBuilder(server.maxRequestSizeBytes);
        while (idx < lines.length) {
            stringBuilder.append(lines[idx++]);
        }
        this.body = stringBuilder.toString();
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return body;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public HttpRequestMethod getHttpRequestMethod() {
        return httpRequestMethod;
    }
}
