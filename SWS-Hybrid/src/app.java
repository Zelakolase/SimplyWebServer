import java.util.Arrays;
import java.io.*;
import java.util.*;

import lib.HeaderToHashmap;

public class app {

	public static void main(String[] args) throws IOException {
		/*
		 * Form of input :
		 * 45;44;115 }-> byte decimals by semicolon delimiter
		 */
		String[] literaldecimals = new BufferedReader(new InputStreamReader(System.in)).readLine().split(";");
		byte[] req = new byte[literaldecimals.length];
		for(int i = 0;i<literaldecimals.length;i++) {
			req[i] = Byte.parseByte(literaldecimals[i].trim());
		}
		String request = "<html>Hello "+HeaderToHashmap.convert(new String(req)).get("User-Agent")+"!</html>";
		// Output formation. 45;44;115,HTTP/1.1 200 OK,html/text }-> response in decimal by semicolon delimiter, HTTP code, MIME type
		// Output is the request itself
		String stdout = Arrays.toString(request.getBytes()).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",\\s+", ";")+",HTTP/1.1 200 OK,text/html";
		System.out.print(stdout);
	}

}
