package http;

import http.config.HttpStatusCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static http.config.ServerConfig.HTTP_PROTO_VERSION;


public abstract class HttpResponse {
    protected final HashMap<String, String> headers = new HashMap<>();
    protected HttpStatusCode httpStatusCode;
    protected String httpContentType;
    protected boolean useGzip = false;

    public static void setBufferWithHeader(ByteBuffer buffer, HttpStatusCode httpStatusCode, String httpContentType,
                                           HashMap<String, String> headers) throws BufferOverflowException {
        buffer.put(HTTP_PROTO_VERSION.getBytes());
        buffer.put((byte) ' ');
        buffer.put(String.valueOf(httpStatusCode.getHttpCode()).getBytes());
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

    public static void setBufferWithHeader(ByteArrayOutputStream buffer, HttpStatusCode httpStatusCode,
                                           String httpContentType,
                                           HashMap<String, String> headers) throws BufferOverflowException, IOException {
        buffer.write(HTTP_PROTO_VERSION.getBytes());
        buffer.write((byte) ' ');
        buffer.write(String.valueOf(httpStatusCode.getHttpCode()).getBytes());
        buffer.write((byte) ' ');
        buffer.write(httpStatusCode.toString().getBytes());
        buffer.write("\r\n".getBytes());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            buffer.write(entry.getKey().toLowerCase().getBytes());
            buffer.write(": ".getBytes());
            buffer.write(entry.getValue().toLowerCase().getBytes());
            buffer.write("\r\n".getBytes());
        }
        buffer.write("content-type: ".getBytes());
        buffer.write(httpContentType.getBytes());
        buffer.write("\r\n".getBytes());
    }

    public abstract boolean isFileResponse();

    public void setHttpStatusCode(HttpStatusCode httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public abstract boolean hasResponse();

    public abstract ByteBuffer getResponse() throws Exception;

    public abstract void addHeader(String header, String value);

    public abstract void deleteHeader(String header);

}
