package http;

import java.util.HashMap;

public class HttpResponse {
    private final HashMap<String, String> headers = new HashMap<>();
    private String body;
    private HttpStatusCode httpStatusCode;
    private HttpContentType httpContentType;

    public HttpResponse() {
        this.httpStatusCode = HttpStatusCode.OK;
    }

    public HttpResponse(String body, HttpContentType httpContentType) {
        this(body, HttpStatusCode.OK, httpContentType, new HashMap<>());
    }


    public HttpResponse(String body, HttpStatusCode httpStatusCode, HttpContentType httpContentType) {
        this(body, httpStatusCode, httpContentType, new HashMap<>());
    }

    public HttpResponse(String body, HttpStatusCode httpStatusCode, HttpContentType httpContentType, HashMap<Object, Object> headers) {
        this.body = body;
        this.httpStatusCode = httpStatusCode;
        this.httpContentType = httpContentType;
        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    public HttpContentType getHttpContentType() {
        return httpContentType;
    }

    public HttpStatusCode getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public String getBody() {
        return body;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(Object key, Object value) {
        headers.put(key.toString(), value.toString());
    }

    public void deleteHeader(Object key) {
        headers.remove(key.toString());
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setHttpStatusCode(HttpStatusCode httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public void setHttpContentType(HttpContentType httpContentType) {
        this.httpContentType = httpContentType;
    }
}
