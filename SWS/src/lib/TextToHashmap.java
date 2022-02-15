package lib;

import java.util.HashMap;

public class TextToHashmap {
	public static HashMap<String, String> Convert(String text, String EDelimiter, String KVDelimiter) {
		/**
		 * This function converts String/Text to HashMap
		 *
		 * @param text        The text to be converted
		 * @param EDelimiter  The delimiter between every element
		 * @param KVDelimiter The delimiter between every Key and Value in one element
		 */
		HashMap<String, String> out = new HashMap<>();
		String[] AText = text.split(EDelimiter);
		for (String Element : AText) {
			String[] pair = Element.split(KVDelimiter);
			out.put(pair[0], pair[1]);
		}
		return out;
	}
}