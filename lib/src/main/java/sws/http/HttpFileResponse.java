package sws.http;

import static sws.http.config.ServerConfig.*;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import sws.http.config.HttpStatusCode;
import sws.http.exceptions.HttpResponseException;
import sws.io.Log;
import sws.utils.Utils;

public class HttpFileResponse extends HttpResponse {
    private final FileChannel fileChannel;
    private final Path filePath;
    private final ByteBuffer body = ByteBuffer.allocateDirect(MAX_FILE_CHUNK_SIZE_BYTES);
    private boolean hasResponse = true;
    private boolean isHeaderSent = false;
    private int headerSize = 64; // enough to handle protocol version & status code

    public HttpFileResponse(String filePathString) throws IOException, HttpResponseException {
        this(filePathString, HttpStatusCode.OK, null, false, new HashMap<>());
    }

    public HttpFileResponse(String filePathString, HttpStatusCode httpStatusCode,
            String httpContentType, boolean useGzip, HashMap<Object, Object> headers)
            throws IOException, HttpResponseException {
        this.filePath = Paths.get(ROOT_DIR, filePathString);
        this.fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);

        if (!filePath.startsWith(ROOT_DIR))
            throw new HttpResponseException("request file is outside webserver root directory");

        this.httpStatusCode = httpStatusCode;

        if (httpContentType == null) {
            try {
                String[] tempSplit = filePathString.split("\\.");
                this.httpContentType = MIME.get(new HashMap<>() {
                    {
                        put("extension", tempSplit[tempSplit.length - 1]);
                    }
                }, "mime", 1).get(0);
            } catch (Exception e) {
                Log.e("failed to find mime in database");
                String contentType = Files.probeContentType(filePath);
                this.httpContentType = contentType == null ? "text/html" : contentType;
            }
        }

        this.headerSize += this.httpContentType.length();
        this.useGzip = useGzip;

        headers.forEach((key, value) -> headers.put(key.toString(), value.toString()));
    }

    void setHttpContentType(String httpContentType) {
        this.headerSize -= this.httpContentType.length();
        this.httpContentType = httpContentType;
        this.headerSize += httpContentType.length();
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
        if (fileChannel.read(this.body) == -1)
            hasResponse = false;
        this.body.flip();
        try {
            if (useGzip) {
                ByteBuffer compressedBody = Utils.compress(this.body);
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
            Log.e("failed to compress response body");
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

    @Override
    public void addHeader(String header, String value) {
        // headerSize = header.length() + ": " + value.length() + "\r\n"
        headerSize += header.length() + 2 + value.length() + 2;
        headers.put(header, value);
    }

    @Override
    public void deleteHeader(String header) {
        if (headers.containsKey(header)) {
            headerSize -= header.length() + headers.get(header).length();
            headers.remove(header);
        }
    }
}
