package http.config;

import lib.SparkDB;

public interface ServerConfig {
    int PORT = 8080;
    int MAX_REQUEST_SIZE_BYTES = 1073741824; // 1GB
    int MAX_RESPONSE_SIZE_BYTES = 1073741824; // 1GB
    int MAX_CONCURRENT_CONNECTIONS = 10_000;

    boolean KEEP_ALIVE = false;
    boolean TCP_NODELAY = true;

    String ROOT_DIR = "www";
    String MIME_DB = "src/etc/MIME.db";
    SparkDB MIME = new SparkDB(MIME_DB);

    String HTTP_PROTO_VERSION = "HTTP/1.1";
}
