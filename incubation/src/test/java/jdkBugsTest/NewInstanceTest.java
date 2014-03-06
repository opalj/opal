package jdkBugsTest;

public class NewInstanceTest {

	static void doSomething() {

	}

	public static Object method1(String s) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Object c = method2(s);
		return c;
	}

	static Object method2(String s) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		doSomething();
		return method3(s);
	}

	static Object method3(String s) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Class c = Class.forName(s);
		Object d = method4(c);
		return d;
	}

	static Object method4(Class c) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		doSomething();
		doSomething();
		return method5(c);
	}

	static public Object MethodDex(String s) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Object temp = method3(s);
		return null;
	}

	static Object method5(Class c) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		return c.newInstance();
	}
}
