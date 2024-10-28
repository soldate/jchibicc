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
	
	Obj var;   // Used if kind == Kind.VAR
	int val;   // Used if kind == Kind.NUM

	@Override
	public String toString() {
		if (token != null) return token.toString();
		else return super.toString();
	}	
	
	// ==================
	// Parser code (static)
	// ==================

	private static Obj locals;
	private static Obj globals;
	
	private static Node new_head() {
		return new Node();
	}
	
	private static Node new_node(Kind kind, Token tok) {
		Node node = new Node();
		node.kind = kind;
		node.token = tok;
		return node;
	}

	private static Node new_binary(Kind kind, Node lhs, Node rhs, Token tok) {
		Node node = new_node(kind, tok);
		node.lhs = lhs;
		node.rhs = rhs;
		return node;
	}

	private static Node new_unary(Kind kind, Node expr, Token tok) {
		Node node = new_node(kind, tok);
		node.lhs = expr;
		return node;
	}

	private static Node new_num(int val, Token tok) {
		Node node = new_node(Kind.NUM, tok);
		node.val = val;
		return node;
	}

	private static Node new_var_node(Obj var, Token tok) {
		Node node = new_node(Kind.VAR, tok);
		node.var = var;
		return node;
	}

	private static Obj new_var(String name, Type ty) {
		Obj var = new Obj();
		var.name = name;
		var.ty = ty;
		return var;
	}

	private static Obj new_lvar(Type ty) {
		Obj var = new_var(ty.name.str, ty);
		var.is_local = true;
		var.next = locals;
		locals = var;
		return var;
	}

	private static Obj new_gvar(Type ty) {
		Obj var = new_var(ty.name.str, ty);
		var.next = globals;
		globals = var;
		return var;
	}

	// Ensure that the current token is `op`.
	private static void skip(String op) {
		if (!tok.equals(op)) S.error("expected '%s'", op);
		else tok = tok.next;
	}

	// Find a local variable by name.
	private static Obj find_var(String name) {
		for (Obj var = locals; var != null; var = var.next)
			if (var.name.equals(name)) return var;
		for (Obj var = globals; var != null; var = var.next)
			if (var.name.equals(name)) return var;		
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
	//  | "[" num "]" type-suffix
	//  | ε
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
	    Type ty2 = type_suffix(ty);
	    return Type.array_of(ty2, sz);
	  }
	  
	  return ty;
	}

	// declarator = "*"* ident type-suffix
	private static Type declarator(Type ty) {
		while (consume("*"))
			ty = Type.pointer_to(ty);

		if (tok.kind != Token.Kind.IDENT) 
			S.error("%s expected a variable name\n", tok.toString());
		
		Token start = tok;
		tok = tok.next;
		ty = type_suffix(ty);		
		ty.name = start;
		
		return ty;
	}

	// declaration = declspec (declarator ("=" expr)? ("," declarator ("=" expr)?)*)? ";"
	private static Node declaration() {
		Type basety = declspec();

		Node head = new_head();
		Node cur = head;
		int i = 0;

		while (!tok.equals(";")) {
			if (i++ > 0) skip(",");

			Type ty = declarator(basety);
			Obj var = new_lvar(ty);

			if (!tok.equals("=")) continue;			

			Node lhs = new_var_node(var, ty.name);
			tok = tok.next;
			Node rhs = assign();
			Node node = new_binary(Kind.ASSIGN, lhs, rhs, tok);
			cur = cur.next = new_unary(Kind.EXPR_STMT, node, tok);
		}

		Node node = new_node(Kind.BLOCK, tok);
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
			Node node = new_node(Kind.RETURN, tok);			
			tok = tok.next;
			node.lhs = expr();
			skip(";");
			return node;
		}

		if (tok.equals("if")) {
			Node node = new_node(Kind.IF, tok);
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
			Node node = new_node(Kind.FOR, tok);
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
			Node node = new_node(Kind.FOR, tok);
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
		Node node = new_node(Kind.BLOCK, tok);
		
		Node head = new_head();
		Node cur = head;

		while (!tok.equals("}")) {
			if (tok.equals("int")) cur = cur.next = declaration();
			else cur = cur.next = stmt();
			Type.add_type(cur);
		}
		
		node.body = head.next;
		tok = tok.next;
		return node;
	}

	// expr-stmt = expr ";"
	private static Node expr_stmt() {
		if (tok.equals(";")) {
			Node node = new_node(Kind.BLOCK, tok);
			tok = tok.next;
			return node;
		}
		Node node = new_node(Kind.EXPR_STMT, tok);
		node.lhs = expr();
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
			node = new_binary(Kind.ASSIGN, node, assign(), start);
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
				node = new_binary(Kind.EQ, node, relational(), start);
				continue;
			}

			if (tok.equals("!=")) {
				tok = tok.next;
				node = new_binary(Kind.NE, node, relational(), start);
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
				node = new_binary(Kind.LT, node, add(), start);
				continue;
			}

			if (tok.equals("<=")) {
				tok = tok.next;
				node = new_binary(Kind.LE, node, add(), start);
				continue;
			}

			if (tok.equals(">")) {
				tok = tok.next;
				node = new_binary(Kind.LT, add(), node, start);
				continue;
			}

			if (tok.equals(">=")) {
				tok = tok.next;
				node = new_binary(Kind.LE, add(), node, start);
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
	    return new_binary(Kind.ADD, lhs, rhs, tok);

	  if (lhs.ty.base != null && rhs.ty.base != null)
	    S.error("%s invalid operands", tok.toString());

	  // Canonicalize `num + ptr` to `ptr + num`.
	  if (lhs.ty.base == null && rhs.ty.base != null) {
	    Node tmp = lhs;
	    lhs = rhs;
	    rhs = tmp;
	  }

	  // ptr + num
	  rhs = new_binary(Kind.MUL, rhs, new_num(lhs.ty.base.size, tok), tok);
	  return new_binary(Kind.ADD, lhs, rhs, tok);
	}

	// Like `+`, `-` is overloaded for the pointer type.
	private static Node new_sub(Node lhs, Node rhs, Token tok) {
	  Type.add_type(lhs);
	  Type.add_type(rhs);

	  // num - num
	  if (Type.is_integer(lhs.ty) && Type.is_integer(rhs.ty))
	    return new_binary(Kind.SUB, lhs, rhs, tok);

	  // ptr - num
	  if (lhs.ty.base != null && Type.is_integer(rhs.ty)) {
	    rhs = new_binary(Kind.MUL, rhs, new_num(lhs.ty.base.size, tok), tok);
	    Type.add_type(rhs);
	    Node node = new_binary(Kind.SUB, lhs, rhs, tok);
	    node.ty = lhs.ty;
	    return node;
	  }

	  // ptr - ptr, which returns how many elements are between the two.
	  if (lhs.ty.base != null && rhs.ty.base != null) {
	    Node node = new_binary(Kind.SUB, lhs, rhs, tok);
	    node.ty = Type.ty_int;
	    return new_binary(Kind.DIV, node, new_num(lhs.ty.base.size, tok), tok);
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
				node = new_binary(Kind.MUL, node, unary(), start);
				continue;
			}

			if (tok.equals("/")) {
				tok = tok.next;
				node = new_binary(Kind.DIV, node, unary(), start);
				continue;
			}

			return node;
		}
	}

	// unary = ("+" | "-" | "*" | "&") unary
	//  | postfix
	private static Node unary() {
		if (tok.equals("+")) {
			tok = tok.next;
			return unary();
		}
		if (tok.equals("-")) {
			tok = tok.next;
			return new_unary(Kind.NEG, unary(), tok);
		}
		if (tok.equals("&")) {
			tok = tok.next;
			return new_unary(Kind.ADDR, unary(), tok);
		}
		if (tok.equals("*")) {
			tok = tok.next;
			return new_unary(Kind.DEREF, unary(), tok);
		}
		return postfix();
	}

	// postfix = primary ("[" expr "]")*
	private static Node postfix() {
	  Node node = primary();

	  while (tok.equals("[")) {
	    // x[y] is short for *(x+y)
	    Token start = tok;
	    tok = tok.next;
	    Node idx = expr();
	    skip("]");
	    node = new_unary(Kind.DEREF, new_add(node, idx, start), start);
	  }
	  return node;
	}
	
	// funcall = ident "(" (assign ("," assign)*)? ")"
	private static Node funcall() {
	  Token start = tok;
	  tok = tok.next.next;

	  Node head = new_head();
	  Node cur = head;

	  while (!tok.equals(")")) {
	    if (cur != head)
	      skip(",");
	    cur = cur.next = assign();
	  }

	  skip(")");

	  Node node = new_node(Kind.FUNCALL, start);
	  node.funcname = start.toString();
	  node.args = head.next;
	  return node;
	}

	// primary = "(" expr ")" | "sizeof" unary | ident func-args? | num
	private static Node primary() {
		if (tok.equals("(")) {
			tok = tok.next;
			Node node = expr();
			skip(")");
			return node;
		}

		if (tok.equals("sizeof")) {
			tok = tok.next;
			Node node = unary();
			Type.add_type(node);
			return new_num(node.ty.size, tok);
		}

		if (tok.kind == Token.Kind.IDENT) {
			// Function call
			if (tok.next.equals("("))
			      return funcall();

		    // Variable			
			Obj var = find_var(tok.str);
			if (var == null) {
				S.error("%s undefined variable\n", tok);
			}
			Node node = new_var_node(var, tok);
			tok = tok.next;
			return node;
		}

		if (tok.kind == Token.Kind.NUM) {
			Node node = new_num(tok.val, tok);
			tok = tok.next;
			return node;
		}

		S.error("%s expected an expression", tok);
		return null;
	}
	
	private static void create_param_lvars(Type param) {
		if (param != null) {
			create_param_lvars(param.next);
			new_lvar(param);
		}
	}

	private static void function(Type basety) {
		Type ty = declarator(basety);

		Obj fn = new_gvar(ty);
		fn.is_function = true;
		
		locals = null;
	    create_param_lvars(ty.params);
		fn.params = locals;		

		skip("{");
		fn.body = compound_stmt();
		fn.locals = locals;
	}

	private static void global_variable(Type basety) {
		boolean first = true;

		while (!consume(";")) {
			if (!first) skip(",");
			first = false;

			Type ty = declarator(basety);
			new_gvar(ty);
		}
	}

	// Lookahead tokens and returns true if a given token is a start
	// of a function definition or declaration.
	private static boolean is_function() {
		if (tok.equals(";")) return false;

		Type dummy = new Type();
		Token start = tok;
		Type ty = declarator(dummy);
		tok = start;
		
		return ty.kind == Type.Kind.FUNC;
	}
		
	private static Token tok;

	// program = function-definition*
	public static Obj parse(Token token) {
		globals = null;
		tok = token;
		
		while (tok.kind != Token.Kind.EOF) {
			Type basety = declspec();	
		    
			// Function
		    if (is_function()) {
		      function(basety);
		      continue;
		    }

		    // Global variable
		    global_variable(basety);
		}
		return globals;
	}

}
