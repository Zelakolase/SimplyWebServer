package lib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.util.HashMap;
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
	 * @param dOS                  data stream to write to
	 * @param ResponseData         the body of the http response
	 * @param bs          the content type of the body
	 * @param bs2         the response code (obv.)
	 * @param GZip                 weather to use GZip or not
	 * @param customHeaders additional response headers to add like
	 *                             'X-XSS-Protection'
	 */
	public static void write(BufferedOutputStream dOS, byte[] ResponseData, byte[] bs, byte[] bs2,
			boolean GZip, HashMap<String, String> customHeaders) {
		try {
			if (GZip)
				ResponseData = compress(ResponseData);
			StringBuilder temp = new StringBuilder((new String(bs2)));
			temp.append("\r\n");
			dOS.write(temp.toString().getBytes());
			dOS.write("Server: SWS 2.0\r\n".getBytes());
			for(java.util.Map.Entry<String, String> e : customHeaders.entrySet()) {
				temp.setLength(0);
				temp.append(e.getKey());
				temp.append(": ");
				temp.append(e.getValue());
				dOS.write(temp.toString().getBytes());
			}
			temp.setLength(0);
			dOS.write(("Connection: Keep-Alive\r\n").getBytes());
			if (GZip)
				dOS.write("Content-Encoding: gzip\r\n".getBytes());
			String bss = new String(bs);
			temp.append("Content-Type: ");
			temp.append(bss);
			if (bss.equals("text/html")) {
				temp.append(";charset=UTF-8\r\n");
			} else {
				temp.append("\r\n");
			}
			dOS.write(temp.toString().getBytes());
			temp.setLength(0);
			temp.append("Content-Length: ");
			temp.append(ResponseData.length);
			temp.append("\r\n\r\n");
			dOS.write(temp.toString().getBytes());
			dOS.write(ResponseData);
			dOS.flush();
			dOS.close();
		} catch (Exception e) {
			log.e(e, Network.class.getName(), "write");
		}
	}

		/**
	 * Reads from socket into ArrayList
	 *
	 * @param MAX_REQ_SIZE the maximum kbytes to read
	 */
	public static ByteArrayOutputStream read(BufferedInputStream dIS, int MAX_REQ_SIZE) {
		MAX_REQ_SIZE = MAX_REQ_SIZE * 1000;
		ByteArrayOutputStream Reply = new ByteArrayOutputStream(1024);
		int counter = 0;
		try {
			ReadLoop: do {
				if (counter < MAX_REQ_SIZE) {
					Reply.write(dIS.readNBytes(1));
					counter++;
				} else {
					break ReadLoop;
				}
			} while (dIS.available() > 0);
		} catch (Exception e) {
		}
		return Reply;
	}

	/**
	 * Mandatory Read (used rarely)
	 */
	public static byte[] ManRead(BufferedInputStream dIS, int bytestoread) {
		try {
			return dIS.readNBytes(bytestoread);
		} catch (Exception e) {
		}
		return null;
	}
}
