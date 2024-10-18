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
	
	// Round up `n` to the nearest multiple of `align`. For instance,
	// align_to(5, 8) returns 8 and align_to(11, 8) returns 16.
	private static int align_to(int n, int align) {
	  return (n + align - 1) / align * align;
	}	

	// Compute the absolute address of a given node.
	// It's an error if a given node does not reside in memory.
	private static void gen_addr(Node node) {
	  if (node.kind == Node.Kind.VAR) {
	    printf("  lea %d(%%rbp), %%rax\n", node.var.offset);
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
		switch (node.kind) {
		case IF: {
			int c = count();
			gen_expr(node.cond);
			printf("  cmp $0, %%rax\n");
			printf("  je  .L.else.%d\n", c);
			gen_stmt(node.then);
			printf("  jmp .L.end.%d\n", c);
			printf(".L.else.%d:\n", c);
			if (node.els != null) gen_stmt(node.els);
			printf(".L.end.%d:\n", c);
			return;
		}
		case FOR: {
			int c = count();
			gen_stmt(node.init);
			printf(".L.begin.%d:\n", c);
			if (node.cond != null) {
				gen_expr(node.cond);
				printf("  cmp $0, %%rax\n");
				printf("  je  .L.end.%d\n", c);
			}
			gen_stmt(node.then);
			if (node.inc != null) gen_expr(node.inc);
			printf("  jmp .L.begin.%d\n", c);
			printf(".L.end.%d:\n", c);
			return;
		}
		case BLOCK:
			for (Node n = node.body; n != null; n = n.next)
				gen_stmt(n);
			return;
		case RETURN:
			gen_expr(node.lhs);
			printf("  jmp .L.return\n");
			return;
		case EXPR_STMT:
			gen_expr(node.lhs);
			return;
		default:
			break;
		}
		S.error("invalid statement");
	}
	
	private static int depth;

	private static int i = 1;
	private static int count() {
		return i++;
	}

	// Assign offsets to local variables.
	private static void assign_lvar_offsets(Function prog) {
	  int offset = 0;
	  for (Obj var = prog.locals; var != null; var = var.next) {
	    offset += 8;
	    var.offset = -offset;
	  }
	  prog.stack_size = align_to(offset, 16);
	}
	
	public static void emit(Function prog) {
		assign_lvar_offsets(prog);
		
		printf("  .globl main\n");
		printf("main:\n");
		
		// Prologue
		printf("  push %%rbp\n");
		printf("  mov %%rsp, %%rbp\n");
		printf("  sub $%d, %%rsp\n", prog.stack_size);
		  
		for (Node n = prog.body; n != null; n = n.next) {
			gen_stmt(n);
			assert (depth == 0);
		}
		
		printf(".L.return:\n");  
		printf("  mov %%rbp, %%rsp\n");
		printf("  pop %%rbp\n");		
		printf("  ret\n");		
	}
}
