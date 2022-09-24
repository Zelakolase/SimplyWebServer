package server;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import javax.net.ssl.*;

import lib.*;
public abstract class Server {
    private boolean Dynamic = true;
	private int Port = 80;
	private int MaxConcurrentRequests = 1000;
	private volatile int CurrentConcurrentRequests = 0;
	private boolean GZip = true;
	private int MaxRequestSizeKB = 100_000;
	private int backlog = MaxConcurrentRequests * 5;
	private String MIMEFile = "../etc/MIME.db";
	private String WWWDir = "www";
	private SparkDB MIME = new SparkDB();
    HashMap<String, String> CustomHeaders = new HashMap<>();
    public void setMaximumConcurrentRequests(int in) {
		MaxConcurrentRequests = in;
	}

	public void setGZip(boolean in) {
		GZip = in;
	}

	public void setMaximumRequestSizeInKB(int in) {
		MaxRequestSizeKB = in;
	}

	public void setDynamic(boolean in) {
		Dynamic = in;
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
    
    public void setHTTPPort(int in) {
        Port = in;
    }

    public void HTTPStart() throws Exception {
		loadMIME();
        ExecutorService executor = Executors.newFixedThreadPool(MaxConcurrentRequests);
        try (ServerSocket SS = new ServerSocket(Port, backlog)) {
			log.s("Server running at :"+Port);
            while (true) {
                /*
                 * Max 10 retries (1ms delay between each) for every request to process if
                 * MaxConcurrentRequests is reached
                 */
                int tries = 0; // current tries
                inner: while (tries < 11) {
                    if (tries > 0)
                        Thread.sleep(1);
                    if (CurrentConcurrentRequests <= MaxConcurrentRequests) {
                        Socket S = SS.accept();
                        S.setKeepAlive(false);
						S.setTcpNoDelay(true);
						S.setReceiveBufferSize(65536);
						S.setSendBufferSize(65536);
						S.setSoTimeout(60000);
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
		if(!Dynamic) MIME.readFromFile(MIMEFile);
	}

    public void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword) throws Exception{
		HTTPSStart(port, KeyStorePath, KeyStorePassword, "TLSv1.3", "JKS", "SunX509");
	}

	public void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion) throws Exception{
		HTTPSStart(port, KeyStorePath, KeyStorePassword, TLSVersion, "JKS", "SunX509");
	}

	public void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion,
			String KeyStoreType) throws Exception{
		HTTPSStart(port, KeyStorePath, KeyStorePassword, TLSVersion, KeyStoreType, "SunX509");
	}

	public void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion,
			String KeyStoreType, String KeyManagerFactoryType) throws Exception{
				loadMIME();
                ExecutorService executor = Executors.newFixedThreadPool(MaxConcurrentRequests);
			char[] keyStorePassword = KeyStorePassword.toCharArray();
			ServerSocket SS = getSSLContext(Path.of(KeyStorePath), keyStorePassword, TLSVersion, KeyStoreType,
					KeyManagerFactoryType).getServerSocketFactory().createServerSocket(port, backlog);
			Arrays.fill(keyStorePassword, '0');
			while (true) {
				/*
				 * Max 10 retries (1ms delay between each) for every request to process if
				 * MaxConcurrentReqs is reached
				 */
				int tries = 0; // current tries
				inner: while (tries < 11) {
					if (tries > 0)
						Thread.sleep(1);
					if (CurrentConcurrentRequests <= MaxConcurrentRequests) {
						Socket S = SS.accept();
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
            	DIS = new BufferedInputStream(s.getInputStream(),65536);
				DOS = new BufferedOutputStream(s.getOutputStream(),65536);
				byte[] Request = Network.read(DIS, MaxRequestSizeKB).toByteArray();
				List<byte[]> ALm = ArraySplit.split(Request, new byte[] { 13, 10, 13, 10 });
				if(Dynamic) {
					HashMap<String, byte[]> response = main(PostRequestMerge.merge(ALm, DIS, HeaderToHashmap.convert(new String(ALm.get(0))), MaxRequestSizeKB));
					Network.write(DOS, response.get("body"), response.get("mime"), response.get("code"), GZip, CustomHeaders);
				}else {
					// Static
					String headers = new String(ALm.get(0));
					String path = "./" + WWWDir + PathFilter.filter(HeaderToHashmap.convert(headers).get("path"));
					String[] pathSplit = path.split("\\.");
					String ContentType = MIME.get(new HashMap<>() {{
						put("extenstion", pathSplit[pathSplit.length - 1]);
					}}, "mime", 1).get(0);
					Network.write(DOS, IO.read(path), ContentType.getBytes(), HTTPCode.OK.getBytes(), GZip, CustomHeaders);
				}
			}catch(Exception e) {
				Network.write(DOS, getStackTrace(e).getBytes(), "text/html".getBytes(), HTTPCode.INTERNAL_SERVER_ERROR.getBytes(), GZip, CustomHeaders);
			}
        }
    }
	private static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
   }
	// Keys: body, mime, code
	public abstract HashMap<String, byte[]> main(byte[] request);
}
