package http;

import http.config.HttpStatusCode;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static http.config.ServerConfig.HTTP_PROTO_VERSION;


public abstract class HttpResponse {
    public final HashMap<String, String> headers = new HashMap<>();

    protected HttpStatusCode httpStatusCode;
    protected String httpContentType;
    protected boolean useGzip = false;
    protected ByteBuffer body = null;

    public abstract boolean isFileResponse();

    public void setHttpStatusCode(HttpStatusCode httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public void setBufferWithHeader(ByteBuffer buffer, HttpStatusCode httpStatusCode, String httpContentType,
                                    HashMap<String, String> headers) throws BufferOverflowException {
        buffer.put(HTTP_PROTO_VERSION.getBytes());
        buffer.put((byte) ' ');
        buffer.put(String.valueOf(httpStatusCode.getHtmlCode()).getBytes());
        buffer.put((byte) ' ');
        buffer.put(httpStatusCode.toString().getBytes());
        buffer.put("\r\n".getBytes());
        headers.forEach((key, value) -> {
            buffer.put(key.toLowerCase().getBytes());
            buffer.put(": ".getBytes());
            buffer.put(value.toLowerCase().getBytes());
            buffer.put("\r\n".getBytes());
        });
        buffer.put("content-type: ".getBytes());
        buffer.put(httpContentType.getBytes());
        buffer.put("\r\n".getBytes());
    }

    public abstract boolean hasResponse();

    public abstract ByteBuffer getResponse() throws Exception;

}
