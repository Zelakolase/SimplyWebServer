package http;

import http.config.HttpStatusCode;
import http.exceptions.HttpResponseException;
import lib.IO;
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
    private final Path filePath;
    private final ByteBuffer body = ByteBuffer.allocateDirect(MAX_FILE_CHUNK_SIZE_BYTES);
    private boolean isHeaderSent = false;
    private boolean hasResponse = true;


    public HttpFileResponse(String filePathString) throws IOException, HttpResponseException {
        this(filePathString, HttpStatusCode.OK, null, false, new HashMap<>());
    }


    public HttpFileResponse(String filePathString, HttpStatusCode httpStatusCode, String httpContentType,
                            boolean useGzip,
                            HashMap<Object, Object> headers) throws IOException, HttpResponseException {
        this.filePath = Paths.get(ROOT_DIR, filePathString);
        this.fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);

        if (!filePath.startsWith(ROOT_DIR))
            throw new HttpResponseException("request file is outside webserver root directory");

        this.httpStatusCode = httpStatusCode;

        if (httpContentType == null) {
            String contentType = Files.probeContentType(filePath);
            this.httpContentType = contentType == null ? "text/html" : contentType;
        }

        this.useGzip = useGzip;

        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    void setHttpContentType(String httpContentType) {
        this.httpContentType = httpContentType;
    }

    @Override
    public boolean isFileResponse() {
        return true;
    }

    @Override
    public boolean hasResponse() {
        try {
            if (!hasResponse)
                fileChannel.close();
        } catch (IOException e) {
            return false;
        }
        return hasResponse;
    }

    @Override
    public ByteBuffer getResponse() throws Exception {
        long fileSize = 0;
        try {
            fileSize = Files.size(filePath);
        } catch (IOException ignored) {
        }

        ByteBuffer response = ByteBuffer.allocate(MAX_RESPONSE_SIZE_BYTES);
        if (!isHeaderSent) {
            isHeaderSent = true;
            setBufferWithHeader(response, this.httpStatusCode, this.httpContentType, headers);
        }

        int maxRead = response.capacity() - response.position();
        if (maxRead < this.body.capacity()) {
            this.body.limit(maxRead);
        }
        fileChannel.read(this.body);
        this.body.flip();
        try {
            if (useGzip) {
                ByteBuffer compressedBody = compress(this.body);
                response.put("content-encoding: gzip\r\n".getBytes());
                response.put("\r\n\r\n".getBytes());
                response.put(compressedBody);
            } else {
                if (fileSize != 0) {
                    response.put("content-length: ".getBytes());
                    response.put(String.valueOf(fileSize).getBytes());
                    response.put("\r\n".getBytes());
                }
                response.put("\r\n".getBytes());
                response.put(this.body);
            }
        } catch (IOException ignored) {
            log.e("failed to compress response body");
            if (fileSize != 0) {
                response.put("content-length: ".getBytes());
                response.put(String.valueOf(fileSize).getBytes());
                response.put("\r\n".getBytes());
            }
            response.put("\r\n".getBytes());
            response.put(this.body);
        } catch (BufferOverflowException e) {
            this.httpStatusCode = HttpStatusCode.INTERNAL_SERVER_ERROR;
            response.position(headers.size());
            response.flip();
            return response;
        }

        response.flip();
        return response;
    }
}
