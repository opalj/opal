package jdkBugsTest;

/**
 * This is a test for the JDKBugs Class.forName() analysis. It has a call to
 * Clas.forName() but doesn't return it back
 * 
 * @author Lars
 * 
 */
public class NotReturned {

	static void doSomething() {

	}

	public static Object method1(String s) throws ClassNotFoundException {
		Object c = method2(s);
		return c;
	}

	static Object method2(String s) throws ClassNotFoundException {
		doSomething();
		Object c = method3(s);
		TmpClass tmp = new TmpClass();
		return tmp;
	}

	static Object method3(String s) throws ClassNotFoundException {
		Object temp = method4(s);
		return temp;
	}

	static Object method4(String s) throws ClassNotFoundException {
		return Class.forName(s);
	}
}

class TmpClass {

	public TmpClass() {

	}
}
