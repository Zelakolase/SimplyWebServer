package http;

import http.config.HttpStatusCode;

import java.nio.ByteBuffer;
import java.util.HashMap;


public abstract class HttpResponse {
    public final HashMap<String, String> headers = new HashMap<>();

    protected HttpStatusCode httpStatusCode;
    protected HttpContentType httpContentType;
    protected boolean isFile = false;
    protected boolean useGzip = false;
    protected ByteBuffer buffer = null;

    public boolean isFileResponse() {
        return isFile;
    }

    public void setHttpStatusCode(HttpStatusCode httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public abstract ByteBuffer getResponse();

}
