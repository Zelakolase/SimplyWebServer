package lib;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

public class Network {
	/**
	 * GZIP Compression
	 *
	 * @param data data to compress in bytes
	 * @return compressed data in bytes
	 */
	public static byte[] compress(byte[] data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		GZIPOutputStream gzip = new GZIPOutputStream(bos);
		gzip.write(data);
		gzip.close();
		byte[] compressed = bos.toByteArray();
		bos.close();
		return compressed;
	}

	/**
	 * HTTP Write Response Function
	 *
	 * @param DOS                  data stream to write to
	 * @param ResponseData         the body of the http response
	 * @param ContentType          the content type of the body
	 * @param ResponseCode         the response code (obv.)
	 * @param GZip                 weather to use GZip or not
	 * @param AddedResponseHeaders additional response headers to add like
	 *                             'X-XSS-Protection'
	 */
	public static void write(DataOutputStream DOS, byte[] ResponseData, String ContentType, String ResponseCode,
			boolean GZip, String AddedResponseHeaders) {
		try {
			if (GZip)
				ResponseData = compress(ResponseData);
			DOS.write((ResponseCode + "\r\n").getBytes());
			DOS.write("Server: SWS 1.0\r\n".getBytes());
			DOS.write((AddedResponseHeaders).getBytes());
			DOS.write(("Connection: Keep-Alive\r\n").getBytes());
			if (GZip)
				DOS.write("Content-Encoding: gzip\r\n".getBytes());
			if (ContentType.equals("text/html")) {
				DOS.write(("Content-Type: " + ContentType + ";charset=UTF-8\r\n").getBytes());
			} else {
				DOS.write(("Content-Type: " + ContentType + "\r\n").getBytes());
			}
			DOS.write(("Content-Length: " + ResponseData.length + "\r\n\r\n").getBytes());
			DOS.write(ResponseData);
			DOS.flush();
			DOS.close();
		} catch (Exception e) {
			log.e(e, Network.class.getName(), "write");
		}
	}

	/**
	 * Reads from socket into ArrayList
	 *
	 * @param MAX_REQ_SIZE the maximum kbytes to read
	 */
	public static ArrayList<Byte> read(DataInputStream DIS, int MAX_REQ_SIZE) {
		ArrayList<Byte> result = new ArrayList<>();
		int byteCounter = 0;
		try {
			do {
				if (byteCounter < MAX_REQ_SIZE * 1000) {
					result.add(DIS.readNBytes(1)[0]);
					byteCounter++;
				}
			} while (DIS.available() > 0);

		} catch (IOException e) {
			log.e(e, Network.class.getName(), "read");
		}
		return result;
	}

	/**
	 * Mandatory Read (used rarely)
	 */
	public static byte[] ManRead(DataInputStream DIS, int bytestoread) {
		try {
			return DIS.readNBytes(bytestoread);
		} catch (IOException e) {
			log.e(e, Network.class.getName(), "ManRead");
		}
		return null;
	}
}
