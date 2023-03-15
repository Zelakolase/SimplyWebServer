package http;

import http.config.HttpStatusCode;
import http.exceptions.HttpResponseException;
import lib.log;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

import static http.config.ServerConfig.*;
import static lib.Network.compress;

public class HttpFileResponse extends HttpResponse {
    private final FileChannel fileChannel;
    private final boolean isFile = true;
    private final Path filePath;

    public HttpFileResponse(String filePathString) throws IOException, HttpResponseException {
        this(filePathString, HttpStatusCode.OK, HttpContentType.TEXT_HTML, false, new HashMap<>());
    }


    public HttpFileResponse(String filePathString, HttpStatusCode httpStatusCode, HttpContentType httpContentType,
                            boolean useGzip,
                            HashMap<Object, Object> headers) throws IOException, HttpResponseException {
        this.filePath = Paths.get(ROOT_DIR, filePathString);
        this.fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);

        if (!filePath.startsWith(ROOT_DIR)) {
            throw new HttpResponseException("request file is outside webserver root directory");
        }

        this.httpStatusCode = httpStatusCode;
        this.httpContentType = HttpContentType.fromString(Files.probeContentType(filePath));
        this.useGzip = useGzip;

        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    void setHttpContentType(HttpContentType httpContentType) {
        this.httpContentType = httpContentType;
    }

    @Override
    public ByteBuffer getResponse() {
        int fileSize = 0;
        try {
            fileSize = (int) Files.size(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ByteBuffer fileBuffer = ByteBuffer.allocateDirect(fileSize);
        ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_RESPONSE_SIZE_BYTES + fileSize);

        try {
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

            if (this.httpContentType != null) {
                buffer.put("content-type: ".getBytes());
                buffer.put(this.httpContentType.toString().getBytes());
                buffer.put("\r\n".getBytes());
            }

            fileChannel.read(fileBuffer);
            fileBuffer.flip();
            if (useGzip) {
                ByteBuffer compressedBody = compress(fileBuffer);
                buffer.put("content-encoding: gzip\r\n".getBytes());
                buffer.put("content-length: ".getBytes());
                buffer.put(String.valueOf(compressedBody.limit()).getBytes());
                buffer.put("\r\n\r\n".getBytes());
                buffer.put(compressedBody);
            } else {
                buffer.put("content-length: ".getBytes());
                buffer.put(String.valueOf(fileBuffer.limit()).getBytes());
                buffer.put("\r\n\r\n".getBytes());
                buffer.put(fileBuffer);
            }
        } catch (IOException ignored) {
            log.e("failed to compress response body");
            buffer.put("content-length: ".getBytes());
            buffer.put(String.valueOf(fileBuffer.limit()).getBytes());
            buffer.put("\r\n\r\n".getBytes());
            buffer.put(fileBuffer);
        } catch (BufferOverflowException e) {
            this.httpStatusCode = HttpStatusCode.INTERNAL_SERVER_ERROR;
            buffer.position(headers.size());
            return buffer;
        }

        buffer.flip();
        return buffer;
    }
}
