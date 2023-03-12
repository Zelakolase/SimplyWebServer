package http;

public enum HttpContentType {
    APPLICATION_JSON("application/json"), TEXT_HTML("text/html"), TEXT_PLAIN("text/plain");

    private final String htmlString;

    HttpContentType(String htmlString) {
        this.htmlString = htmlString;
    }

    @Override
    public String toString() {
        return this.htmlString;
    }
}
