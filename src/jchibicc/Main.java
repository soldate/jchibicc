package jchibicc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	private static Pattern pIsNumeric = Pattern.compile("-?\\d+(\\.\\d+)?");

	static boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		return pIsNumeric.matcher(strNum).matches();
	}

	private enum NodeKind {
		ND_ADD, // +
		ND_SUB, // -
		ND_MUL, // *
		ND_DIV, // /
		ND_NEG, // unary -
		ND_EQ, // ==
		ND_NE, // !=
		ND_LT, // <
		ND_LE, // <=
		ND_NUM, // Integer
	}

	private enum TokenKind {
		TK_PUNCT, // Punctuators
		TK_NUM, // Numeric literals
		TK_EOF, // End-of-file markers
	}

	private static List<Token> tokens;
	private static int tokenIndex = 0;
	private static Token token;

	private static void nextToken() {
		tokenIndex++;
		if (tokenIndex < tokens.size()) {
			token = tokens.get(tokenIndex);
		} else {
			token = new Token("", 0, 0);
			token.kind = TokenKind.TK_EOF;
		}
	}

	static class Token {
		TokenKind kind;
		String value;
		int loc; // Token location
		int len; // Token length
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
			if (this.value.equals(s)) {
				nextToken();
				return true;
			} else return false;
		}

		@Override
		public String toString() {
			return value;
		}

	}

	static class Node {
		NodeKind kind; // Node kind
		Node lhs; // Left-hand side
		Node rhs; // Right-hand side
		int val; // Used if kind == ND_NUM

		Node(int val) {
			super();
			this.kind = NodeKind.ND_NUM;
			this.val = val;
			nextToken();
		}

		Node(NodeKind kind, Node lhs, Node rhs) {
			super();
			this.kind = kind;
			this.lhs = lhs;
			this.rhs = rhs;
		}

	}

	private static void error(String s, Object... o) {
		printf(System.err, s, o);
		System.exit(1);
	}

	private static void printf(String s, Object... o) {
		printf(System.out, s, o);
	}

	private static void printf(PrintStream out, String s, Object... o) {
		out.printf(s, o);
	}

	//
	// Tokenizer
	//

	private static List<Token> tokenize(String code) {
		String regex = "\\w+|[{}();]|==|<=|>=|!=|\\+\\+|--|&&|\\|\\||[+\\-*/<>=!]";

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

	//
	// Parser
	//
	
	// expr = equality
	static Node expr() {
	  return equality();
	}	

	// equality = relational ("==" relational | "!=" relational)*
	static Node equality() {
	  Node node = relational();

	  for (;;) {
	    if (token.equals("==")) {
	      node = new Node(NodeKind.ND_EQ, node, relational());
	      continue;
	    }

	    if (token.equals("!=")) {
	      node = new Node(NodeKind.ND_NE, node, relational());
	      continue;
	    }

	    return node;
	  }
	}

	// relational = add ("<" add | "<=" add | ">" add | ">=" add)*
	static Node relational() {
	  Node node = add();

	  for (;;) {
	    if (token.equals("<")) {
	      node = new Node(NodeKind.ND_LT, node, add());
	      continue;
	    }

	    if (token.equals("<=")) {
	      node = new Node(NodeKind.ND_LE, node, add());
	      continue;
	    }

	    if (token.equals(">")) {
	      node = new Node(NodeKind.ND_LT, add(), node);
	      continue;
	    }

	    if (token.equals(">=")) {
	      node = new Node(NodeKind.ND_LE, add(), node);
	      continue;
	    }

	    return node;
	  }
	}
	
	// add = mul ("+" mul | "-" mul)*
	static Node add() {
		Node node = mul();

		for (;;) {
			if (token.equals("+")) {
				node = new Node(NodeKind.ND_ADD, node, mul());
				continue;
			}

			if (token.equals("-")) {
				node = new Node(NodeKind.ND_SUB, node, mul());
				continue;
			}

			return node;
		}
	}	

	// mul = primary ("*" primary | "/" primary)*
	static Node mul() {
		Node node = unary();

		for (;;) {
			if (token.equals("*")) {
				node = new Node(NodeKind.ND_MUL, node, primary());
				continue;
			}

			if (token.equals("/")) {
				node = new Node(NodeKind.ND_DIV, node, primary());
				continue;
			}

			return node;
		}
	}

	// unary = ("+" | "-") unary | primary
	static Node unary() {
		if (token.equals("+")) return unary();
		if (token.equals("-")) return new Node(NodeKind.ND_NEG, unary(), null);
		return primary();
	}

	// primary = "(" expr ")" | num
	static Node primary() {

		if (token.equals("(")) {
			Node node = expr();
			if (!token.equals(")")) error("expected ')'");
			return node;
		}

		if (token.kind == TokenKind.TK_NUM) {
			Node node = new Node(token.val);
			return node;
		}

		error(token.value, " expected an expression");
		return null;
	}

	//
	// Code generator
	//

	static int depth;

	static void push() {
		printf("  push %%rax\n");
		depth++;
	}

	static void pop(String s) {
		printf("  pop %s\n", s);
		depth--;
	}

	static void gen_expr(Node node) {
		switch (node.kind) {
		case ND_NUM:
			printf("  mov $%d, %%rax\n", node.val);
			return;
		case ND_NEG:
			gen_expr(node.lhs);
			printf("  neg %%rax\n");
			return;
		default:
			break;
		}

		gen_expr(node.rhs);
		push();
		gen_expr(node.lhs);
		pop("%rdi");

		switch (node.kind) {
		case ND_ADD:
			printf("  add %%rdi, %%rax\n");
			return;
		case ND_SUB:
			printf("  sub %%rdi, %%rax\n");
			return;
		case ND_MUL:
			printf("  imul %%rdi, %%rax\n");
			return;
		case ND_DIV:
			printf("  cqo\n");
			printf("  idiv %%rdi\n");
			return;
		  case ND_EQ:
		  case ND_NE:
		  case ND_LT:
		  case ND_LE:
		    printf("  cmp %%rdi, %%rax\n");

		    if (node.kind == NodeKind.ND_EQ)
		      printf("  sete %%al\n");
		    else if (node.kind == NodeKind.ND_NE)
		      printf("  setne %%al\n");
		    else if (node.kind == NodeKind.ND_LT)
		      printf("  setl %%al\n");
		    else if (node.kind == NodeKind.ND_LE)
		      printf("  setle %%al\n");

		    printf("  movzb %%al, %%rax\n");
		    return;
		default:
			break;
		}

		error("invalid expression");
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			error("%s: invalid number of arguments\n", args[0]);
			return;
		}

		String code = args[0];
		tokens = tokenize(code);
		token = tokens.get(0);

		Node node = expr();

		printf("  .globl main\n");
		printf("main:\n");

		// Traverse the AST to emit assembly.
		gen_expr(node);
		printf("  ret\n");

		assert (depth == 0);
	}

}
