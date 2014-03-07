package jdkBugsTest;

/**
 * This is a test for the JDKBugs Class.forName() analysis. It has a call to
 * Clas.forName() via a private method and returns it back
 * 
 * @author Lars
 * 
 */
public class InvokeSpecialTest {

	static void doSomething() {

	}

	public static Object method1(String s) throws ClassNotFoundException {
		Object c = method2(s);
		return c;
	}

	static Object method2(String s) throws ClassNotFoundException {
		doSomething();
		return method3(s);
	}

	static Object method3(String s) throws ClassNotFoundException {
		Class c = Class.forName(s);
		Class d = new InvokeSpecialTest().method4(c);
		return d;
	}

	private Class method4(Class c) throws ClassNotFoundException {
		doSomething();
		doSomething();
		return method5(c);
	}

	static Class method5(Class c) throws ClassNotFoundException {
		return c;
	}

}
