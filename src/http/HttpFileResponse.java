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

/*
 * HTTP File Response Class. This class represents a single HTTP Response. It is used to send file content,
 * not data.
 * @see HttpBufferResponse
 * @see HttpResponse
 * @author Omar M. K. and Morad A.
 * @version 1.0
 */
public class HttpFileResponse extends HttpResponse {
    /* Used as a file reading channel */
    private final FileChannel fileChannel;
    /* The file path */
    private final Path filePath;
    /* The file content */
    private final ByteBuffer body = ByteBuffer.allocateDirect(MAX_FILE_CHUNK_SIZE_BYTES);
    private final boolean hasResponse = true;
    private boolean isHeaderSent = false;
    /* The header size, the default size is 64 for the first line only. */
    private int headerSize = 64;

    /*
     * The default constructor. Default Status Code is OK with automatic detection of Content Type, no GZip.
     * @param filePathString The path of the file inside ROOT_DIR
     */
    public HttpFileResponse(String filePathString) throws IOException, HttpResponseException {
        this(filePathString, HttpStatusCode.OK, null, false, new HashMap<>());
    }

    /*
     * The full constructor
     */
    public HttpFileResponse(String filePathString, HttpStatusCode httpStatusCode, String httpContentType, boolean useGzip, HashMap<Object, Object> headers) throws IOException, HttpResponseException {
        this.filePath = Paths.get(filePathString);
        this.fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);

        this.httpStatusCode = httpStatusCode;

        if (httpContentType == null) {
            try {
                String[] tempSplit = filePathString.split("\\.");
                this.httpContentType = MIME.get(new HashMap<>() {{
                    put("extension", tempSplit[tempSplit.length - 1]);
                }}, "mime", 1).get(0);
            } catch (Exception e) {
                log.e("failed to find mime in database");
                String contentType = Files.probeContentType(filePath);
                this.httpContentType = contentType == null ? "text/html" : contentType;
            }
        }

        this.headerSize += this.httpContentType.length();
        this.useGzip = useGzip;

        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    /*
     * Sets a custom HTTP Content Type
     */
    void setHttpContentType(String httpContentType) {
        this.headerSize -= this.httpContentType.length();
        this.httpContentType = httpContentType;
        this.headerSize += httpContentType.length();
    }

    /*
     * Check if the response is a file response, always true
     */
    @Override
    public boolean isFileResponse() {
        return true;
    }

    /*
     * Check if the object has a response
     */
    @Override
    public boolean hasResponse() {
        try {
            if (!hasResponse) fileChannel.close();
        } catch (IOException e) {
            return false;
        }
        return hasResponse;
    }

    /*
     * Construct the HTTP Response, then return a ByteBuffer with the response
     */
    @Override
    public ByteBuffer getResponse() throws Exception {
        long fileSize = 0;
        try {
            fileSize = Files.size(filePath);
        } catch (IOException ignored) {}

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
            response.position(headerSize);
            response.flip();
            return response;
        }

        response.flip();
        return response;
    }

    /*
     * Adds an HTTP Header
     */
    @Override
    public void addHeader(String header, String value) {
        // headerSize = header.length() + ": " + value.length() + "\r\n"
        headerSize += header.length() + 2 + value.length() + 2;
        headers.put(header, value);
    }

    /*
     * Removes an HTTP Header based on its key
     */
    @Override
    public void deleteHeader(String header) {
        if (headers.containsKey(header)) {
            headerSize -= header.length() + headers.get(header).length();
            headers.remove(header);
        }
    }
}
