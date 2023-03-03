package lib;

import java.io.*;
import java.util.*;
public class PostRequestMerge {
	public static byte[] merge(List<byte[]> aLm, BufferedInputStream dIS, HashMap<String, String> headers, int max_bytes) throws IOException {
		ByteArrayOutputStream whole = new ByteArrayOutputStream();
		aLm.remove(0);
		if(aLm.size() > 0) {
        whole.write(aLm.get(0));
		if (aLm.size() >= 2) {
			whole.write(aLm.get(1));
			if (aLm.size() - 2 != 0) {
				// ALm(1) is the whole
				int x = aLm.size() - 2; // 1 element after ALm(1) if x = 1
				for (int i = 1; i <= x; i++) {
					byte[] CRLFCRLF = { 13, 10, 13, 10 };
					whole.write(CRLFCRLF);
					whole.write(aLm.get(i + 1));
				}
			}
			if (headers.containsKey("Content-Length")) {
				int num = Integer.parseInt(headers.get("Content-Length")); // num. of bytes in body only
				if (whole.size() < num) {
					// S L O W
					int to_read = num - whole.size();
					whole.write(Network.ManRead(dIS, to_read > max_bytes ? max_bytes : to_read));
				}
			}
		}
	}
		return whole.toByteArray();
	}
}