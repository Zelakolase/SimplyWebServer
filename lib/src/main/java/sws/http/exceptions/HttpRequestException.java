package sws.http.exceptions;

public class HttpRequestException extends Exception {
    private final String errorMsg;

    public HttpRequestException(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public String getMessage() {
        return errorMsg;
    }
}
