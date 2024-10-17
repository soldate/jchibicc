package jchibicc;

// Code Generator
class Assembly {

	private static void push() {
		S.printf("  push %%rax\n");
		depth++;
	}

	private static void pop(String s) {
		S.printf("  pop %s\n", s);
		depth--;
	}

	private static void gen_expr(Node node) {
		switch (node.kind) {
		case NUM:
			S.printf("  mov $%d, %%rax\n", node.val);
			return;
		case NEG:
			gen_expr(node.lhs);
			S.printf("  neg %%rax\n");
			return;
		default:
			break;
		}

		gen_expr(node.rhs);
		push();
		gen_expr(node.lhs);
		pop("%rdi");

		switch (node.kind) {
		case ADD:
			S.printf("  add %%rdi, %%rax\n");
			return;
		case SUB:
			S.printf("  sub %%rdi, %%rax\n");
			return;
		case MUL:
			S.printf("  imul %%rdi, %%rax\n");
			return;
		case DIV:
			S.printf("  cqo\n");
			S.printf("  idiv %%rdi\n");
			return;
		  case EQ:
		  case NE:
		  case LT:
		  case LE:
		    S.printf("  cmp %%rdi, %%rax\n");

		    if (node.kind == Node.Kind.EQ)
		      S.printf("  sete %%al\n");
		    else if (node.kind == Node.Kind.NE)
		      S.printf("  setne %%al\n");
		    else if (node.kind == Node.Kind.LT)
		      S.printf("  setl %%al\n");
		    else if (node.kind == Node.Kind.LE)
		      S.printf("  setle %%al\n");

		    S.printf("  movzb %%al, %%rax\n");
		    return;
		default:
			break;
		}

		S.error("invalid expression");
	}
	
	private static void gen_stmt(Node node) {
		if (node.kind == Node.Kind.EXPR_STMT) {
			gen_expr(node.lhs);
			return;
		}
		S.error("invalid statement");
	}
	
	private static int depth;

	public static void emit(Node node) {
		depth = 0; // reset code gen control
		
		S.printf("  .globl main\n");
		S.printf("main:\n");
		
		for (Node n = node; n != null; n = n.next) {
			gen_stmt(n);
			assert (depth == 0);
		}
		  
		S.printf("  ret\n");		
	}
}
