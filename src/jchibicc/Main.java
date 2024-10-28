package jchibicc;

public class Main {

	public static void main(String[] args) {
		if (args.length != 1) {
			S.error("%s: invalid number of arguments\n", args[0]);
			return;
		}

		String c_code = args[0];		
		
		Token tok = Token.tokenize(c_code);
		
		Obj prog = Node.parse(tok);
		
		// Traverse the AST to emit assembly. 
		Assembly.codegen(prog);		
	}

}