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
	
	public static void main(String[] args) {
		if (args.length != 1) {
			error("%s: invalid number of arguments\n", args[0]);
			return;
		}

		printf("  .globl main\n");
		printf("main:\n");
		printf("  mov $%d, %%rax\n", Integer.parseInt(args[0]));
		printf("  ret\n");
	}

}
