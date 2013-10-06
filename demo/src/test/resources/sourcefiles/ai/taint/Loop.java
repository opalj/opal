package ai.taint;

public class Loop {

	public static Class<?> foo(String className) throws ClassNotFoundException {
		Class<?> forName = Class.forName(className);
		Class[] result = new Class[10];
		for (int i = 0; i < 10; i++) {
			result[i] = forName;
		}
		return result[0];
	}
}
