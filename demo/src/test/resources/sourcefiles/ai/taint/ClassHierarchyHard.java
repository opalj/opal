package ai.taint;

public class ClassHierarchyHard {

	public static Class<?> invokeable(String inv_className, A a) throws ClassNotFoundException {
		String inv_name = a.test(inv_className);
		return Class.forName(inv_name);
	}

	public static Class<?> redundantInvokeable(String red_className, A a) throws ClassNotFoundException {
		String red_name = a.test(red_className);
		return Class.forName(red_name);
	}

	private static class A {
		String test(String a_param) throws ClassNotFoundException {
			String a_test_name = "Constant";
			return a_test_name;
		}
	}

	private static class B extends A {

		@Override
		String test(String b_test_name) throws ClassNotFoundException {
			return b_test_name;
		}
	}
}
