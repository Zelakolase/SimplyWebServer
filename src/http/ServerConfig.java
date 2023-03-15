package http;

public interface ServerConfig {
    int PORT = 8080;
    int MAX_REQUEST_SIZE_BYTES = 4096;
    int MAX_RESPONSE_SIZE_BYTES = 8192;
    int MAX_CONCURRENT_CONNECTIONS = 10_000;

    boolean KEEP_ALIVE = true;
    boolean TCP_NODELAY = true;

    String ROOT_DIR = "www";
    String MIME_DB = "src/etc/MIME.db";

    String HTTP_PROTO_VERSION = "HTTP/1.1";
}