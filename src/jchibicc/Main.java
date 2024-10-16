package jchibicc;

import java.util.List;

public class Main {

	public static void main(String[] args) {
		if (args.length != 1) {
			S.error("%s: invalid number of arguments\n", args[0]);
			return;
		}

		String c_code = args[0];
		
		// Step 1 - Tokenize
		List<Token> tokens = Token.tokenize(c_code);
		
		// Step 2 - Parse (generate AST)
		Node node = Node.parse(tokens);

		// Step 3 - Traverse the AST to emit assembly 
		Assembly.emit(node);		
	}

}