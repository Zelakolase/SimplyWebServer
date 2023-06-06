package http.exceptions;

/*
 * HTTP Request Exception. Used in HTTP Request-related errors.
 * @author Omar M. K. and Morad A.
 * @version 1.0
 */
public class HttpRequestException extends Exception {
    /* Error Message in String */
    private final String errorMsg;

    /* 
     * Constructor for HttpRequestException, set the errorMsg global variable.
     * @param errorMsg      The input Error Message
     */
    public HttpRequestException(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /*
     * Gets the error message, overrides default Object.toString().
     * @returns             The current stored error message
     */
    @Override
    public String getMessage() {
        return errorMsg;
    }
}
