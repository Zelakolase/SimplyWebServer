package lib;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class IO {
	/**
	 * Reads file as byte array
	 *
	 * @param filename the name of the dest. file
	 * @return file content in bytes
	 */
	public static byte[] read(String filename) {
		try {
			return Files.readAllBytes(Paths.get(filename));
		} catch (Exception e) {
			log.e(e, IO.class.getName(), "read");
			return null;
		}
	}

	/**
	 * Writes on file
	 *
	 * @param filename the name of the dest. file
	 * @param content  the content to write in String
	 * @param append   weather to append to existing value or not
	 * @see write(String filename, byte[] content, boolean append)
	 */
	public static void write(String filename, String content, boolean append) {
		write(filename, content.getBytes(), append);
	}

	/**
	 * Writes on file
	 *
	 * @param filename the name of the dest. file
	 * @param content  the content to write in bytes
	 * @param append   weather to append to existing value or not
	 */
	public static void write(String filename, byte[] content, boolean append) {

		try {
			StandardOpenOption set = null;
			if (append)
				set = StandardOpenOption.APPEND;
			if (!append)
				set = StandardOpenOption.WRITE;
			Files.write(Paths.get(filename), content, set);
		} catch (Exception e) {
			log.e(e, IO.class.getName(), "write");
		}
	}
}