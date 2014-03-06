package jdkBugsTest;

public class TrivialForward {

	static String doSomething(String s) {
		return s;
	}

	public static Object method1(String s) throws ClassNotFoundException {
		Object c = method2(s);
		return c;
	}

	static Object method2(String s) throws ClassNotFoundException {
		String st = doSomething(s);
		return method3(s);
	}

	static Object method3(String s) throws ClassNotFoundException {
		int a = 1;
		Object temp = method4(s);
		return temp;
	}

	static Object method4(String s) throws ClassNotFoundException {
		return Class.forName(s);
	}
}
