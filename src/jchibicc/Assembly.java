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
		switch (node.kind) {
		case VAR:
			if (node.var.is_local) {
				// Local variable
				printf("  lea %d(%%rbp), %%rax\n", node.var.offset);
			} else {
				// Global variable
				printf("  lea %s(%%rip), %%rax\n", node.var.name);
			}
			return;
		case DEREF:
			gen_expr(node.lhs);
			return;
		default:
			break;
		}

		S.error("%s not an lvalue", node.token.toString());
	}
	
	// Load a value from where %rax is pointing to.
	private static void load(Type ty) {
	  if (ty.kind == Type.Kind.ARRAY) {
	    // If it is an array, do not attempt to load a value to the
	    // register because in general we can't load an entire array to a
	    // register. As a result, the result of an evaluation of an array
	    // becomes not the array itself but the address of the array.
	    // This is where "array is automatically converted to a pointer to
	    // the first element of the array in C" occurs.
	    return;
	  }

	  printf("  mov (%%rax), %%rax\n");
	}

	// Store %rax to an address that the stack top is pointing to.
	private static void store() {
	  pop("%rdi");
	  printf("  mov %%rax, (%%rdi)\n");
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
			load(node.ty);
			return;
		case DEREF:
			gen_expr(node.lhs);
		    load(node.ty);
			return;
		case ADDR:
			gen_addr(node.lhs);
			return;
		case ASSIGN:
			gen_addr(node.lhs);
			push();
			gen_expr(node.rhs);
			store();
			return;
		case FUNCALL:
		    int nargs = 0;
		    for (Node arg = node.args; arg != null; arg = arg.next) {
		      gen_expr(arg);
		      push();
		      nargs++;
		    }

		    for (int i = nargs - 1; i >= 0; i--)
		      pop(argreg[i]);

			printf("  mov $0, %%rax\n");
			printf("  call %s\n", node.funcname);
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

		S.error("%s invalid expression", node.token.toString());
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
			if (node.init != null) gen_stmt(node.init);
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
			printf("  jmp .L.return.%s\n", current_fn.name);
			return;
		case EXPR_STMT:
			gen_expr(node.lhs);
			return;
		default:
			break;
		}
		S.error("%s invalid statement", node.token.toString());
	}
	
	private static int depth;
	private static String argreg[] = {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"};
	private static Obj current_fn;
	
	private static int i = 1;
	private static int count() {
		return i++;
	}

	// Assign offsets to local variables.
	private static void assign_lvar_offsets(Obj prog) {
		for (Obj fn = prog; fn != null; fn = fn.next) {
			if (!fn.is_function)
			      continue;
			
			int offset = 0;
			for (Obj var = fn.locals; var != null; var = var.next) {
				offset += var.ty.size;
				var.offset = -offset;
			}
			fn.stack_size = align_to(offset, 16);
		}
	}
	
	private static void emit_data(Obj prog) {
		for (Obj var = prog; var != null; var = var.next) {
			if (var.is_function) continue;

			printf("  .data\n");
			printf("  .globl %s\n", var.name);
			printf("%s:\n", var.name);
			printf("  .zero %d\n", var.ty.size);
		}
	}

	private static void emit_text(Obj prog) {
		for (Obj fn = prog; fn != null; fn = fn.next) {
			if (!fn.is_function) continue;

			printf("  .globl %s\n", fn.name);
			printf("  .text\n");
			printf("%s:\n", fn.name);
			current_fn = fn;

			// Prologue
			printf("  push %%rbp\n");
			printf("  mov %%rsp, %%rbp\n");
			printf("  sub $%d, %%rsp\n", fn.stack_size);

			// Save passed-by-register arguments to the stack
			int i = 0;
			for (Obj var = fn.params; var != null; var = var.next)
				printf("  mov %s, %d(%%rbp)\n", argreg[i++], var.offset);

			// Emit code
			gen_stmt(fn.body);
			assert (depth == 0);

			// Epilogue
			printf(".L.return.%s:\n", fn.name);
			printf("  mov %%rbp, %%rsp\n");
			printf("  pop %%rbp\n");
			printf("  ret\n");
		}
	}

	public static void codegen(Obj prog) {
		assign_lvar_offsets(prog);
		emit_data(prog);
		emit_text(prog);
	}
}
