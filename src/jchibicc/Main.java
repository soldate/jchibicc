package jchibicc;

import java.io.PrintStream;

public class Main {

	private static void error(String s, Object... o) {
		printf(System.err, s, o);
	}

	private static void printf(String s, Object... o) {
		printf(System.out, s, o);
	}

	private static void printf(PrintStream out, String s, Object... o) {
		out.printf(s, o);
	}

	private static int pIndex = 0;
	
	private static String getNextNumber(String p) {		
		int indexOfMore = p.indexOf('+', pIndex);
		int indexOfLess = p.indexOf('-', pIndex);

		int pIndexInitial = pIndex;
		
		if (indexOfMore == -1 && indexOfLess == -1) pIndex = p.length();
		else if (indexOfMore == -1) pIndex = indexOfLess;
		else if (indexOfLess == -1) pIndex = indexOfMore;
		else if (indexOfMore < indexOfLess) pIndex = indexOfMore;
		else if (indexOfMore > indexOfLess) pIndex = indexOfLess;

		return p.substring(pIndexInitial, pIndex);
	}	

	public static void main(String[] args) {
		if (args.length != 1) {
			error("%s: invalid number of arguments\n", args[0]);
			return;
		}

		String p = args[0];

		printf("  .globl main\n");
		printf("main:\n");
		printf("  mov $%d, %%rax\n", Long.parseLong(getNextNumber(p)));

		while (pIndex < p.length()) {
			if (p.charAt(pIndex) == '+') {
				pIndex++;
				printf("  add $%d, %%rax\n", Long.parseLong(getNextNumber(p)));
				continue;
			}

			if (p.charAt(pIndex) == '-') {
				pIndex++;
				printf("  sub $%d, %%rax\n", Long.parseLong(getNextNumber(p)));
				continue;
			}

			error("unexpected character: '%c'\n", p.charAt(pIndex));
			return;
		}

		printf("  ret\n");
	}

}
