package lib;

import java.nio.charset.StandardCharsets;

public class URIDecode {
	/*
	 * Decode URI
	 */
	public static String decode(String in) {
		in = in.replaceAll("\\+", " ");
		in = in.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
		return java.net.URLDecoder.decode(in, StandardCharsets.UTF_8);
	}
}
