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
		case ND_NUM:
			S.printf("  mov $%d, %%rax\n", node.val);
			return;
		case ND_NEG:
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
		case ND_ADD:
			S.printf("  add %%rdi, %%rax\n");
			return;
		case ND_SUB:
			S.printf("  sub %%rdi, %%rax\n");
			return;
		case ND_MUL:
			S.printf("  imul %%rdi, %%rax\n");
			return;
		case ND_DIV:
			S.printf("  cqo\n");
			S.printf("  idiv %%rdi\n");
			return;
		  case ND_EQ:
		  case ND_NE:
		  case ND_LT:
		  case ND_LE:
		    S.printf("  cmp %%rdi, %%rax\n");

		    if (node.kind == Node.Kind.ND_EQ)
		      S.printf("  sete %%al\n");
		    else if (node.kind == Node.Kind.ND_NE)
		      S.printf("  setne %%al\n");
		    else if (node.kind == Node.Kind.ND_LT)
		      S.printf("  setl %%al\n");
		    else if (node.kind == Node.Kind.ND_LE)
		      S.printf("  setle %%al\n");

		    S.printf("  movzb %%al, %%rax\n");
		    return;
		default:
			break;
		}

		S.error("invalid expression");
	}
	
	private static int depth;

	public static void emit(Node node) {
		depth = 0; // reset code gen control
		
		S.printf("  .globl main\n");
		S.printf("main:\n");
		gen_expr(node);
		S.printf("  ret\n");
		assert (depth == 0);		
	}
}
