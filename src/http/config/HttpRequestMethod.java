package http.config;

public enum HttpRequestMethod {
    GET("get"), POST("post"), DELETE("delete"), UPDATE("update"), CUSTOM("custom");

    private final String method;

    HttpRequestMethod(String method) {
        this.method = method;
    }

    public static HttpRequestMethod fromString(String string) {
        for (HttpRequestMethod httpRequestMethod : HttpRequestMethod.values()) {
            if (httpRequestMethod.method.equalsIgnoreCase(string)) return httpRequestMethod;
        }
        return CUSTOM;
    }
}
