package http.config;
/*
 * Contains status codes for HTTP(S) protocol.
 * @author Omar M. K. and Morad A.
 * @version 1.0
 */
public enum HttpStatusCode {
    OK(200, "OK"), CREATED(201, "Created"), NO_CONTENT(204, "No Content"), PARTIAL_CONTENT(206, "Partial Content"),
    MOVED_PERMANENTLY(301, "Moved Permanently"), FOUND(302, "Found"), BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"), FORBIDDEN(403, "Forbidden"), NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"), REQUEST_TIMEOUT(408, "Request Timeout"),
    URI_TOO_LONG(404, "URI Too Long"), PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"), INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    BAD_GATEWAY(502, "Bad Gateway"), SERVICE_UNAVAILABLE(503, "Service Unavailable");

    /* The HTTP Status Code */
    private final int httpCode;
    /* The HTTP Status as String */
    private final String httpString;

    /*
     * A constructor to set custom HTTP Code
     * @param httpCode      HTTP Status Code
     * @param httpString    HTTP Status String
     */
    HttpStatusCode(int httpCode, String httpString) {
        this.httpCode = httpCode;
        this.httpString = httpString;
    }

    /*
     * Get current HTTP Status Code
     * @return      HTTP Code as number
     */
    public int getHttpCode() {
        return this.httpCode;
    }

    /*
     * Get current HTTP Status Code as String, overrides default Object.toString().
     * @return      HTTP Code as String
     */
    @Override
    public String toString() {
        return this.httpString;
    }

}
