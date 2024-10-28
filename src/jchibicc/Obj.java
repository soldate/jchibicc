package jchibicc;

class Obj {
	Obj next;
	String name; 		 // Variable name
	Type ty;    		 // Type
	boolean is_local; 	 // local or global/function
	int offset;  		 // Offset from RBP  

	boolean is_function; // Global variable or function	
	
	Obj params; 		 // Function
	Node body;
	Obj locals;
	int stack_size;
	
	Obj(String name, Obj locals) {
		this.name = name;
		this.next = locals;		
	}
	
	Obj(Type ty, Obj locals) {
		this.name = ty.name.str;
		this.ty = ty;		
		this.next = locals;
	}	

	Obj(String name, Type ty, Obj locals) {
		this.name = name;
		this.ty = ty;
		this.next = locals;
	}

	@Override
	public String toString() {
		if (name != null) return name.toString();
		else return super.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return this.name.equals(obj);
		} else return super.equals(obj);
	}	
}
