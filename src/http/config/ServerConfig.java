package http.config;

import lib.SparkDB;
/*
 * Global Variables for Server Configuration
 * @author Omar M. K. and Morad A.
 * @version 1.0
 */

public interface ServerConfig {
    /* The Server port. For HTTPS, it should be 443 or 8443. Default is 8080. */
    int PORT = 8080;
    /* The maximum request size in bytes, default is 2 kilobytes. */
    int MAX_REQUEST_SIZE_BYTES = 2048;
    /* The maximum response size in bytes, default is 4 kilobytes. */
    int MAX_RESPONSE_SIZE_BYTES = 4096;
    /* The maximum file chunk size in bytes, default is 2 kilobytes. */
    int MAX_FILE_CHUNK_SIZE_BYTES = 2048;
    /* The maximum requests the server can handle at the same time. Backlog size is MAX_CONCURRENT_CONNECTIONS * 5. */
    int MAX_CONCURRENT_CONNECTIONS = 10000;

    /* Set Keep-Alive mode, default is false. */
    boolean KEEP_ALIVE = false;
    /* Set TCO_NODELAY (Nagle's Algorithm), default is true.
     * @see <a href="https://www.ibm.com/docs/en/tsm/7.1.3?topic=options-tcpnodelay">IBM Description of TCP_NODELAY</a>
     */
    boolean TCP_NODELAY = true;

    /* The folder of frontend files, default is www. */
    String ROOT_DIR = "www";
    /* MIME Types Filepath, default is src/etc/MIME.db. */
    String MIME_DB = "src/etc/MIME.db";
    /* MIME Types Object Declaration. */
    SparkDB MIME = new SparkDB(MIME_DB);
    /* Default index filename */
    String indexFileName = "index.html";

    /* HTTP Protocol Version.
     * @see <a href="https://www.baeldung.com/cs/http-versions">Baeldung Article of different HTTP Versions</a>
     */
    String HTTP_PROTO_VERSION = "HTTP/1.1";
}
