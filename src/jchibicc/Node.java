package jchibicc;

import java.util.List;

// Node (objs) + Parser (static methods)
class Node {

	// ==================
	// Node class (objs)
	// ==================

	enum Kind {
		ADD,       // +
		SUB,       // -
		MUL,       // *
		DIV,       // /
		NEG,       // unary -
		EQ,        // ==
		NE,        // !=
		LT,        // <
		LE,        // <=
		EXPR_STMT, // Expression statement
		NUM,       // Integer
	}

	Kind kind; // Node kind
	Node next; // Next node
	Node lhs;  // Left-hand side
	Node rhs;  // Right-hand side
	int val;   // Used if kind == ND_NUM

	Node(int val) {
		super();
		this.kind = Kind.NUM;
		this.val = val;
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
	private static boolean tok_equals(String s) {
		if (tok.equals(s)) {
			tok = tok.next;
			return true;
		} else return false;
	}
	
	// stmt = expr-stmt
	private static Node stmt() {
	  return expr_stmt();
	}

	// expr-stmt = expr ";"
	private static Node expr_stmt() {
	  Node node = new Node(Node.Kind.EXPR_STMT, expr(), null);
	  if (!tok_equals(";")) S.error("expected ';'");
	  return node;
	}	

	// expr = equality
	private static Node expr() {
		return equality();
	}

	// equality = relational ("==" relational | "!=" relational)*
	private static Node equality() {
		Node node = relational();

		for (;;) {
			if (tok_equals("==")) {
				node = new Node(Node.Kind.EQ, node, relational());
				continue;
			}

			if (tok_equals("!=")) {
				node = new Node(Node.Kind.NE, node, relational());
				continue;
			}

			return node;
		}
	}

	// relational = add ("<" add | "<=" add | ">" add | ">=" add)*
	private static Node relational() {
		Node node = add();

		for (;;) {
			if (tok_equals("<")) {
				node = new Node(Node.Kind.LT, node, add());
				continue;
			}

			if (tok_equals("<=")) {
				node = new Node(Node.Kind.LE, node, add());
				continue;
			}

			if (tok_equals(">")) {
				node = new Node(Node.Kind.LT, add(), node);
				continue;
			}

			if (tok_equals(">=")) {
				node = new Node(Node.Kind.LE, add(), node);
				continue;
			}

			return node;
		}
	}

	// add = mul ("+" mul | "-" mul)*
	private static Node add() {
		Node node = mul();

		for (;;) {
			if (tok_equals("+")) {
				node = new Node(Node.Kind.ADD, node, mul());
				continue;
			}

			if (tok_equals("-")) {
				node = new Node(Node.Kind.SUB, node, mul());
				continue;
			}

			return node;
		}
	}

	// mul = primary ("*" primary | "/" primary)*
	private static Node mul() {
		Node node = unary();

		for (;;) {
			if (tok_equals("*")) {
				node = new Node(Node.Kind.MUL, node, unary());
				continue;
			}

			if (tok_equals("/")) {
				node = new Node(Node.Kind.DIV, node, unary());
				continue;
			}

			return node;
		}
	}

	// unary = ("+" | "-") unary | primary
	private static Node unary() {
		if (tok_equals("+")) return unary();
		if (tok_equals("-")) return new Node(Node.Kind.NEG, unary(), null);
		return primary();
	}

	// primary = "(" expr ")" | num
	private static Node primary() {

		if (tok_equals("(")) {
			Node node = expr();
			if (!tok_equals(")")) S.error("expected ')'");
			return node;
		}

		if (tok.kind == Token.Kind.NUM) {
			Node node = new Node(tok.val);
			tok = tok.next;
			return node;
		}

		S.error(tok.str, " expected an expression");
		return null;
	}

	private static Token tok;
	
	public static Node parse(Token token) {		  
		  Node head = new Node(0);
		  Node cur = head;
		  tok = token;
		  
		  while (tok.kind != Token.Kind.EOF) {
			  cur = cur.next = stmt();
		  }
		  
		  return head.next;
	}

}
