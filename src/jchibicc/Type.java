package jchibicc;

class Type {
	enum Kind {
		INT, PTR,
	}

	Kind kind;
	Type base;

	Type() {}

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
		case VAR:
		case NUM:
			node.ty = ty_int;
			return;
		case ADDR:
			node.ty = pointer_to(node.lhs.ty);
			return;
		case DEREF:
			if (node.lhs.ty.kind == Kind.PTR) node.ty = node.lhs.ty.base;
			else node.ty = ty_int;
			return;
		default:
			break;
		}
	}
}
