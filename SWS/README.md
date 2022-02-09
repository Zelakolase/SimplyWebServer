# SimplyWebServer
## Try it
`$ javac HTTPTest.java && sudo java HTTPTest` : For HTTP static mode server on port 80.<br>
`$ javac HTTPSTest.java && sudo java HTTPSTest` : For HTTPS static mode server on port 443.<br>

## Modes of SWS
- Static mode : Where it **ONLY** serves files found in *WWWDir*. You have to edit *MIME.dat* or *MIMEFile* according to the file extensions you'll host.
- Dynamic mode : Implementation is found either in *HTTPTest.java* or *HTTPSTest.java*.<br> The abstract function's role is to take 3 key-value pairs, *content*,*mime*,*code*. <br>You're supplied with the headers in *req* and POST arguments "if any" in *body* and other multipart-formdata information in *additional*. Revise *MULTIPART-NOTICE.md* before making any web app that has multipart-formdata involved.

## Setters
- `setMaximumConcurrentRequests(int in)` : Sets maximum requests to process at the same moment. default: 1000.
- `setGZip(boolean in)` : Enables/Disables GZip [Read more](https://www.gnu.org/software/gzip). default: true.
- `setMaximumRequestSizeInKB(int in)` : Sets maximum data to read from the TCP stream. default: 1000MB.
- `setDynamic(boolean in)` : Changes the server mode. default: true.
- `setBacklog(int in)` : Sets the maximum clients that wait till they get accepted. default: 5000.
- `setMIMEFile(String in)` : Sets the MIME file path. default: MIME.dat.
- `setWWWDirectory(String in)` : Sets the default WWW directory for static mode. default: www.

## HTTPS
Default settings when calling `HTTPSStart(int port, String KeyStorePath, String KeyStorePassword)` are:
- TLS/SSL Version : TLSv1.3
- KeyStore type : JKS
- KeyManager Factory type : SunX509

Following functions can edit these settings:
- `HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion)`
- `HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion, String KeyStoreType)`
- `HTTPSStart(int port, String KeyStorePath, String KeyStorePassword, String TLSVersion, String KeyStoreType, String KeyManagerFactoryType)`

## HTTPCode Class
They are supplied in `HTTPCode` Class. Example of using them in the dynamic mode :
```java
	@Override
	HashMap<String, byte[]> main(String req, byte[] body, byte[] additional) {
		HashMap<String, byte[]> response = new HashMap<String, byte[]>() {{
			put("content","Hello".getBytes());
			put("mime","text/html".getBytes());
			put("code",HTTPCode.OK.getBytes()); // Here where we use it
		}};
		return response;
	}
```

## Classes definition
- ArraySplit : Splits the array according to a byte subarray.
- HeaderToHashmap : Converts header data (coming from *req* as an example) to a hashmap. Header name is key and header value is value.
- URIDecode : Decodes URI.
- SparkDB : A whole project explained [here](https://github.com/Zelakolase/SparkDB)
- PathFilter : A tool to add *index.html* to the end of */* path, and LFI protection.
- log : Makes colorful logs.
- IO : Reads/Writes in disk.