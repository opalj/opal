package ai.taint;

public class ValidPathCheck {

	private static Class<?> c(String className) throws ClassNotFoundException {
		return Class.forName(className);
	}

	private static Class<?> b(String paramClassName) throws ClassNotFoundException {
		String className = d(paramClassName);
		return c(className);
	}

	public static Class<?> a(String className) throws ClassNotFoundException {
		return b(className);
	}

	private static String d(String paramClassName) {
		return paramClassName;
	}

	public static String e(String className) {
		return d(className);
	}
}
