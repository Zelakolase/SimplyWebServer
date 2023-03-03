package server;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.security.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import javax.net.ssl.*;

import lib.*;
public abstract class Server {
	private int Port = 80;
	private int MaxConcurrentRequests = 1000;
	private volatile int CurrentConcurrentRequests = 0;
	private boolean GZip = true;
	private int MaxRequestSizeKB = 100_000;
	private int backlog = MaxConcurrentRequests * 5;
	private String MIMEFile = "etc/MIME.db";
	private String WWWDir = "www";
	private SparkDB MIME = new SparkDB();
	private int MaxTries = 11;
    private HashMap<String, String> CustomHeaders = new HashMap<>();
	private int SOTimeout = 60_000;
	private int BufferSize = 524288;

	public void setSOTimeout(int in) {
		SOTimeout = in;
	}

	public void setBufferSize(int in) { 
		BufferSize = in;
	}

    public void setMaximumConcurrentRequests(int in) {
		MaxConcurrentRequests = in;
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

	public void addCustomHeader(Entry<String, String> in){
		CustomHeaders.put(in.getKey(), in.getValue());
	}

	public void setMaximumSocketTries(int in) {
		MaxTries = in + 1;
	}

    public void HTTPStart() throws Exception {
		loadMIME();
        ExecutorService executor = Executors.newFixedThreadPool(MaxConcurrentRequests);
        try (ServerSocket SS = new ServerSocket(Port, backlog)) {
			log.s("Server running at :"+Port);
            while (true) {
                /*
                 * Max x retries (1ms delay between each) for every request to process if
                 * MaxConcurrentRequests is reached
                 */
                int tries = 0; // current tries
                inner: while (tries < MaxTries) {
                    if (tries > 0)
                        Thread.sleep(1);
                    if (CurrentConcurrentRequests <= MaxConcurrentRequests) {
                        Socket S = SS.accept();
                        S.setKeepAlive(false);
						S.setTcpNoDelay(true);
						S.setReceiveBufferSize(BufferSize);
						S.setSendBufferSize(BufferSize);
						S.setSoTimeout(SOTimeout);
                        executor.execute(new Engine(S));
                        CurrentConcurrentRequests++;
                        break inner;
                    } else {
                        tries++;
                    }
                }
            }
        }
        finally {
            executor.shutdownNow();
        }
    }
	private void loadMIME() throws Exception {
		MIME.readFromFile(MIMEFile);
	}

    public void HTTPSStart(String KeyStorePath, String KeyStorePassword) throws Exception{
		HTTPSStart(KeyStorePath, KeyStorePassword, "TLSv1.3", "JKS", "SunX509");
	}

	public void HTTPSStart(String KeyStorePath, String KeyStorePassword, String TLSVersion) throws Exception{
		HTTPSStart(KeyStorePath, KeyStorePassword, TLSVersion, "JKS", "SunX509");
	}

	public void HTTPSStart(String KeyStorePath, String KeyStorePassword, String TLSVersion,
			String KeyStoreType) throws Exception{
		HTTPSStart(KeyStorePath, KeyStorePassword, TLSVersion, KeyStoreType, "SunX509");
	}

	public void HTTPSStart(String KeyStorePath, String KeyStorePassword, String TLSVersion,
			String KeyStoreType, String KeyManagerFactoryType) throws Exception{
				System.setProperty("jdk.tls.ephemeralDHKeySize", "2048"); // Mitigation against LOGJAM TLS Attack
				System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true"); // Mitigation against Client Renegotiation Attack
				loadMIME();
                ExecutorService executor = Executors.newFixedThreadPool(MaxConcurrentRequests);
				char[] keyStorePassword = KeyStorePassword.toCharArray();
				ServerSocket SS = getSSLContext(Path.of(KeyStorePath), keyStorePassword, TLSVersion, KeyStoreType,
				KeyManagerFactoryType).getServerSocketFactory().createServerSocket(Port, backlog);
				Arrays.fill(keyStorePassword, '0');
			while (true) {
				/*
				 * Max x retries (1ms delay between each) for every request to process if
				 * MaxConcurrentReqs is reached
				 */
				int tries = 0; // current tries
				inner: while (tries < MaxTries) {
					if (tries > 0)
						Thread.sleep(1);
					if (CurrentConcurrentRequests <= MaxConcurrentRequests) {
						Socket S = SS.accept();
						S.setKeepAlive(false);
						S.setTcpNoDelay(true);
						S.setReceiveBufferSize(BufferSize);
						S.setSendBufferSize(BufferSize);
						S.setSoTimeout(SOTimeout);
						executor.execute(new Engine(S));
						CurrentConcurrentRequests++;
						break inner;
					} else {
						tries++;
					}
				}
			}
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
    public class Engine implements Runnable {
        private Socket s;
        public Engine(Socket S) {
            s = S;
        }

        @Override
        public void run() {
			BufferedInputStream DIS = null;
			BufferedOutputStream DOS = null;
			try {
            	DIS = new BufferedInputStream(s.getInputStream(), BufferSize);
				DOS = new BufferedOutputStream(s.getOutputStream(), BufferSize);
				List<byte[]> ALm = ArraySplit.split(Network.read(DIS, MaxRequestSizeKB).toByteArray(), new byte[] { 13, 10, 13, 10 });
				HashMap<String, String> Headers = HeaderToHashmap.convert(new String(ALm.get(0)));
				byte[] request = PostRequestMerge.merge(ALm, DIS, Headers, MaxRequestSizeKB);
				HashMap<String, byte[]> response = main(Headers, request);
				Network.write(DOS, response.get("body"), response.get("mime"), response.get("code"), GZip, CustomHeaders, new String(response.get("isFile")).equals("0") ? false : true);
			}catch(Exception e) {
				Network.write(DOS, getStackTrace(e).getBytes(), "text/html".getBytes(), HTTPCode.INTERNAL_SERVER_ERROR.getBytes(), GZip, CustomHeaders, false);
			}
        }
    }
	private static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
   }

   public HashMap<String, String> pathFilteration(HashMap<String, String> Headers) {
		StringBuilder PathBuilder = new StringBuilder();
		PathBuilder.append("./").append(WWWDir).append(PathFilter.filter(Headers.get("path")));
		String path = PathBuilder.toString();
		String[] pathSplit = path.split("\\.");
		String ContentType = MIME.get(new HashMap<>() {{
			put("extension", pathSplit[pathSplit.length - 1]);
		}}, "mime", 1).get(0);
		return new HashMap<>() {{
			put("path", path);
			put("mime", ContentType);
		}};
   }

	// Keys: body, mime, code
	public abstract HashMap<String, byte[]> main(HashMap<String, String> headers, byte[] body);
}
