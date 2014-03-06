package jdkBugsTest;

public class ForwardClass {

	static String test;

	static void doSomething() {

	}

	public static Object method1(String s) throws ClassNotFoundException {
		Object c = method2(s);
		return c;
	}

	static Object method2(String s) throws ClassNotFoundException {
		doSomething();
		test = s;
		return method3(test);
	}

	static Object method3(String s) throws ClassNotFoundException {
		Class c = Class.forName(s);
		Class d = method4(c);
		return d;
	}

	static Class method4(Class c) throws ClassNotFoundException {
		doSomething();
		doSomething();
		return method5(c);
	}

	static Class method5(Class c) throws ClassNotFoundException {
		return c;
	}
}
