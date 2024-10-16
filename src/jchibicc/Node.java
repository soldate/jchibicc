package jchibicc;

import java.util.List;

// Node (objs) + Parser (static methods)
class Node {

	// ==================
	// Node class (objs)
	// ==================
	
	enum Kind {
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

	Kind kind; // Node kind
	Node lhs; // Left-hand side
	Node rhs; // Right-hand side
	int val; // Used if kind == ND_NUM

	Node(int val) {
		super();
		this.kind = Kind.ND_NUM;
		this.val = val;
		nextToken();
	}

	Node(Kind kind, Node lhs, Node rhs) {
		super();
		this.kind = kind;
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	// ==================
	// Parser code (static)
	// ==================

	// if true, move to the next token
	private static boolean equals(Token tok, String s) {
		if (tok.equals(s)) {
			nextToken();
			return true;
		} else return false;
	}
	
	// expr = equality
	private static Node expr() {
		return equality();
	}

	// equality = relational ("==" relational | "!=" relational)*
	private static Node equality() {
		Node node = relational();

		for (;;) {
			if (equals(actualToken, "==")) {
				node = new Node(Node.Kind.ND_EQ, node, relational());
				continue;
			}

			if (equals(actualToken, "!=")) {
				node = new Node(Node.Kind.ND_NE, node, relational());
				continue;
			}

			return node;
		}
	}

	// relational = add ("<" add | "<=" add | ">" add | ">=" add)*
	private static Node relational() {
		Node node = add();

		for (;;) {
			if (equals(actualToken, "<")) {
				node = new Node(Node.Kind.ND_LT, node, add());
				continue;
			}

			if (equals(actualToken, "<=")) {
				node = new Node(Node.Kind.ND_LE, node, add());
				continue;
			}

			if (equals(actualToken, ">")) {
				node = new Node(Node.Kind.ND_LT, add(), node);
				continue;
			}

			if (equals(actualToken, ">=")) {
				node = new Node(Node.Kind.ND_LE, add(), node);
				continue;
			}

			return node;
		}
	}

	// add = mul ("+" mul | "-" mul)*
	private static Node add() {
		Node node = mul();

		for (;;) {
			if (equals(actualToken, "+")) {
				node = new Node(Node.Kind.ND_ADD, node, mul());
				continue;
			}

			if (equals(actualToken, "-")) {
				node = new Node(Node.Kind.ND_SUB, node, mul());
				continue;
			}

			return node;
		}
	}

	// mul = primary ("*" primary | "/" primary)*
	private static Node mul() {
		Node node = unary();

		for (;;) {
			if (equals(actualToken, "*")) {
				node = new Node(Node.Kind.ND_MUL, node, unary());
				continue;
			}

			if (equals(actualToken, "/")) {
				node = new Node(Node.Kind.ND_DIV, node, unary());
				continue;
			}

			return node;
		}
	}

	// unary = ("+" | "-") unary | primary
	private static Node unary() {
		if (equals(actualToken, "+")) return unary();
		if (equals(actualToken, "-")) return new Node(Node.Kind.ND_NEG, unary(), null);
		return primary();
	}

	// primary = "(" expr ")" | num
	private static Node primary() {

		if (equals(actualToken, "(")) {
			Node node = expr();
			if (!equals(actualToken, ")")) S.error("expected ')'");
			return node;
		}

		if (actualToken.kind == Token.Kind.TK_NUM) {
			Node node = new Node(actualToken.val);
			return node;
		}

		S.error(actualToken.value, " expected an expression");
		return null;
	}

	// parser control (one token at time)
	private static List<Token> tokens;
	private static int tokenIndex;
	private static Token actualToken;

	private static void nextToken() {
		tokenIndex++;
		if (tokenIndex < tokens.size()) {
			actualToken = tokens.get(tokenIndex);
		} else {
			actualToken = new Token(Token.Kind.TK_EOF);
		}
	}
	
	public static Node parse(List<Token> tokens) {
		tokenIndex = 0; // reset parser control
		
		Node.tokens = tokens;
		actualToken = tokens.get(tokenIndex);
		return expr();
	}

}
