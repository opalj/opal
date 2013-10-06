package ai.taint;

import java.util.Random;

public class RecursivePopState {

	static boolean random = new Random().nextBoolean();

	public static Class<?> foo(String className) throws ClassNotFoundException {
		Class<?> result = Class.forName(className);
		return bar(result);
	}

	private static Class<?> bar(Class<?> result) {
		if (random)
			return bar(result);
		else
			return result;
	}
}
