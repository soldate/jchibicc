package jchibicc;

import java.awt.desktop.PrintFilesEvent;

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
		FUNCALL,   // Function call
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
	
	// Function call
	String funcname;
	Node args;
	
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
	
	Node(Kind kind, Node lhs) {
		this.kind = kind;
		this.lhs = lhs;
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

	Node(Obj var, Token token) {
		this.kind = Kind.VAR;
		this.var = var;
		this.token = token;
	}
	
	Node(Kind kind, Token token) {
		this.kind = kind;
		this.token = token;
	}

	@Override
	public String toString() {
		if (token != null) return token.toString();
		else return super.toString();
	}	
	
	// ==================
	// Parser code (static)
	// ==================

	private static Obj locals;

	// Ensure that the current token is `op`.
	private static void skip(String op) {
		if (!tok.equals(op)) S.error("expected '%s'", op);
		else tok = tok.next;
	}

	// Find a local variable by name.
	private static Obj find_var(String name) {
		if (locals == null) return null;
		for (Obj tmp = locals; tmp != null; tmp = tmp.next)
			if (tmp.name.equals(name)) return tmp;
		return null;
	}

	private static boolean consume(String s) {
		if (tok.equals(s)) {
			tok = tok.next;
			return true;
		} else return false;
	}

	private static int get_number() {
		if (tok.kind != Token.Kind.NUM) S.error("%s expected a number", tok);
		return tok.val;
	}

	// declspec = "int"
	private static Type declspec() {
		skip("int");
		return Type.ty_int;
	}

	// func-params = (param ("," param)*)? ")"
	// param       = declspec declarator
	private static Type func_params(Type ty) {
	  Type head = new Type();
	  Type cur = head;

	  while (!tok.equals(")")) {
	    if (cur != head)
	      skip(",");
	    Type basety = declspec();
	    Type ty2 = declarator(basety);
	    cur = cur.next = Type.copy_type(ty2);
	  }

	  ty = Type.func_type(ty);
	  ty.params = head.next;
	  tok = tok.next;
	  return ty;
	}

	// type-suffix = "(" func-params
//	             | "[" num "]"
//	             | ε
	private static Type type_suffix(Type ty) {
	  if (tok.equals("(")) {
		tok = tok.next;
	    return func_params(ty);
	  }

	  if (tok.equals("[")) {
		tok = tok.next;
	    int sz = get_number();
	    tok = tok.next;
	    skip("]");
	    return Type.array_of(ty, sz);
	  }
	  
	  return ty;
	}

	// declarator = "*"* ident type-suffix
	private static Type declarator(Type ty) {
		while (consume("*"))
			ty = Type.pointer_to(ty);

		if (tok.kind != Token.Kind.IDENT) 
			S.error("%s expected a variable name", tok.toString());
		
		Token start = tok;
		tok = tok.next;
		ty = type_suffix(ty);		
		ty.name = start;
		
		return ty;
	}

	// declaration = declspec (declarator ("=" expr)? ("," declarator ("="
	// expr)?)*)? ";"
	private static Node declaration() {
		Type basety = declspec();

		Node head = new Node(0);
		Node cur = head;
		int i = 0;

		while (!tok.equals(";")) {
			if (i++ > 0) skip(",");

			Type ty = declarator(basety);
			Obj var = new Obj(ty, locals);
			locals = var;

			if (!tok.equals("=")) continue;			

			Node lhs = new Node(var, ty.name);
			tok = tok.next;
			Node rhs = assign();
			Node node = new Node(Node.Kind.ASSIGN, lhs, rhs);
			cur = cur.next = new Node(Node.Kind.EXPR_STMT, node);
		}

		Node node = new Node(Node.Kind.BLOCK);
		node.body = head.next;
		tok = tok.next;
		return node;
	}

	// stmt = "return" expr ";"
	// | "if" "(" expr ")" stmt ("else" stmt)?
	// | "for" "(" expr-stmt expr? ";" expr? ")" stmt
	// | "while" "(" expr ")" stmt
	// | "{" compound-stmt
	// | expr-stmt
	private static Node stmt() {
		if (tok.equals("return")) {			
			Node node = new Node(Node.Kind.RETURN);			
			tok = tok.next;
			node.lhs = expr();
			skip(";");
			return node;
		}

		if (tok.equals("if")) {
			Node node = new Node(Node.Kind.IF);
			tok = tok.next;
			skip("(");						
			node.cond = expr();			
			skip(")");			
			node.then = stmt();			
			if (tok.equals("else")) {
				tok = tok.next;
				node.els = stmt();
			}
			return node;
		}

		if (tok.equals("for")) {
			Node node = new Node(Node.Kind.FOR);
			tok = tok.next;
			skip("(");

			node.init = expr_stmt();

			if (!tok.equals(";")) node.cond = expr();				
			skip(";");

			if (!tok.equals(")")) node.inc = expr();				
			skip(")");

			node.then = stmt();
			return node;
		}

		if (tok.equals("while")) {
			Node node = new Node(Node.Kind.FOR);
			tok = tok.next;
			skip("(");
			node.cond = expr();
			skip(")");
			node.then = stmt();
			return node;
		}

		if (tok.equals("{")) {
			tok = tok.next;
			return compound_stmt();
		}

		return expr_stmt();
	}

	// compound-stmt = stmt* "}"
	private static Node compound_stmt() {
		Node head = new Node(0);
		Node cur = head;

		while (!tok.equals("}")) {
			if (tok.equals("int")) cur = cur.next = declaration();
			else cur = cur.next = stmt();
			Type.add_type(cur);
		}

		Node node = new Node(Node.Kind.BLOCK);
		node.body = head.next;
		tok = tok.next;
		return node;
	}

	// expr-stmt = expr ";"
	private static Node expr_stmt() {
		if (tok.equals(";")) {
			Node node = new Node(Node.Kind.BLOCK);
			tok = tok.next;
			return node;
		}
		Node node = new Node(Node.Kind.EXPR_STMT, expr());
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
		Token start = tok;
		if (tok.equals("=")) {
			tok = tok.next;
			node = new Node(Node.Kind.ASSIGN, node, assign(), start);
		}
		return node;
	}

	// equality = relational ("==" relational | "!=" relational)*
	private static Node equality() {
		Node node = relational();

		for (;;) {
			Token start = tok;
			
			if (tok.equals("==")) {
				tok = tok.next;
				node = new Node(Node.Kind.EQ, node, relational(), start);
				continue;
			}

			if (tok.equals("!=")) {
				tok = tok.next;
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
			
			if (tok.equals("<")) {
				tok = tok.next;
				node = new Node(Node.Kind.LT, node, add(), start);
				continue;
			}

			if (tok.equals("<=")) {
				tok = tok.next;
				node = new Node(Node.Kind.LE, node, add(), start);
				continue;
			}

			if (tok.equals(">")) {
				tok = tok.next;
				node = new Node(Node.Kind.LT, add(), node, start);
				continue;
			}

			if (tok.equals(">=")) {
				tok = tok.next;
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
	  rhs = new Node(Node.Kind.MUL, rhs, new Node(lhs.ty.base.size));
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
	    rhs = new Node(Node.Kind.MUL, rhs, new Node(lhs.ty.base.size));
	    Type.add_type(rhs);
	    Node node = new Node(Node.Kind.SUB, lhs, rhs);
	    node.ty = lhs.ty;
	    return node;
	  }

	  // ptr - ptr, which returns how many elements are between the two.
	  if (lhs.ty.base != null && rhs.ty.base != null) {
	    Node node = new Node(Node.Kind.SUB, lhs, rhs);
	    node.ty = Type.ty_int;
	    return new Node(Node.Kind.DIV, node, new Node(lhs.ty.base.size));
	  }

	  S.error("%s invalid operands", tok.toString());
	  return null;
	}	

	// add = mul ("+" mul | "-" mul)*
	private static Node add() {
		Node node = mul();

		for (;;) {
			Token start = tok;
			
			if (tok.equals("+")) {
				tok = tok.next;
				node = new_add(node, mul(), start);
				continue;
			}

			if (tok.equals("-")) {
				tok = tok.next;
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
			
			if (tok.equals("*")) {
				tok = tok.next;
				node = new Node(Node.Kind.MUL, node, unary(), start);
				continue;
			}

			if (tok.equals("/")) {
				tok = tok.next;
				node = new Node(Node.Kind.DIV, node, unary(), start);
				continue;
			}

			return node;
		}
	}

	// unary = ("+" | "-" | "*" | "&") unary
	//  | primary
	private static Node unary() {
		if (tok.equals("+")) {
			tok = tok.next;
			return unary();
		}
		if (tok.equals("-")) {
			tok = tok.next;
			return new Node(Node.Kind.NEG, unary());
		}
		if (tok.equals("&")) {
			tok = tok.next;
			return new Node(Node.Kind.ADDR, unary());
		}
		if (tok.equals("*")) {
			tok = tok.next;
			return new Node(Node.Kind.DEREF, unary());
		}
		return primary();
	}

	// funcall = ident "(" (assign ("," assign)*)? ")"
	private static Node funcall() {
	  Token start = tok;
	  tok = tok.next.next;

	  Node head = new Node(0);
	  Node cur = head;

	  while (!tok.equals(")")) {
	    if (cur != head)
	      skip(",");
	    cur = cur.next = assign();
	  }

	  skip(")");

	  Node node = new Node(Node.Kind.FUNCALL, start);
	  node.funcname = start.toString();
	  node.args = head.next;
	  return node;
	}

	// primary = "(" expr ")" | ident func-args? | num
	private static Node primary() {
		if (tok.equals("(")) {
			tok = tok.next;
			Node node = expr();
			skip(")");
			return node;
		}

		if (tok.kind == Token.Kind.IDENT) {
		    // Function call
			if (tok.next.equals("("))
			      return funcall();

		    // Variable			
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

		S.error("%s expected an expression", tok);
		return null;
	}
	
	private static void create_param_lvars(Type param) {
		if (param != null) {
			create_param_lvars(param.next);
			Obj p = new Obj(param.name.toString(), param, locals);
			locals = p;
		}
	}

	private static Function function() {
		Type ty = declspec();
		ty = declarator(ty);

		locals = null;

		Function fn = new Function();
		fn.name = ty.name.toString();
	    create_param_lvars(ty.params);
		fn.params = locals;		

		skip("{");
		fn.body = compound_stmt();
		fn.locals = locals;
		return fn;
	}

	private static Token tok;

	// program = function-definition*
	public static Function parse(Token token) {
		tok = token;
		Function head = new Function();
		Function cur = head;

		while (tok.kind != Token.Kind.EOF)
			cur = cur.next = function();
		return head.next;
	}

}
