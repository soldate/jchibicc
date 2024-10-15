package jchibicc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	private static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
	static boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false; 
	    }
	    return pattern.matcher(strNum).matches();
	}	
	
	private enum TokenKind {
		TK_PUNCT, // Punctuators
		TK_NUM, // Numeric literals
	};
	
	static class Token {
		TokenKind kind;
	    String value;
	    int loc;
	    int len;
	    int val; // If kind is TK_NUM, its value

	    Token(String value, int start, int end) {
	        this.value = value;
	        this.loc = start;
	        this.len = end - start;
	        if (isNumeric(value)) {
	        	kind = TokenKind.TK_NUM;
	        	val = Integer.parseInt(value);	        	
	        } else kind = TokenKind.TK_PUNCT;
	    }
	    
	    boolean equals(String s) {
	        return (this.value.equals(s));
	    }

		@Override
		public String toString() {
			return value;
		}	  
	    
	}

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

		String code = args[0];
		List<Token> tokens = tokenize(code);

		printf("  .globl main\n");
		printf("main:\n");
		
		// The first token must be a number
		printf("  mov $%d, %%rax\n", Long.parseLong(tokens.get(0).toString()));

		for(int i=0; i<tokens.size(); i++) {
			Token tok = tokens.get(i);
			
			if (tok.equals("+")) {
				i++;
				tok = tokens.get(i);
				printf("  add $%d, %%rax\n", tok.val);
				continue;
			}

			if (tok.equals("-")) {
				i++;
				tok = tokens.get(i);				
				printf("  sub $%d, %%rax\n", tok.val);
				continue;
			}
			
		}

		printf("  ret\n");
	}

	private static List<Token> tokenize(String code) {
        String regex = "\\w+|[{}();=+\\-*/]";
        
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(code);

        List<Token> tokens = new ArrayList<>();
        
        while (matcher.find()) {
            String token = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            tokens.add(new Token(token, start, end));
        }
        
        return tokens;
	}

}
