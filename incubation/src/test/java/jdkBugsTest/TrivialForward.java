package jdkBugsTest;

/**
 * This is a test for the JDKBugs Class.forName() analysis. It has a call to
 * Clas.forName() and returns it back.
 * 
 * @author Lars
 * 
 */
public class TrivialForward {

	static String doSomething(String s) {
		return s;
	}

	public static Object method1(String s) {
		Object c = method2(s);
		return c;
	}

	static Object method2(String s) {
		String st = doSomething(s);
		return method3(st);
	}

	static Object method3(String s) {
		int a = 1;
		Object temp = method4(s);
		return temp;
	}

	static Object method4(String s) {
		try {
			return Class.forName(s);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
