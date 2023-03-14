package lib;

import http.HttpResponse;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.zip.GZIPOutputStream;

import static http.ServerConfig.*;
import static http.ServerConfig.HTTP_PROTO_VERSION;

public class Network {
    /**
     * GZIP Compression
     *
     * @param data data to compress in bytes
     * @return compressed data in bytes
     */
    public static ByteBuffer compress(ByteBuffer data) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(data.limit(), 1024))) {
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                gzipOutputStream.write(data.array(), 0, data.limit());
            }
            byte[] compressed = outputStream.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(compressed.length);
            buffer.put(compressed);
            buffer.flip();
            return buffer;
        }
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
