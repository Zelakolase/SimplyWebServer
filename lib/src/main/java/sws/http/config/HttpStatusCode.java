package sws.http.config;

public enum HttpStatusCode {
  OK(200, "OK"),
  CREATED(201, "Created"),
  NO_CONTENT(204, "No Content"),
  PARTIAL_CONTENT(206, "Partial Content"),
  MOVED_PERMANENTLY(301, "Moved Permanently"),
  FOUND(302, "Found"),
  BAD_REQUEST(400, "Bad Request"),
  UNAUTHORIZED(401, "Unauthorized"),
  FORBIDDEN(403, "Forbidden"),
  NOT_FOUND(404, "Not Found"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
  REQUEST_TIMEOUT(408, "Request Timeout"),
  URI_TOO_LONG(404, "URI Too Long"),
  PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
  TOO_MANY_REQUESTS(429, "Too Many Requests"),
  INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
  BAD_GATEWAY(502, "Bad Gateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable");

  private final int htmlCode;
  private final String htmlString;

  HttpStatusCode(int htmlCode, String htmlString) {
    this.htmlCode = htmlCode;
    this.htmlString = htmlString;
  }

  public int getHtmlCode() { return this.htmlCode; }

  @Override
  public String toString() {
    return this.htmlString;
  }
}
