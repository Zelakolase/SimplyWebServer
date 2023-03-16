package http.config;

public interface ServerConfig {
    int PORT = 8080;
    int MAX_REQUEST_SIZE_BYTES = 2048;
    int MAX_RESPONSE_SIZE_BYTES = 4096;
    int MAX_FILE_CHUNK_SIZE_BYTES = 2048;
    int MAX_CONCURRENT_CONNECTIONS = 10000;

    boolean KEEP_ALIVE = false;
    boolean TCP_NODELAY = true;

    String ROOT_DIR = "www";
    String MIME_DB = "src/etc/MIME.db";

    String HTTP_PROTO_VERSION = "HTTP/1.1";
}
