package jchibicc;

class Obj {
	Obj next;
	String name;
	int offset;
	
	public Obj(String name, Obj locals) {
		this.name = name;
		this.next = locals;		
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return this.name.equals(obj);
		} else return super.equals(obj);
	}	
}
