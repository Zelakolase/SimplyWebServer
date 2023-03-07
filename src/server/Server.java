package server;

import http.HttpContentType;
import http.HttpRequest;
import http.HttpResponse;
import http.HttpStatusCode;
import lib.Network;
import lib.SparkDB;
import lib.log;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Server {
    public static final String HTTP_PROTO_VERSION = "HTTP/1.1";

    public final int port;
    public final int maxRequestSizeBytes;
    public final int maxConcurrentRequests;
    public final int backlog;
    public final boolean useGzip;
    public final String rootDir;
    public final String mimeFile;

    private final SparkDB MIME = new SparkDB();
    private final Function<HttpRequest, HttpResponse> handler;


    public Server(Function<HttpRequest, HttpResponse> handler) {
        this(8080, false, handler);
    }

    public Server(int port, boolean useGzip, Function<HttpRequest, HttpResponse> handler) {
        this(port, 100000, 4096, 4096, useGzip, handler);
    }

    public Server(int port, int maxConcurrentRequests, int maxRequestSizeBytes, int maxResponseSizeBytes, boolean useGzip, Function<HttpRequest, HttpResponse> handler) {
        this(port, maxConcurrentRequests, maxRequestSizeBytes, useGzip, "www", "etc/MIME.db", handler);
    }

    public Server(int port, int maxConcurrentRequests, int maxRequestSizeBytes, boolean useGzip, String rootDir, String mimeFile, Function<HttpRequest, HttpResponse> handler) {
        this.port = port;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.backlog = maxConcurrentRequests * 5;
        this.maxRequestSizeBytes = maxRequestSizeBytes;
        this.useGzip = useGzip;
        this.rootDir = rootDir;
        this.mimeFile = mimeFile;
        this.handler = handler;
    }


    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    private void mainLoop(ServerSocket serverSocket, ThreadPoolExecutor poolExecutor) {
        while (true) {
            try {
                Socket s = serverSocket.accept();
                s.setKeepAlive(true);
                s.setTcpNoDelay(true);
                s.setReceiveBufferSize(maxRequestSizeBytes);
                s.setSendBufferSize(maxRequestSizeBytes);
                s.setSoTimeout(60000);

                try {
                    poolExecutor.execute(new Engine(s));
                } catch (RejectedExecutionException ignore) {
                    log.i("concurrent connections exceed the configured maximum");
                    Network.write(new BufferedOutputStream(s.getOutputStream()), new HttpResponse("", HttpStatusCode.SERVICE_UNAVAILABLE, HttpContentType.TEXT_PLAIN), false);
                }

            } catch (IOException e) {
                log.e(e.getMessage());
                return;
            }
        }
    }

    public void startHttp() throws Exception {
        loadMIME();
        final int nCores = Runtime.getRuntime().availableProcessors();
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(maxConcurrentRequests);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(nCores, nCores, 60, TimeUnit.SECONDS, workQueue);
        try (ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            log.s("Server running at :" + port);
            mainLoop(serverSocket, poolExecutor);
        } finally {
            poolExecutor.shutdownNow();
        }
    }

    private void loadMIME() throws Exception {
        MIME.readFromFile(mimeFile);
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword) throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, "TLSv1.3", "JKS", "SunX509");
    }


    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion) throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, TLSVersion, "JKS", "SunX509");
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion, String KeyStoreType) throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, TLSVersion, KeyStoreType, "SunX509");
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion, String KeyStoreType, String KeyManagerFactoryType) throws Exception {
        System.setProperty("jdk.tls.ephemeralDHKeySize", "2048"); // Mitigation against LOGJAM TLS Attack
        System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true"); // Mitigation against Client Renegotiation Attack
        loadMIME();
        final int nCores = Runtime.getRuntime().availableProcessors();
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(maxConcurrentRequests);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(nCores, nCores, 60, TimeUnit.SECONDS, workQueue);
        try (ServerSocket serverSocket = getSSLContext(Path.of(KeyStorePath), KeyStorePassword.toCharArray(), TLSVersion, KeyStoreType, KeyManagerFactoryType).getServerSocketFactory().createServerSocket(port, backlog)) {
            log.s("Server running at :" + port);
            mainLoop(serverSocket, poolExecutor);
        } finally {
            poolExecutor.shutdownNow();
        }
    }

    private SSLContext getSSLContext(Path keyStorePath, char[] keyStorePass, String TLSVersion, String KeyStoreType, String KeyManagerFactoryType) throws Exception {
        var keyStore = KeyStore.getInstance(KeyStoreType);
        keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePass);
        var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactoryType);
        keyManagerFactory.init(keyStore, keyStorePass);
        var sslContext = SSLContext.getInstance(TLSVersion);
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }


    public class Engine implements Runnable {
        private final Socket s;

        public Engine(Socket s) {
            this.s = s;
        }

        @Override
        public void run() {
            BufferedInputStream bufferedInputStream;
            BufferedOutputStream bufferedOutputStream = null;
            try {
                bufferedInputStream = new BufferedInputStream(s.getInputStream(), maxRequestSizeBytes);
                bufferedOutputStream = new BufferedOutputStream(s.getOutputStream(), maxRequestSizeBytes);
                Network.write(bufferedOutputStream, handler.apply(new HttpRequest(Network.read(bufferedInputStream, maxRequestSizeBytes), Server.this)), useGzip);
            } catch (Exception e) {
                // If you're building a highly-secured system, it is highly recommended to change getStackTrace(e) to something else
                Network.write(bufferedOutputStream, new HttpResponse(getStackTrace(e), HttpStatusCode.NO_CONTENT, HttpContentType.TEXT_PLAIN), false);
            }
        }
    }
}
