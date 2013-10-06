package ai.taint;


public class ExceptionalPath {

	public static Class<?> test(String className) {
		return delegate(className);
	}

	private static Class<?> delegate(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}
}
