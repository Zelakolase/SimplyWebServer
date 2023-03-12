package lib;

import http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static server.Server.HTTP_PROTO_VERSION;

public class Network {
    /**
     * GZIP Compression
     *
     * @param data data to compress in bytes
     * @return compressed data in bytes
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data);
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }

    /**
     * HTTP Write Response Function
     *
     * @param outputStream data stream to write to
     * @param httpResponse the http response
     * @param gzip         whether to compress the body using gzip or not
     */
    public static void write(BufferedOutputStream outputStream, HttpResponse httpResponse, boolean gzip) {
        StringBuilder temp = new StringBuilder(1024);
        temp.append(HTTP_PROTO_VERSION);
        temp.append(' ');
        temp.append(httpResponse.getHttpStatusCode().getHtmlCode());
        temp.append(' ');
        temp.append(httpResponse.getHttpStatusCode().toString());
        temp.append("\r\n");
        httpResponse.getHeaders().forEach((key, value) -> {
            temp.append(key.toLowerCase());
            temp.append(": ");
            temp.append(value.toLowerCase());
            temp.append("\r\n");
        });
        temp.append("content-type: ");
        temp.append(httpResponse.getHttpContentType());
        temp.append("\r\n");

        try {
            if (gzip) {
                byte[] body = compress(httpResponse.getBody().getBytes());
                temp.append("content-encoding: gzip\r\n");
                temp.append("content-length: ");
                temp.append(body.length);
                temp.append("\r\n\r\n");
                outputStream.write(temp.toString().getBytes());
                outputStream.write(body);
            } else {
                temp.append("content-length: ");
                temp.append(httpResponse.getBody().length());
                temp.append("\r\n\r\n");
                temp.append(httpResponse.getBody());
                outputStream.write(temp.toString().getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            log.e(e, Network.class.getName(), "write");
        }
    }

    /**
     * Reads a bare request from a socket
     *
     * @param maxReqSize the maximum bytes to read
     * @return String representing the request
     */
    public static String read(BufferedInputStream inputStream, int maxReqSize) throws IOException {
        byte[] buffer = new byte[maxReqSize];
        if (inputStream.read(buffer, 0, maxReqSize) == maxReqSize) {
            throw new IOException("Request to big");
        }
        return new String(buffer);

    }

    /**
     * Mandatory Read (used rarely)
     */
    public static byte[] ManRead(BufferedInputStream dIS, int bytesToRead) {
        try {
            return dIS.readNBytes(bytesToRead);
        } catch (Exception ignored) {
        }
        return null;
    }
}
