package lib;

import java.io.PrintWriter;
import java.io.StringWriter;

public class log {
	public static final String RESET = "\u001B[0m";
	public static final String RED = "\u001B[31m";
	public static final String GREEN = "\u001B[32m";
	public static final String CYAN = "\u001B[36m";

	/**
	 * Display error msg
	 *
	 * @param in message to print
	 */
	public static void e(String in) {
		System.out.println(RED + "[Error] " + in + RESET);
	}

	/**
	 * Display success msg
	 *
	 * @param in message to print
	 */
	public static void s(String in) {
		System.out.println(GREEN + "[Success] " + in + RESET);
	}

	/**
	 * Display informative msg
	 *
	 * @param in message to print
	 */
	public static void i(String in) {
		System.out.println(CYAN + "[Info] " + in + RESET);
	}

	/**
	 * Display error msg
	 *
	 * @param e         the exception object to gain data from
	 * @param className the name of the class where the error happened
	 * @param FuncName  the name of the function where the error happened
	 */
	public static void e(Exception e, String className, String FuncName) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		log.e(className + "." + FuncName + "(..)" + " : " + sw.toString());
	}
}
