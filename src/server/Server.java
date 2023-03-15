package server;

import http.HttpRequest;
import http.HttpResponse;
import http.exceptions.HttpRequestException;
import io.KeyAttachment;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static http.config.ServerConfig.*;

public class Server {
    public final int backlog;
    public final boolean useGzip;
    private final Function<HttpRequest, HttpResponse> handler;
    private final AtomicInteger currentConnections = new AtomicInteger();
    private final ThreadLocal<Selector> selectorThreadLocal = new ThreadLocal<>();


    public Server(Function<HttpRequest, HttpResponse> handler) {
        this(handler, false);
    }

    public Server(Function<HttpRequest, HttpResponse> handler, boolean useGzip) {
        this.backlog = MAX_CONCURRENT_CONNECTIONS * 5;
        this.useGzip = useGzip;
        this.handler = handler;
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    private void init(Selector selector, ServerSocketChannel serverSocketChannel) {
        selectorThreadLocal.set(selector);
        try {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
            serverSocketChannel.bind(new InetSocketAddress(PORT), backlog);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Void handleWriteRequest(KeyAttachment.HandlerArgs handlerArgs) {
        try (SocketChannel socketChannel = (SocketChannel) handlerArgs.channel) {
            HttpResponse response = handler.apply(((HttpRequest) handlerArgs.keyAttachment.attachment()));
            socketChannel.write(response.getResponse());
            currentConnections.decrementAndGet();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Void handleReadRequest(KeyAttachment.HandlerArgs handlerArgs) {
        SocketChannel socketChannel = (SocketChannel) handlerArgs.channel;
        try {
            socketChannel.socket()
                         .setOption(StandardSocketOptions.SO_KEEPALIVE, KEEP_ALIVE)
                         .setOption(StandardSocketOptions.TCP_NODELAY, TCP_NODELAY)
                         .setOption(StandardSocketOptions.SO_RCVBUF, MAX_REQUEST_SIZE_BYTES)
                         .setOption(StandardSocketOptions.SO_SNDBUF, MAX_RESPONSE_SIZE_BYTES);

            ByteBuffer buffer = ByteBuffer.allocate(MAX_REQUEST_SIZE_BYTES + 1);
            if (socketChannel.read(buffer) > MAX_REQUEST_SIZE_BYTES) {
                socketChannel.close();
                currentConnections.decrementAndGet();
                throw new IOException("Request too big");
            }
            buffer.flip();
            if (buffer.limit() == 0) {
                socketChannel.close();
                currentConnections.decrementAndGet();
                return null;
            }


            HttpRequest httpRequest;
            try {
                if (handlerArgs.keyAttachment.attachment() != null) {
                    httpRequest = (HttpRequest) handlerArgs.keyAttachment.attachment();
                    httpRequest.appendBuffer(buffer);
                } else {
                    httpRequest = new HttpRequest(buffer);
                }
            } catch (HttpRequestException e) {
                socketChannel.close();
                currentConnections.decrementAndGet();
                log.e(getStackTrace(e));
                return null;
            }
            handlerArgs.keyAttachment.attach(httpRequest);

            try {
                if (httpRequest.getHeaders().containsKey("content-length")) {
                    int contentLength = Integer.parseInt(httpRequest.getHeaders().get("content-length"), 10);
                    if (contentLength > MAX_REQUEST_SIZE_BYTES - httpRequest.getHeaderSize()) {
                        socketChannel.close();
                        currentConnections.decrementAndGet();
                        throw new IOException("Request too big");
                    }

                    if (httpRequest.getBodySize() < contentLength) {
                        socketChannel.register(handlerArgs.selector, SelectionKey.OP_READ, handlerArgs.keyAttachment);
                        return null;
                    }
                }
            } catch (NumberFormatException e) {
                socketChannel.close();
                currentConnections.decrementAndGet();
                log.e(getStackTrace(e));
                return null;
            }

            try {
                handlerArgs.keyAttachment.setHandler(this::handleWriteRequest);
                socketChannel.register(handlerArgs.selector, SelectionKey.OP_WRITE, handlerArgs.keyAttachment);
            } catch (ClosedChannelException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void mainLoop() {

        while (true) {
            try {
                selectorThreadLocal.get().select();
                Iterator<SelectionKey> keysIterator = selectorThreadLocal.get().selectedKeys().iterator();

                while (keysIterator.hasNext()) {
                    SelectionKey key = keysIterator.next();

                    if (!key.isValid()) {
                        continue;
                    } else if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        if (socketChannel != null) {
                            if (currentConnections.incrementAndGet() > MAX_CONCURRENT_CONNECTIONS) {
                                socketChannel.close();
                                currentConnections.decrementAndGet();
                                log.e("maximum concurrent connections reached");
                            }

                            socketChannel.configureBlocking(false);
                            KeyAttachment keyAttachment = new KeyAttachment(socketChannel, selectorThreadLocal.get(),
                                    this::handleReadRequest);
                            socketChannel.register(selectorThreadLocal.get(), SelectionKey.OP_READ, keyAttachment);
                        }
                    } else if (key.isReadable()) {
                        keysIterator.remove();
                        KeyAttachment keyAttachment = (KeyAttachment) key.attachment();
                        keyAttachment.handle();
                    } else if (key.isWritable()) {
                        keysIterator.remove();
                        KeyAttachment keyAttachment = (KeyAttachment) key.attachment();
                        keyAttachment.handle();
                    }


                }
            } catch (IOException e) {
                log.e(getStackTrace(e));
            }
        }
    }

    public void startHttp() throws Exception {
        final int nCores = Runtime.getRuntime().availableProcessors();
        final Thread[] threads = new Thread[nCores];

        log.s("Server running at :" + PORT);
        for (int i = 0; i < nCores; ++i) {
            threads[i] = new Thread(() -> {
                try (Selector selector = Selector.open(); ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
                    init(selector, serverSocketChannel);
                    mainLoop();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads)
            thread.join();
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword) throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, "TLSv1.3", "JKS", "SunX509");
    }


    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion) throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, TLSVersion, "JKS", "SunX509");
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion,
                           String KeyStoreType) throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, TLSVersion, KeyStoreType, "SunX509");
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion, String KeyStoreType,
                           String KeyManagerFactoryType) throws Exception {
        System.setProperty("jdk.tls.ephemeralDHKeySize", "2048");                   // LOGJAM TLS Attack
        System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true");   // Client Renegotiation Attack
        final int nCores = Runtime.getRuntime().availableProcessors();
        final Thread[] threads = new Thread[nCores];

        log.s("Server running at :" + PORT);
        for (int i = 0; i < nCores; ++i) {
            threads[i] = new Thread(() -> {
                try (Selector selector = Selector.open(); ServerSocketChannel serverSocketChannel = getSSLContext(
                        Path.of(KeyStorePath), KeyStorePassword.toCharArray(), TLSVersion, KeyStoreType,
                        KeyManagerFactoryType).getServerSocketFactory()
                                              .createServerSocket(PORT, backlog)
                                              .getChannel()) {
                    init(selector, serverSocketChannel);
                    mainLoop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads)
            thread.join();
    }

    private SSLContext getSSLContext(Path keyStorePath, char[] keyStorePass, String TLSVersion, String KeyStoreType,
                                     String KeyManagerFactoryType) throws Exception {
        var keyStore = KeyStore.getInstance(KeyStoreType);
        keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePass);
        var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactoryType);
        keyManagerFactory.init(keyStore, keyStorePass);
        var sslContext = SSLContext.getInstance(TLSVersion);
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }
}
