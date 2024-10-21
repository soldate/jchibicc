package jchibicc;

class Function {
    Function next;
    String name;	
    Obj params;
	Node body;
	Obj locals;
	int stack_size;
	
	@Override
	public String toString() {
		if (name != null) return name;
		else return super.toString();
	}	
}
