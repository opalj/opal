package ai.taint;

public class Recursion {

	public static Class<?> recursive(String className) throws ClassNotFoundException {
		Class<?> result = Class.forName(className);
		if (result == null) {
			Class<?> recResult = recursive(className);
			return recResult;
		} else
			return result;
	}

	public static Class<?> recursiveA(String className) throws ClassNotFoundException {
		if (className.length() > 10)
			return recursiveB(className);
		else
			return Class.forName(className);
	}

	private static Class<?> recursiveB(String className) throws ClassNotFoundException {
		if (className.length() < 10)
			return recursiveA(className);
		else
			return recursiveB(className);
	}
}
