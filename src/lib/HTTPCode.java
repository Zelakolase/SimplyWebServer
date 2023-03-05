package lib;

public class HTTPCode {
    public static String OK = "HTTP/1.1 200 OK";
    public static String CREATED = "HTTP/1.1 201 Created";
    public static String NO_CONTENT = "HTTP/1.1 204 No Content";
    public static String PARTIAL_CONTENT = "HTTP/1.1 206 Partial Content";
    public static String MOVED_PERM = "HTTP/1.1 301 Moved Permanently";
    public static String FOUND = "HTTP/1.1 302 Found";
    public static String BAD_REQUEST = "HTTP/1.1 400 Bad Request";
    public static String UNAUTHORIZED = "HTTP/1.1 401 Unauthorized";
    public static String FORBIDDEN = "HTTP/1.1 403 Forbidden";
    public static String NOT_FOUND = "HTTP/1.1 404 Not Found";
    public static String METHOD_NOT_ALLOWED = "HTTP/1.1 405 Method Not Allowed";
    public static String REQUEST_TIMEOUT = "HTTP/1.1 408 Request Timeout";
    public static String URI_TOO_LONG = "HTTP/1.1 414 URI Too Long";
    public static String PAYLOAD_TOO_LARG = "HTTP/1.1 413 Payload Too Large";
    public static String TOO_MANY_REQUESTS = "HTTP/1.1 429 Too Many Requests";
    public static String INTERNAL_SERVER_ERROR = "HTTP/1.1 500 Internal Server Error";
    public static String BAD_GATEWAY = "HTTP/1.1 502 Bad Gateway";
    public static String SERVICE_UNAVAILABLE = "HTTP/1.1 503 Service Unavailable";
}
