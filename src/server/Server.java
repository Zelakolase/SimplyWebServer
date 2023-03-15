package server;

import http.HttpRequest;
import http.HttpResponse;
import http.exceptions.HttpRequestException;
import lib.SparkDB;
import lib.log;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.function.Function;

import static http.ServerConfig.*;

public class Server {

    public final int backlog;
    public final boolean useGzip;

    private final SparkDB MIME = new SparkDB();
    private final Function<HttpRequest, HttpResponse> handler;


    public Server(Function<HttpRequest, HttpResponse> handler) {
        this(handler, false);
    }

    public Server(Function<HttpRequest, HttpResponse> handler, boolean useGzip) {
        this.backlog = MAX_CONCURRENT_CONNECTIONS * 5;
        this.useGzip = useGzip;
        this.handler = handler;
    }

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    private void mainLoop(Selector selector, int maxConcurrentConnections) {
        int currentConnections = 0;

        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> keysIterator = selector.selectedKeys().iterator();

                while (keysIterator.hasNext()) {
                    SelectionKey key = keysIterator.next();

                    if (!key.isValid()) {
                        continue;
                    } else if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        if (socketChannel != null) {
                            if (++currentConnections > maxConcurrentConnections) {
                                socketChannel.close();
                                --currentConnections;
                                log.e("maximum concurrent connections reached");
                            }

                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ);
                        }
                    } else if (key.isReadable()) {
                        keysIterator.remove();
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        socketChannel.socket().setOption(StandardSocketOptions.SO_KEEPALIVE, KEEP_ALIVE).setOption(StandardSocketOptions.TCP_NODELAY, TCP_NODELAY).setOption(StandardSocketOptions.SO_RCVBUF, MAX_REQUEST_SIZE_BYTES).setOption(StandardSocketOptions.SO_SNDBUF, MAX_RESPONSE_SIZE_BYTES);

                        ByteBuffer buffer = ByteBuffer.allocate(MAX_REQUEST_SIZE_BYTES + 1);
                        if (socketChannel.read(buffer) > MAX_REQUEST_SIZE_BYTES) {
                            socketChannel.close();
                            --currentConnections;
                            throw new IOException("Request too big");
                        }
                        buffer.flip();
                        if (buffer.limit() == 0) {
                            socketChannel.close();
                            --currentConnections;
                            continue;
                        }

                        HttpRequest httpRequest;
                        try {
                            if (key.attachment() != null) {
                                httpRequest = (HttpRequest) key.attachment();
                                httpRequest.appendBuffer(buffer);
                            } else {
                                httpRequest = new HttpRequest(buffer);
                            }
                        } catch (HttpRequestException exception) {
                            socketChannel.close();
                            --currentConnections;
                            log.e(getStackTrace(exception));
                            continue;
                        }

                        if (httpRequest.getHeaders().containsKey("content-length")) {
                            int contentLength = Integer.parseInt(httpRequest.getHeaders().get("content-length"), 10);
                            if (contentLength > MAX_REQUEST_SIZE_BYTES - httpRequest.getHeaderSize()) {
                                socketChannel.close();
                                --currentConnections;
                                throw new IOException("Request too big");
                            }

                            if (httpRequest.getBodySize() < contentLength) {
                                socketChannel.register(selector, SelectionKey.OP_READ, httpRequest);
                                continue;
                            }
                        }

                        try {
                            socketChannel.register(selector, SelectionKey.OP_WRITE, handler.apply(httpRequest));
                        } catch (ClosedChannelException e) {
                            throw new RuntimeException(e);
                        }
                        // log.i("concurrent connections exceed the configured maximum");
                    } else if (key.isWritable()) {
                        keysIterator.remove();
                        try (SocketChannel socketChannel = (SocketChannel) key.channel()) {
                            socketChannel.write(((HttpResponse) key.attachment()).getHttpResponseBuffer(useGzip));
                            --currentConnections;
                        }
                    }


                }
            } catch (IOException e) {
                log.e(getStackTrace(e));
            }
        }
    }

    public void startHttp() throws Exception {
        loadMIME();
        final int nCores = Runtime.getRuntime().availableProcessors();
        final Thread[] threads = new Thread[nCores];

        log.s("Server running at :" + PORT);
        for (int i = 0; i < nCores; ++i) {
            threads[i] = new Thread(() -> {
                try (Selector selector = Selector.open(); ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
                    serverSocketChannel.bind(new InetSocketAddress(PORT), backlog);
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                    mainLoop(selector, MAX_CONCURRENT_CONNECTIONS / nCores);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads)
            thread.join();
    }

    private void loadMIME() throws Exception {
        MIME.readFromFile(MIME_DB);
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
        final Thread[] threads = new Thread[nCores];

        log.s("Server running at :" + PORT);
        for (int i = 0; i < nCores; ++i) {
            threads[i] = new Thread(() -> {
                try (Selector selector = Selector.open(); ServerSocketChannel serverSocketChannel = getSSLContext(Path.of(KeyStorePath), KeyStorePassword.toCharArray(), TLSVersion, KeyStoreType, KeyManagerFactoryType).getServerSocketFactory().createServerSocket(PORT, backlog).getChannel()) {
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
                    serverSocketChannel.bind(new InetSocketAddress(PORT), backlog);
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                    mainLoop(selector, MAX_CONCURRENT_CONNECTIONS / nCores);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads)
            thread.join();
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
}
