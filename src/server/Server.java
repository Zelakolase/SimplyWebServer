package server;

import lib.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class Server {
    private final SparkDB MIME = new SparkDB();
    private final HashMap<String, String> CustomHeaders = new HashMap<>();
    private int Port = 8080;
    private int MaxConcurrentRequests = 1000;
    private boolean GZip = true;
    private int MaxRequestSizeKB = 100_000;
    private int backlog = MaxConcurrentRequests * 5;
    private String MIMEFile = "etc/MIME.db";
    private String WWWDir = "www";
    private int SOTimeout = 60_000;
    private int BufferSize = 524288;

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public void setSOTimeout(int in) {
        SOTimeout = in;
    }

    public void setBufferSize(int in) {
        BufferSize = in;
    }

    public void setMaximumConcurrentRequests(int in) {
        MaxConcurrentRequests = in;
        backlog = MaxConcurrentRequests * 5;
    }

    public void setGZip(boolean in) {
        GZip = in;
    }

    public void setMaximumRequestSizeInKB(int in) {
        MaxRequestSizeKB = in;
    }

    public void setBacklog(int in) {
        backlog = in;
    }

    public void setMIMEFile(String in) throws Exception {
        MIMEFile = in;
        loadMIME();
    }

    public void setWWWDirectory(String in) {
        WWWDir = in;
    }

    public void setPort(int in) {
        Port = in;
    }

    public void addCustomHeader(Entry<String, String> in) {
        CustomHeaders.put(in.getKey(), in.getValue());
    }

    private void mainLoop(ServerSocket serverSocket, ThreadPoolExecutor poolExecutor) {
        while (true) {
            try {
                Socket s = serverSocket.accept();
                s.setKeepAlive(false);
                s.setTcpNoDelay(true);
                s.setReceiveBufferSize(BufferSize);
                s.setSendBufferSize(BufferSize);
                s.setSoTimeout(SOTimeout);

                try {
                    poolExecutor.execute(new Engine(s));
                } catch (RejectedExecutionException ignore) {
                    log.i("concurrent connections exceed the configured maximum");
                    Network.write(new BufferedOutputStream(s.getOutputStream()), new byte[]{}, "", HTTPCode.SERVICE_UNAVAILABLE, false, new HashMap<>(), false);
                }

            } catch (IOException e) {
                log.e(e.getMessage());
                return;
            }
        }
    }

    public void HTTPStart() throws Exception {
        loadMIME();
        final int nCores = Runtime.getRuntime().availableProcessors();
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(MaxConcurrentRequests);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(nCores, nCores, 60, TimeUnit.SECONDS, workQueue);
        try (ServerSocket serverSocket = new ServerSocket(Port, backlog)) {
            log.s("Server running at :" + Port);
            mainLoop(serverSocket, poolExecutor);
        } finally {
            poolExecutor.shutdownNow();
        }
    }

    private void loadMIME() throws Exception {
        MIME.readFromFile(MIMEFile);
    }

    public void HTTPSStart(String KeyStorePath, String KeyStorePassword) throws Exception {
        HTTPSStart(KeyStorePath, KeyStorePassword, "TLSv1.3", "JKS", "SunX509");
    }


    public void HTTPSStart(String KeyStorePath, String KeyStorePassword, String TLSVersion) throws Exception {
        HTTPSStart(KeyStorePath, KeyStorePassword, TLSVersion, "JKS", "SunX509");
    }

    public void HTTPSStart(String KeyStorePath, String KeyStorePassword, String TLSVersion, String KeyStoreType) throws Exception {
        HTTPSStart(KeyStorePath, KeyStorePassword, TLSVersion, KeyStoreType, "SunX509");
    }

    public void HTTPSStart(String KeyStorePath, String KeyStorePassword, String TLSVersion, String KeyStoreType, String KeyManagerFactoryType) throws Exception {
        System.setProperty("jdk.tls.ephemeralDHKeySize", "2048"); // Mitigation against LOGJAM TLS Attack
        System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true"); // Mitigation against Client Renegotiation Attack
        loadMIME();
        final int nCores = Runtime.getRuntime().availableProcessors();
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(MaxConcurrentRequests);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(nCores, nCores, 60, TimeUnit.SECONDS, workQueue);
        try (ServerSocket serverSocket = getSSLContext(Path.of(KeyStorePath), KeyStorePassword.toCharArray(), TLSVersion, KeyStoreType, KeyManagerFactoryType).getServerSocketFactory().createServerSocket(Port, backlog)) {
            log.s("Server running at :" + Port);
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

    public HashMap<String, String> pathFiltration(HashMap<String, String> Headers) {
        String path = "./" + WWWDir + PathFilter.filter(Headers.get("path"));
        String[] pathSplit = path.split("\\.");
        String ContentType = MIME.get(new HashMap<>() {{
            put("extension", pathSplit[pathSplit.length - 1]);
        }}, "mime", 1).get(0);
        return new HashMap<>() {{
            put("path", path);
            put("mime", ContentType);
        }};
    }

    // Keys: byte[] body, String mime, String code, String isFile, [Optional] HashMap<String, String> CustomHeaders
    public abstract HashMap<String, Object> main(HashMap<String, String> headers, byte[] body);

    public class Engine implements Runnable {
        private final Socket s;

        public Engine(Socket S) {
            s = S;
        }

        @Override
        public void run() {
            BufferedInputStream DIS;
            BufferedOutputStream DOS = null;
            try {
                DIS = new BufferedInputStream(s.getInputStream(), BufferSize);
                DOS = new BufferedOutputStream(s.getOutputStream(), BufferSize);
                List<byte[]> ALm = ArraySplit.split(Network.read(DIS, MaxRequestSizeKB).toByteArray(), new byte[]{13, 10, 13, 10});
                HashMap<String, String> Headers = HeaderToHashmap.convert(new String(ALm.get(0)));
                byte[] request = PostRequestMerge.merge(ALm, DIS, Headers, MaxRequestSizeKB);
                HashMap<String, Object> response = main(Headers, request);
                HashMap<String, String> ResHeaders = new HashMap<>();
                ResHeaders.putAll(CustomHeaders);
                if(response.containsKey("CustomHeaders")) ResHeaders.putAll((Map<? extends String, ? extends String>) response.get("CustomHeaders"));
                Network.write(DOS, (byte[]) response.get("body"), (String) response.get("mime"), (String) response.get("code"), GZip, ResHeaders, !((String) response.get("isFile")).equals("0"));
            } catch (Exception e) {
                // If you're building a highly-secured system, it is highly recommended to change getStackTrace(e) to something else
                Network.write(DOS, getStackTrace(e).getBytes(), "text/html", HTTPCode.INTERNAL_SERVER_ERROR, GZip, CustomHeaders, false);
            }
        }
    }
}
