package server;
import java.io.FileInputStream;
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
	private String MIMEFile = "MIME.db";
	private String WWWDir = "www";
	private SparkDB MIME = new SparkDB();
    HashMap<String, String> CustomHeaders = new HashMap<>();
    public Server() {
        try {
            MIME.readFromFile(MIMEFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

	public void setDynamic(boolean in) {
		Dynamic = in;
	}

	public void setBacklog(int in) {
		backlog = in;
	}

	public void setMIMEFile(String in) {
		MIMEFile = in;
	}

	public void setWWWDirectory(String in) {
		WWWDir = in;
	}
    
    public void setHTTPPort(int in) {
        Port = in;
    }

    public void HTTPStart() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(MaxConcurrentRequests);
        try (ServerSocket SS = new ServerSocket(Port, backlog)) {
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
            
        }
    }
	public abstract HashMap<String, byte[]> main(byte[] request);
}
