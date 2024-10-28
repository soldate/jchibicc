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
