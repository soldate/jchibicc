package jchibicc;

// Code Generator
class Assembly {

	private static void printf(String s, Object... o) {
		S.printf(s, o);
	}
	
	private static void push() {
		printf("  push %%rax\n");
		depth++;
	}

	private static void pop(String s) {
		printf("  pop %s\n", s);
		depth--;
	}

	// Compute the absolute address of a given node.
	// It's an error if a given node does not reside in memory.
	private static void gen_addr(Node node) {
	  if (node.kind == Node.Kind.VAR) {
	    int offset = (node.name.charAt(0) - 'a' + 1) * 8;
	    printf("  lea %d(%%rbp), %%rax\n", -offset);
	    return;
	  }

	  S.error("not an lvalue");
	}
	
	private static void gen_expr(Node node) {
		switch (node.kind) {
		case NUM:
			printf("  mov $%d, %%rax\n", node.val);
			return;
		case NEG:
			gen_expr(node.lhs);
			printf("  neg %%rax\n");
			return;
		case VAR:
			gen_addr(node);
			printf("  mov (%%rax), %%rax\n");
			return;
		case ASSIGN:
			gen_addr(node.lhs);
			push();
			gen_expr(node.rhs);
			pop("%rdi");
			printf("  mov %%rax, (%%rdi)\n");
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
			printf("  add %%rdi, %%rax\n");
			return;
		case SUB:
			printf("  sub %%rdi, %%rax\n");
			return;
		case MUL:
			printf("  imul %%rdi, %%rax\n");
			return;
		case DIV:
			printf("  cqo\n");
			printf("  idiv %%rdi\n");
			return;
		  case EQ:
		  case NE:
		  case LT:
		  case LE:
		    printf("  cmp %%rdi, %%rax\n");

		    if (node.kind == Node.Kind.EQ)
		      printf("  sete %%al\n");
		    else if (node.kind == Node.Kind.NE)
		      printf("  setne %%al\n");
		    else if (node.kind == Node.Kind.LT)
		      printf("  setl %%al\n");
		    else if (node.kind == Node.Kind.LE)
		      printf("  setle %%al\n");

		    printf("  movzb %%al, %%rax\n");
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
		printf("  .globl main\n");
		printf("main:\n");
		
		// Prologue
		printf("  push %%rbp\n");
		printf("  mov %%rsp, %%rbp\n");
		printf("  sub $208, %%rsp\n");
		  
		for (Node n = node; n != null; n = n.next) {
			gen_stmt(n);
			assert (depth == 0);
		}
		  
		printf("  mov %%rbp, %%rsp\n");
		printf("  pop %%rbp\n");		
		printf("  ret\n");		
	}
}
