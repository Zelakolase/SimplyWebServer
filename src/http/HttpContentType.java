package http;

import http.config.HttpRequestMethod;

public enum HttpContentType {
    APPLICATION_JSON("application/json"), TEXT_HTML("text/html"), TEXT_PLAIN("text/plain");

    private final String htmlString;

    HttpContentType(String htmlString) {
        this.htmlString = htmlString;
    }

    public static HttpContentType fromString(String string) {
        for (HttpContentType httpContentType : HttpContentType.values()) {
            if (httpContentType.htmlString.equalsIgnoreCase(string)) return httpContentType;
        }
        return null;
    }

    @Override
    public String toString() {
        return this.htmlString;
    }
}
