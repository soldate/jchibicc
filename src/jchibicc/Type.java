package jchibicc;

class Type {
	enum Kind {
		INT, PTR,
	}

	Kind kind;
	
	// Pointer
	Type base;
	
	// Declaration
	Token name;

	Type() {
	}

	Type(Kind kind) {
		this.kind = kind;
	}

	static Type ty_int = new Type(Kind.INT);

	static boolean is_integer(Type ty) {
		return ty.kind == Kind.INT;
	}

	static Type pointer_to(Type base) {
		Type ty = new Type();
		ty.kind = Kind.PTR;
		ty.base = base;
		return ty;
	}

	static void add_type(Node node) {
		if (node == null || node.ty != null) return;

		add_type(node.lhs);
		add_type(node.rhs);
		add_type(node.cond);
		add_type(node.then);
		add_type(node.els);
		add_type(node.init);
		add_type(node.inc);

		for (Node n = node.body; n != null; n = n.next)
			add_type(n);

		switch (node.kind) {
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case NEG:
		case ASSIGN:
			node.ty = node.lhs.ty;
			return;
		case EQ:
		case NE:
		case LT:
		case LE:
		case NUM:
			node.ty = ty_int;
			return;
		case FUNCALL:
		    node.ty = ty_int;
		    return;			
		case VAR:
			node.ty = node.var.ty;
			return;
		case ADDR:
			node.ty = pointer_to(node.lhs.ty);
			return;
		case DEREF:
			if (node.lhs.ty.kind != Kind.PTR) 
				S.error("%s invalid pointer dereference", node.token.toString());
			node.ty = node.lhs.ty.base;
			return;
		default:
			break;
		}
	}
}
