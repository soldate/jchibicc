package jchibicc;

import java.io.PrintStream;
import java.util.regex.Pattern;

// S = static methods (boilerplate code)
class S {
	
	private static Pattern pIsNumeric = Pattern.compile("-?\\d+(\\.\\d+)?");
	
	static boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		return pIsNumeric.matcher(strNum).matches();
	}	

	static void error(String s, Object... o) {
		printf(System.err, s, o);
		System.exit(1);
	}

	static void printf(String s, Object... o) {
		printf(System.out, s, o);
	}

	private static void printf(PrintStream out, String s, Object... o) {
		out.printf(s, o);
	}	
	
}
