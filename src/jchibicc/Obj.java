package jchibicc;

class Obj {
	Obj next;
	String name; // Variable name
	Type ty;     // Type
	int offset;  // Offset from RBP
	
	public Obj(String name, Obj locals) {
		this.name = name;
		this.next = locals;		
	}
	
	public Obj(Type ty, Obj locals) {
		this.name = ty.name.str;
		this.ty = ty;		
		this.next = locals;
	}	

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return this.name.equals(obj);
		} else return super.equals(obj);
	}	
}
