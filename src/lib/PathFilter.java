package lib;
public class PathFilter {
	public static String filter(String path) {
		String res = path;
		res = res.replaceAll("\\.\\.", ""); // LFI protection
		res = res.replaceAll("//", "/");
		if (res.endsWith("/"))
			res = res + "index.html";
		return res;
	}
}
