package jchibicc;

class Type {
	enum Kind {
		INT, PTR, FUNC, ARRAY,
	}

	Kind kind;
	
	// sizeof() value
	int size;

	// Pointer-to or array-of type. We intentionally use the same member
	// to represent pointer/array duality in C.
	//
	// In many contexts in which a pointer is expected, we examine this
	// member instead of "kind" member to determine whether a type is a
	// pointer or not. That means in many contexts "array of T" is
	// naturally handled as if it were "pointer to T", as required by
	// the C spec.
	Type base;

	// Declaration
	Token name;

	// Array
	int array_len;

	// Function type
	Type return_ty;
	Type params;
	Type next;	

	Type() {
	}

	Type(Kind kind) {
		this.kind = kind;
	}
	
	Type(Kind kind, int size) {
		this.kind = kind;
		this.size = size;
	}	
	
	@Override
	public String toString() {
		if (name != null && kind != null) return name.toString() + " " + kind.toString();
		else if (name != null) return name.toString();
		else if (kind != null) return kind.toString();
		else return super.toString();
	}

	static Type ty_int = new Type(Kind.INT, 8);

	static boolean is_integer(Type ty) {
		return ty.kind == Kind.INT;
	}

	static Type copy_type(Type ty) {
	  Type ret = new Type();
	  ret.kind = ty.kind;
	  ret.name = ty.name;
	  ret.base = ty.base;
	  ret.next = ty.next;
	  ret.params = ty.params;
	  ret.return_ty = ty.return_ty;
	  return ret;
	}	

	static Type pointer_to(Type base) {
		Type ty = new Type();
		ty.kind = Kind.PTR;
		ty.size = 8;
		ty.base = base;
		return ty;
	}

	static Type func_type(Type return_ty) {
		Type ty = new Type();
		ty.kind = Kind.FUNC;
		ty.return_ty = return_ty;
		return ty;
	}

	static Type array_of(Type base, int len) {
		  Type ty = new Type();
		  ty.kind = Kind.ARRAY;
		  ty.size = base.size * len;
		  ty.base = base;
		  ty.array_len = len;
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
		for (Node n = node.args; n != null; n = n.next)
		    add_type(n);		

		switch (node.kind) {
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case NEG:
		    node.ty = node.lhs.ty;
		    return;			
		case ASSIGN:
		    if (node.lhs.ty.kind == Kind.ARRAY)
		        S.error("%s not an lvalue", node.lhs.token);			
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
		    if (node.lhs.ty.kind == Kind.ARRAY)
		        node.ty = pointer_to(node.lhs.ty.base);
		      else node.ty = pointer_to(node.lhs.ty);
			return;
		case DEREF:
			if (node.lhs.ty.base == null) 
				S.error("%s invalid pointer dereference", node.token.toString());
			node.ty = node.lhs.ty.base;
			return;
		default:
			break;
		}
	}
}
