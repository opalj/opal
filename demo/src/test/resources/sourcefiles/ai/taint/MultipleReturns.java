package ai.taint;

public class MultipleReturns {

	public static Class<?> test(String className) throws ClassNotFoundException {
		String name = foo(className);
		return Class.forName(name);
	}

	private static String foo(String className) {
		if (className.length() > 50)
			return className;
		else
			return "something" + className;
	}
}
