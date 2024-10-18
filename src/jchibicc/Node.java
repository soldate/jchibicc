package jchibicc;

//This file contains a recursive descent parser for C.
//
//Most functions in this file are named after the symbols they are
//supposed to read from an input token list. For example, stmt() is
//responsible for reading a statement from a token list. The function
//then construct an AST node representing a statement.
//
//Each function conceptually returns two values, an AST node and
//remaining part of the input tokens.
//
//Input tokens are represented by a linked list. Unlike many recursive
//descent parsers, we don't have the notion of the "input token stream".
//Most parsing functions don't change the global state of the parser.
//So it is very easy to lookahead arbitrary number of tokens in this
//parser.

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
		ADDR,      // unary &
		DEREF,     // unary *		
		RETURN,    // "return"
		IF,        // "if"
		FOR,       // "for" or "while"
		BLOCK,     // { ... }
		EXPR_STMT, // Expression statement
		VAR,       // Variable
		NUM,       // Integer
	}

	Kind kind;   // Node kind
	Node next;   // Next node
	Type ty;     // Type, e.g. int or pointer to int
	Token token; // Representative token
	Node lhs;    // Left-hand side
	Node rhs;    // Right-hand side
	
	// "if" or "for" statement
    Node cond;
	Node then;
	Node els;
	Node init;
	Node inc;	
	
	// Block
	Node body; 
	
	Obj var;   // Used if kind == Node.Kind.VAR
	int val;   // Used if kind == Node.Kind.NUM

	Node(int val) {
		this.kind = Kind.NUM;
		this.val = val;
		this.token = tok;
	}

	Node(Kind kind) {
		this.kind = kind;
		this.token = tok;
	}

	Node(Obj var) {
		this.kind = Kind.VAR;
		this.var = var;
		this.token = tok;
	}

	Node(Kind kind, Node lhs, Node rhs) {
		this.kind = kind;
		this.lhs = lhs;
		this.rhs = rhs;
		this.token = tok;
	}
	
	Node(Kind kind, Node lhs, Node rhs, Token token) {
		this.kind = kind;
		this.lhs = lhs;
		this.rhs = rhs;
		this.token = token;
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
	//  | "while" "(" expr ")" stmt	
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

		if (tok_equals("while")) {
			Node node = new Node(Node.Kind.FOR);
			skip("(");
			node.cond = expr();
			skip(")");
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

		while (!tok_equals("}")) {
			cur = cur.next = stmt();
			Type.add_type(cur);
		}

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
			Token start = tok;
			
			if (tok_equals("==")) {
				node = new Node(Node.Kind.EQ, node, relational(), start);
				continue;
			}

			if (tok_equals("!=")) {
				node = new Node(Node.Kind.NE, node, relational(), start);
				continue;
			}

			return node;
		}
	}

	// relational = add ("<" add | "<=" add | ">" add | ">=" add)*
	private static Node relational() {
		Node node = add();

		for (;;) {
			Token start = tok;
			
			if (tok_equals("<")) {
				node = new Node(Node.Kind.LT, node, add(), start);
				continue;
			}

			if (tok_equals("<=")) {
				node = new Node(Node.Kind.LE, node, add(), start);
				continue;
			}

			if (tok_equals(">")) {
				node = new Node(Node.Kind.LT, add(), node, start);
				continue;
			}

			if (tok_equals(">=")) {
				node = new Node(Node.Kind.LE, add(), node, start);
				continue;
			}

			return node;
		}
	}
	
	// In C, `+` operator is overloaded to perform the pointer arithmetic.
	// If p is a pointer, p+n adds not n but sizeof(*p)*n to the value of p,
	// so that p+n points to the location n elements (not bytes) ahead of p.
	// In other words, we need to scale an integer value before adding to a
	// pointer value. This function takes care of the scaling.
	private static Node new_add(Node lhs, Node rhs, Token tok) {
	  Type.add_type(lhs);
	  Type.add_type(rhs);

	  // num + num
	  if (Type.is_integer(lhs.ty) && Type.is_integer(rhs.ty))
	    return new Node(Node.Kind.ADD, lhs, rhs);

	  if (lhs.ty.base != null && rhs.ty.base != null)
	    S.error("%s invalid operands", tok.toString());

	  // Canonicalize `num + ptr` to `ptr + num`.
	  if (lhs.ty.base == null && rhs.ty.base != null) {
	    Node tmp = lhs;
	    lhs = rhs;
	    rhs = tmp;
	  }

	  // ptr + num
	  rhs = new Node(Node.Kind.MUL, rhs, new Node(8));
	  return new Node(Node.Kind.ADD, lhs, rhs);
	}

	// Like `+`, `-` is overloaded for the pointer type.
	private static Node new_sub(Node lhs, Node rhs, Token tok) {
	  Type.add_type(lhs);
	  Type.add_type(rhs);

	  // num - num
	  if (Type.is_integer(lhs.ty) && Type.is_integer(rhs.ty))
	    return new Node(Node.Kind.SUB, lhs, rhs);

	  // ptr - num
	  if (lhs.ty.base != null && Type.is_integer(rhs.ty)) {
	    rhs = new Node(Node.Kind.MUL, rhs, new Node(8));
	    Type.add_type(rhs);
	    Node node = new Node(Node.Kind.SUB, lhs, rhs);
	    node.ty = lhs.ty;
	    return node;
	  }

	  // ptr - ptr, which returns how many elements are between the two.
	  if (lhs.ty.base != null && rhs.ty.base != null) {
	    Node node = new Node(Node.Kind.SUB, lhs, rhs);
	    node.ty = Type.ty_int;
	    return new Node(Node.Kind.DIV, node, new Node(8));
	  }

	  S.error("%s invalid operands", tok.toString());
	  return null;
	}	

	// add = mul ("+" mul | "-" mul)*
	private static Node add() {
		Node node = mul();

		for (;;) {
			Token start = tok;
			
			if (tok_equals("+")) {
				node = new_add(node, mul(), start);
				continue;
			}

			if (tok_equals("-")) {
				node = new_sub(node, mul(), start);
				continue;
			}

			return node;
		}
	}

	// mul = primary ("*" primary | "/" primary)*
	private static Node mul() {
		Node node = unary();

		for (;;) {
			Token start = tok;
			
			if (tok_equals("*")) {
				node = new Node(Node.Kind.MUL, node, unary(), start);
				continue;
			}

			if (tok_equals("/")) {
				node = new Node(Node.Kind.DIV, node, unary(), start);
				continue;
			}

			return node;
		}
	}

	// unary = ("+" | "-" | "*" | "&") unary
	//  | primary
	private static Node unary() {
		if (tok_equals("+")) return unary();
		if (tok_equals("-")) return new Node(Node.Kind.NEG, unary(), null);
		if (tok_equals("&")) return new Node(Node.Kind.ADDR, unary(), null);
		if (tok_equals("*")) return new Node(Node.Kind.DEREF, unary(), null);
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
