package jchibicc;

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
		ASSIGN,    // =
		RETURN,    // "return"
		IF,        // "if"
		FOR,       // "for"
		BLOCK,     // { ... }
		EXPR_STMT, // Expression statement
		VAR,       // Variable
		NUM,       // Integer
	}

	Kind kind; // Node kind
	Node next; // Next node
	Node lhs;  // Left-hand side
	Node rhs;  // Right-hand side
	
	// "if" or "for" statement
    Node cond;
	Node then;
	Node els;
	Node init;
	Node inc;	
	
	// Block
	Node body; 
	
	Obj var;   // Used if kind == ND_VAR
	int val;   // Used if kind == ND_NUM

	Node(int val) {
		this.kind = Kind.NUM;
		this.val = val;
	}

	Node(Kind kind) {
		this.kind = kind;
	}

	Node(Obj var) {
		this.kind = Kind.VAR;
		this.var = var;
	}

	Node(Kind kind, Node lhs, Node rhs) {
		this.kind = kind;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	// ==================
	// Parser code (static)
	// ==================

	private static Obj locals;

	// Ensure that the current token is `op`.
	private static void skip(String op) {
		if (!tok_equals(op)) S.error("expected '%s'", op);
	}

	// Find a local variable by name.
	private static Obj find_var(String name) {
		if (locals == null) return null;
		for (Obj tmp = locals; tmp != null; tmp = tmp.next)
			if (tmp.name.equals(name)) return tmp;
		return null;
	}

	// if true, move to the next token
	private static boolean tok_equals(String s) {
		if (tok.equals(s)) {
			tok = tok.next;
			return true;
		} else return false;
	}

	// stmt = "return" expr ";" 
	//	| "if" "(" expr ")" stmt ("else" stmt)?
	//  | "for" "(" expr-stmt expr? ";" expr? ")" stmt
	//  | "{" compound-stmt
	//  | expr-stmt
	private static Node stmt() {
		if (tok_equals("return")) {
			Node node = new Node(Node.Kind.RETURN, expr(), null);
			skip(";");
			return node;
		}

		if (tok_equals("if")) {
			Node node = new Node(Node.Kind.IF);
			skip("(");			
			
			node.cond = expr();
			
			skip(")");
			
			node.then = stmt();
			
			if (tok_equals("else")) node.els = stmt();
			return node;
		}

		if (tok_equals("for")) {
			Node node = new Node(Node.Kind.FOR);
			skip("(");

			node.init = expr_stmt();

			if (!tok_equals(";")) {
				node.cond = expr();
				skip(";");
			}			

			if (!tok_equals(")")) {
				node.inc = expr();
				skip(")");
			}			

			node.then = stmt();
			return node;
		}

		if (tok_equals("{")) return compound_stmt();

		return expr_stmt();
	}

	// compound-stmt = stmt* "}"
	private static Node compound_stmt() {
		Node head = new Node(0);
		Node cur = head;

		while (!tok_equals("}"))
			cur = cur.next = stmt();

		Node node = new Node(Node.Kind.BLOCK);
		node.body = head.next;
		return node;
	}

	// expr-stmt = expr ";"
	private static Node expr_stmt() {
		if (tok_equals(";")) {
			return new Node(Node.Kind.BLOCK);
		}
		Node node = new Node(Node.Kind.EXPR_STMT, expr(), null);
		skip(";");
		return node;
	}

	// expr = equality
	private static Node expr() {
		return assign();
	}

	// assign = equality ("=" assign)?
	private static Node assign() {
		Node node = equality();
		if (tok_equals("=")) node = new Node(Node.Kind.ASSIGN, node, assign());
		return node;
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
			skip(")");
			return node;
		}

		if (tok.kind == Token.Kind.IDENT) {
			Obj var = find_var(tok.str);
			if (var == null) {
				var = new Obj(tok.str, locals);
				locals = var;
			}
			Node node = new Node(var);
			tok = tok.next;
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

	public static Function parse(Token token) {
		tok = token;
		skip("{");
		Function prog = new Function();
		prog.body = compound_stmt();
		prog.locals = locals;
		return prog;
	}

}
