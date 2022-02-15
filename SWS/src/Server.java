import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import lib.ArraySplit;
import lib.HTTPCode;
import lib.HeaderToHashmap;
import lib.IO;
import lib.Network;
import lib.SparkDB;
import lib.log;

public abstract class Server {
	boolean dynamic = true; // Dynamic mode?
	int port = 80; // Port Number
	int MaxConcurrentRequests = 1000; // Max requests to process at a time
	private int CurrentConcurrentRequests = 0; // Current Concurrent Requests
	boolean GZip = true; // GZip compression? (default true)
	int MAX_REQ_SIZE = 1000000; // Max bytes to read in kb. (default 1000MB)
	int backlog = MaxConcurrentRequests * 5; // Max requests to wait for processing, default is 5000MB
	String MIMEFile = "MIME.dat";
	String WWWDir = "www";
	SparkDB MIME = new SparkDB();
	String AddedResponseHeaders = ""; // Custom Response headers

	public void setMaximumConcurrentRequests(int in) {
		MaxConcurrentRequests = in;
	}

	public void setGZip(boolean in) {
		GZip = in;
	}

	public void setMaximumRequestSizeInKB(int in) {
		MAX_REQ_SIZE = in;
	}

	public void setDynamic(boolean in) {
		dynamic = in;
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

	public void HTTPStart(int port) {
		// HTTP Server start
		try {
			MIME.readfromfile(MIMEFile);
			try (ServerSocket SS = new ServerSocket(port, backlog)) {
				while (true) {
					/*
					 * Max 5 retries (1ms delay between each) for every request to process if
					 * MaxConcurrentReqs is reached
					 */
					int tries = 0; // current tries
					inner: while (tries < 6) {
						if (tries > 0)
							Thread.sleep(1);
						if (CurrentConcurrentRequests <= MaxConcurrentRequests) {
							Socket S = SS.accept();
							Engine e = new Engine(S);
							e.start();
							CurrentConcurrentRequests++;
							break inner;
						} else {
							tries++;
						}
					}
				}
			}
		} catch (Exception e) {
			log.e(e, Server.class.getName(), "HTTPStart");
		}
	}

	public void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword) {
		HTTPSStart(port, KeyStorePath, KeyStorePassword, "TLSv1.3", "JKS", "SunX509");
	}

	public void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion) {
		HTTPSStart(port, KeyStorePath, KeyStorePassword, TLSVersion, "JKS", "SunX509");
	}

	public void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion,
			String KeyStoreType) {
		HTTPSStart(port, KeyStorePath, KeyStorePassword, TLSVersion, KeyStoreType, "SunX509");
	}

	public void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion,
			String KeyStoreType, String KeyManagerFactoryType) {
		// HTTPS Server start, default values
		try {
			MIME.readfromfile(MIMEFile);
			char[] keyStorePassword = KeyStorePassword.toCharArray();
			ServerSocket SS = getSSLContext(Path.of(KeyStorePath), keyStorePassword, TLSVersion, KeyStoreType,
					KeyManagerFactoryType).getServerSocketFactory().createServerSocket(port, backlog);
			Arrays.fill(keyStorePassword, '0');
			while (true) {
				/*
				 * Max 5 retries (1ms delay between each) for every request to process if
				 * MaxConcurrentReqs is reached
				 */
				int tries = 0; // current tries
				inner: while (tries < 6) {
					if (tries > 0)
						Thread.sleep(1);
					if (CurrentConcurrentRequests <= MaxConcurrentRequests) {
						Socket S = SS.accept();
						Engine e = new Engine(S);
						e.start();
						CurrentConcurrentRequests++;
						break inner;
					} else {
						tries++;
					}
				}
			}
		} catch (Exception e) {
			log.e(e, Server.class.getName(), "HTTPSStart");
		}
	}

	private SSLContext getSSLContext(Path keyStorePath, char[] keyStorePass, String TLSVersion, String KeyStoreType,
			String KeyManagerFactoryType) {
		try {
			var keyStore = KeyStore.getInstance(KeyStoreType);
			keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePass);
			var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactoryType);
			keyManagerFactory.init(keyStore, keyStorePass);
			var sslContext = SSLContext.getInstance(TLSVersion);
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
			return sslContext;
		} catch (Exception e) {
			log.e(e, Server.class.getName(), "getSSLContext");
			return null;
		}
	}

	private byte[] toPrimitives(Byte[] oBytes) {
		byte[] bytes = new byte[oBytes.length];
		for (int i = 0; i < oBytes.length; i++) {
			bytes[i] = oBytes[i];
		}
		return bytes;
	}

	public class Engine extends Thread {
		// The main request processor
		Socket S;

		Engine(Socket in) {
			S = in;
		}

		@Override
		public void run() {
			try {
				DataInputStream DIS = new DataInputStream(S.getInputStream());
				DataOutputStream DOS = new DataOutputStream(S.getOutputStream());

				ArrayList<Byte> RequestInAL = Network.read(DIS, MAX_REQ_SIZE);
				byte[] Request = toPrimitives(RequestInAL.toArray(Byte[]::new));
				List<byte[]> headerPost = ArraySplit.split(Request, new byte[] { 13, 10, 13, 10 });
				String headers = new String(headerPost.get(0));
				// above split header \r\n\r\n from post request (if any.)

				if (!dynamic) {
					/*
					 * Static Mode
					 */
					String path = "./" + WWWDir + "/"
							+ HeaderToHashmap.convert(headers).get("path").replaceFirst("/", "");
					String[] pathSplit = path.split("\\.");
					String ContentType = MIME.get("extension", pathSplit[pathSplit.length - 1], "mime"); // the last
																											// section
																											// in dots.
																											// for
																											// example,
																											// index.old.html
																											// pathsplit
																											// would
																											// result
																											// html as
																											// it's the
																											// last item
					Network.write(DOS, IO.read(path), ContentType, HTTPCode.OK, GZip, AddedResponseHeaders);
				} else {
					HashMap<String, byte[]> Reply = new HashMap<>();
					/*
					 * Dynamic Mode
					 */
					/*
					 * multipart-formdata processing for TLS records limitation. Require mandatory
					 * read as s.available() is lying and oracle devs are lazy to fix it.
					 */
					ArrayList<Byte> multipart = new ArrayList<>();
					if (headerPost.size() > 2) { // multipart-formdata is potentially detected
						for (int i = 0; i < headerPost.get(2).length; i++) {
							multipart.add(headerPost.get(2)[i]);
						}
						HashMap<String, String> headersHM = HeaderToHashmap.convert(headers);
						if (headersHM.containsKey("Content-Length") // if the request has 'Content-Length' specified
								&& (Request.length - headerPost.get(0).length) < Integer
										.valueOf(headersHM.get("Content-Length")) // if we have additional data
								&& Integer.valueOf(headersHM.get("Content-Length")) < MAX_REQ_SIZE) { // if the
																										// remaining
																										// data is less
																										// than 1000MB
							// Other data is available, confirmed.
							byte[] add = Network.ManRead(DIS,
									Integer.valueOf(HeaderToHashmap.convert(headers).get("Content-Length"))
											- (headerPost.get(1).length + headerPost.get(2).length + 4)); // Read n
																											// bytes
							for (byte element : add) {
								multipart.add(element);
							}
						}
						Reply = main(headers, headerPost.get(1), toPrimitives(multipart.toArray(Byte[]::new)));
					} else if (headerPost.size() == 2) { // We have a body
						Reply = main(headers, headerPost.get(1), null);
					} else { // We have a request without a body
						Reply = main(headers, null, null);
					}
					// [content = hi , mime = text/html , code = HTTPCode.OK]
					Network.write(DOS, Reply.get("content"), new String(Reply.get("mime")),
							new String(Reply.get("code")), GZip, AddedResponseHeaders);
				}
				DIS.close();
				DOS.close();
			} catch (Exception e) {
				log.e(e, Engine.class.getName(), "run");
			}
		}

	}

	abstract HashMap<String, byte[]> main(String req, byte[] body, byte[] additional);
}
