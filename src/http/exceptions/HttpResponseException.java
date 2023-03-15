package http.exceptions;

public class HttpResponseException extends Exception {
    private final String errorMsg;

    public HttpResponseException(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public String getMessage() {
        return errorMsg;
    }
}
