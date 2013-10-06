package ai.taint;

public class ClassHierarchySimple {

	public static Class<?> foo(String className) throws ClassNotFoundException {
		A a = new B();
		return a.test(className);
	}

	private static class A {
		Class<?> test(String className) throws ClassNotFoundException {
			return Class.forName(className);
		}
	}

	private static class B extends A {

		@Override
		Class<?> test(String className) throws ClassNotFoundException {
			return Class.forName(className);
		}
	}
}
