package sws;

import static sws.http.config.ServerConfig.MAX_CONCURRENT_CONNECTIONS;
import static sws.http.config.ServerConfig.MAX_REQUEST_SIZE_BYTES;
import static sws.http.config.ServerConfig.PORT;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import sws.http.HttpRequest;
import sws.http.HttpResponse;
import sws.io.EventLoopController;
import sws.io.IOEvent;
import sws.io.Log;
import sws.io.OperationContext;
import sws.io.OperationContext.Handler;

public class Server {
    public final int backlog;
    public final boolean useGzip;
    private final Function<HttpRequest, HttpResponse> handler;
    private final AtomicInteger currentConnections = new AtomicInteger();

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

    class SocketWriteHandler implements Handler {

        @Override
        public void handle(EventLoopController eventLoopController, Selector selector,
                SelectionKey key, OperationContext operationContext) {
            final var channel = (SocketChannel) key.channel();
            final var response = operationContext.attachment;

            try {
                if (response == null) {
                    key.cancel();
                    channel.close();
                    return;
                }

                final var responseBuffer = (ByteBuffer) operationContext.attachment;
                do {
                    channel.write(responseBuffer);
                } while (responseBuffer.hasRemaining());
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            key.cancel();
            currentConnections.decrementAndGet();
        }
    }

    class SocketReadHandler implements Handler {

        @Override
        public void handle(EventLoopController eventLoopController, Selector selector,
                SelectionKey key, OperationContext operationContext) {
            final var channel = (SocketChannel) key.channel();
            try {
                ByteBuffer tempBuffer = ByteBuffer.allocate(256);
                ByteArrayOutputStream fullBuffer;
                if (operationContext.attachment == null) {
                    fullBuffer = new ByteArrayOutputStream();
                    operationContext.attachment = fullBuffer;
                } else {
                    fullBuffer = (ByteArrayOutputStream) operationContext.attachment;
                }

                int readSize, totalReadSize = fullBuffer.size();
                while ((readSize = channel.read(tempBuffer)) > 0) {
                    totalReadSize += readSize;

                    if (totalReadSize > MAX_REQUEST_SIZE_BYTES) {
                        key.cancel();
                        channel.close();
                        return;
                        // throw new IOException("Request too big");
                    }

                    tempBuffer.flip();
                    fullBuffer.write(tempBuffer.array(), 0, tempBuffer.limit());
                    tempBuffer.clear();
                }

                // TODO: proper handling
                // if (readSize == 0) { // didn't reach end of stream, reschedule
                // Log.e("here we fail");
                // return;
                // }

                try {
                    final var httpRequest = new HttpRequest(fullBuffer);
                    final var httpResponse = handler.apply(httpRequest);
                    operationContext.handler = new SocketWriteHandler();
                    operationContext.attachment = httpResponse.getResponse();
                    key.cancel();
                    eventLoopController.pushEvent(
                            new IOEvent(channel, SelectionKey.OP_WRITE, operationContext));

                } catch (Exception e) {
                    key.cancel();
                    channel.close();
                    e.printStackTrace();
                }

            } catch (IOException e) {
                key.cancel();
                e.printStackTrace();
            }
        }
    }

    class SocketAcceptHandler implements Handler {

        @Override
        public void handle(EventLoopController eventLoopController, Selector selector,
                SelectionKey key, OperationContext operationContext) {
            if (currentConnections.get() >= MAX_CONCURRENT_CONNECTIONS) {
                return;
            }

            try {
                key.channel().configureBlocking(false);
            } catch (IOException e) {
                e.printStackTrace();
            }

            final var channel = (ServerSocketChannel) key.channel();
            try {
                eventLoopController.pushEvent(new IOEvent(channel.accept().configureBlocking(false),
                        SelectionKey.OP_READ, new OperationContext(new SocketReadHandler(), null)));
                currentConnections.incrementAndGet();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startHttp() throws Exception {
        final int nCores = Runtime.getRuntime().availableProcessors();
        final var threads = new Thread[nCores];
        final var eventLoopController = new EventLoopController(nCores);
        final var channel = ServerSocketChannel.open()
                .bind(new InetSocketAddress("localhost", PORT)).configureBlocking(false);

        for (var i = 0; i < nCores; ++i) {
            threads[i] = new Thread(eventLoopController.createEventLoop());
            threads[i].start();
        }

        eventLoopController.pushEvent(new IOEvent(channel, SelectionKey.OP_ACCEPT,
                new OperationContext(new SocketAcceptHandler(), null)));

        Log.s("Server running at :" + PORT);
        for (var thread : threads)
            thread.join();
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion,
            String KeyStoreType, String KeyManagerFactoryType) throws Exception {
        System.setProperty("jdk.tls.ephemeralDHKeySize", "2048"); // LOGJAM TLS Attack
        System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true"); // Client
                                                                                  // Renegotiation
                                                                                  // Attack
        final int nCores = Runtime.getRuntime().availableProcessors();
        final var threads = new Thread[nCores];
        final var eventLoopController = new EventLoopController(nCores);

        final var serverSocketFactory =
                getSSLContext(Path.of(KeyStorePath), KeyStorePassword.toCharArray(), TLSVersion,
                        KeyStoreType, KeyManagerFactoryType).getServerSocketFactory();
        final var channel = serverSocketFactory.createServerSocket(PORT, backlog).getChannel();

        for (var i = 0; i < nCores; ++i) {
            threads[i] = new Thread(eventLoopController.createEventLoop());
            threads[i].start();
        }

        eventLoopController.pushEvent(new IOEvent(channel, SelectionKey.OP_ACCEPT,
                new OperationContext(new SocketAcceptHandler(), null)));

        Log.s("Server running at :" + PORT);
        for (var thread : threads)
            thread.join();
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword) throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, "TLSv1.3", "JKS", "SunX509");
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion)
            throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, TLSVersion, "JKS", "SunX509");
    }

    public void startHttps(String KeyStorePath, String KeyStorePassword, String TLSVersion,
            String KeyStoreType) throws Exception {
        startHttps(KeyStorePath, KeyStorePassword, TLSVersion, KeyStoreType, "SunX509");
    }

    private SSLContext getSSLContext(Path keyStorePath, char[] keyStorePass, String TLSVersion,
            String KeyStoreType, String KeyManagerFactoryType) throws Exception {
        var keyStore = KeyStore.getInstance(KeyStoreType);
        keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePass);
        var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactoryType);
        keyManagerFactory.init(keyStore, keyStorePass);
        var sslContext = SSLContext.getInstance(TLSVersion);
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }
}
