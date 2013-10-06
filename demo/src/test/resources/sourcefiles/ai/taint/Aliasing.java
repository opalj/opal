package ai.taint;

public class Aliasing {
	
	static class A {
		String f;
	}
	
	static class B {
		String f;
	}

	static class C extends A {
	}

	//here we assume that "name" may be tainted because we
	//only use a dumb pointer analysis
	//name2 should not be tainted, though, because B is of a different type
	public Class<?> nameInput(String name, String name2, String name3) throws ClassNotFoundException {
		A a1 = new A();
		A a2 = new A();
		B b = new B();
		C c = new C();
		
		c.f = name3;
		b.f = name2;
		a1.f = name;
		String arg = a2.f;
		return Class.forName(arg);
	}
	
	//name is tainted; name2 not
	public Class<?> nameInput2(String name, String name2) throws ClassNotFoundException {
		String[] a1 = new String[] {name};
		String[] a2 = new String[] {name2};
		@SuppressWarnings("unused")
		String s = a2[0];
		return Class.forName(a1[0]);
	}
}
