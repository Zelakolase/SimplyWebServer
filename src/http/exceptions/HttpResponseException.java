package http.exceptions;

/*
 * HTTP Response Exception, used in HTTP Response-related errors.
 * @author Omar M. K. and Morad A.
 * @version 1.0
 */
public class HttpResponseException extends Exception {
    /* Error Message in String */
    private final String errorMsg;

    /* 
     * Constructor for HttpResponseException, set the errorMsg global variable.
     * @param errorMsg      The input Error Message
     */
    public HttpResponseException(String errorMsg) {
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
