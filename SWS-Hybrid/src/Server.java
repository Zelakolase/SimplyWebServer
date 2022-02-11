import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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

import lib.*;

public abstract class Server {
	static boolean dynamic = true; // Dynamic mode?
	static int port = 80; // Port Number
	static int MaxConcurrentRequests = 1000; // Max requests to process at a time
	private static int CurrentConcurrentRequests = 0; // Current Concurrent Requests
	static boolean GZip = false; // GZip compression? (default true)
	static int MAX_REQ_SIZE = 1000000; // Max bytes to read in kb. (default 1000MB)
	static int backlog = MaxConcurrentRequests * 5; // Max requests to wait for processing, default is 5000MB
	static String MIMEFile = "MIME.dat";
	static String WWWDir = "www";
	static String commandFire = "java app %request%";
	static SparkDB MIME = new SparkDB();
	static String AddedResponseHeaders = ""; // Custom Response headers
	static ProcessBuilder PB = new ProcessBuilder();
	public static void main(String[] args) {
		HashMap<String, String> arguments = new HashMap<String, String>();
		if(args[0].equals("--help")) {
			System.out.println("--port [num] - Specify port. MANDATORY\n"
					+ "--proto [http/https]- Specify protocol. MANDATORY\n"
					+ "--max-concurrent-requests [num] - Max requests to process at a time. MANDATORY\n"
					+ "--backlog [num] - Max requests to wait for processing. MANDATORY\n"
					+ "--mime-file [path] - mime db. default: MIME.dat\n"
					+ "--www-dir [path] - www index directory. default: www\n"
					+ "--gzip [1/0] - GZip. MANDATORY\n"
					+ "--dynamic [1/0] - dynamic/static. MANDATORY\n"
					+ "--tls-version [SSL/SSLv2/SSLv3/TLS/TLSv1/TLSv1.1/TLSv1.2/TLSv1.3/DTLS/DTLSv1.0/DTLSv1.2] - SSL/TLS Version\n"
					+ "--keystore-path [path]. MANDATORY IF PROTO IS HTTPS\n"
					+ "--keystore-pass [password]. MANDATORY IF PROTO IS HTTPS\n"
					+ "--keystore-type [JKS/JCEKS/DKS/PKCS11/PKCS12]. TLS VERSION IS MANDATORY"
					+ "--keymanager-type [type]. TLS VERSION AND KEYSTORE TYPE IS MANDATORY\n"
					+ "--app-command [cmd %request%]. MANDATORY\n"
					+ "--max-req-size [num in kb]. MANDATORY");
		}else {
			for(int i = 0;i<args.length;i+=2) {
				arguments.put(args[i], args[i+1]);
			}
			port = Integer.parseInt(arguments.get("--port"));
			MaxConcurrentRequests = Integer.parseInt(arguments.get("--max-concurrent-requests"));
			backlog = Integer.parseInt(arguments.get("--backlog"));
			if(arguments.get("--gzip").equals("1")) GZip = true;
			if(arguments.get("--dynamic").equals("0")) dynamic = false;
			commandFire = arguments.get("--app-command");
			MAX_REQ_SIZE = Integer.parseInt(arguments.get("--max-req-size"));
			if(arguments.containsKey("--mime-file")) MIMEFile = arguments.get("--mime-file");
			if(arguments.containsKey("--www-dir")) WWWDir = arguments.get("--www-dir");
			if(arguments.get("--proto").equals("http")) {
				// http
				HTTPStart(port);
			}else {
				// https
				String KeyStorePath = arguments.get("--keystore-path");
				String KeyStorePass = arguments.get("--keystore-pass");
				if(arguments.containsKey("--keymanager-type")) {
					HTTPSStart(port,KeyStorePath,KeyStorePass, arguments.get("--tls-version"),arguments.get("--keystore-type"),arguments.get("--keymanager-type"));
				}else if(arguments.containsKey("--keystore-type")) {
					HTTPSStart(port,KeyStorePath,KeyStorePass, arguments.get("--tls-version"),arguments.get("--keystore-type"));
				}else if(arguments.containsKey("--tls-version")) {
					HTTPSStart(port,KeyStorePath,KeyStorePass, arguments.get("--tls-version"));
				}else {
					HTTPSStart(port,KeyStorePath,KeyStorePass);
				}
			}
			
		}
	} 
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

	public static void HTTPStart(int port) {
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

	public static void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword) {
		HTTPSStart(port, KeyStorePath, KeyStorePassword, "TLSv1.3", "JKS", "SunX509");
	}

	public static void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion) {
		HTTPSStart(port, KeyStorePath, KeyStorePassword, TLSVersion, "JKS", "SunX509");
	}

	public static void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion,
			String KeyStoreType) {
		HTTPSStart(port, KeyStorePath, KeyStorePassword, TLSVersion, KeyStoreType, "SunX509");
	}

	public static void HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion,
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

	private static SSLContext getSSLContext(Path keyStorePath, char[] keyStorePass, String TLSVersion, String KeyStoreType,
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

	private static byte[] toPrimitives(Byte[] oBytes) {
		byte[] bytes = new byte[oBytes.length];
		for (int i = 0; i < oBytes.length; i++) {
			bytes[i] = oBytes[i];
		}
		return bytes;
	}

	public static class Engine extends Thread {
		// The main request processor
		Socket S;

		Engine(Socket in) {
			S = in;
		}

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
					HashMap<String, byte[]> Reply = new HashMap<String, byte[]>();
					/*
					 * Dynamic Mode
					 */
					/*
					 * multipart-formdata processing for TLS records limitation. Require mandatory
					 * read as s.available() is lying and oracle devs are lazy to fix it.
					 */
					ArrayList<Byte> multipart = new ArrayList<Byte>();
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
							for (int i = 0; i < add.length; i++) {
								multipart.add(add[i]);
							}
						}
						Reply = main(headers, headerPost.get(1), toPrimitives(multipart.toArray(Byte[]::new)));
					} else if (headerPost.size() == 2) { // We have a body
						Reply = main(headers, headerPost.get(1), null);
					} else { // We have a request without a body
						Reply = main(headers, null, null);
					}
					// [content = hi , mime = text/html , code = HTTPCode.OK]
					Network.write(DOS, Reply.get("content"),
							new String(Reply.get("mime")),
							new String(Reply.get("code")),
							GZip, AddedResponseHeaders);
				}
				DIS.close();
				DOS.close();
			} catch (Exception e) {
				log.e(e, Engine.class.getName(), "run");
			}
		}

	}

	public static HashMap<String, byte[]> main(String req, byte[] body, byte[] additional) {
		String to_stdin = (Arrays.toString(req.getBytes()) +" "+ Arrays.toString(body) +" "+ Arrays.toString(additional))
				.replaceAll("\\[", "")
				.replaceAll("\\]", "")
				.replaceAll(",\\s+","\\;")
				.replaceAll("null", "");
		String[] reply = cmd(commandFire.replace("%request%", "<<< "+"'"+to_stdin+"'")).split(","); // response, code, mime
		/**
		 * Response "45;115" to byte arr {45,115}
		 */
		String[] bytes = reply[0].split(";"); // space delimiter
		byte[] response = new byte[bytes.length];
		for(int i = 0;i<bytes.length;i++) {
			response[i] = Byte.parseByte(bytes[i].trim());
		}
		return new HashMap<String, byte[]>() {{
			put("content",response);
			put("mime",reply[2].getBytes());
			put("code",reply[1].getBytes());
		}};
		
	}
	public static String cmd(String s) {
    	String out = "";
    	try {
        	Process pr = PB.command("bash","-c",s).start();
        	pr.waitFor();
        	BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        	out = buf.readLine();
    	}catch(Exception e) {
    		e.printStackTrace();
    		// Shhh. be silent
    	}
    	return out;
    }
}
