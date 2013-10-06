package ai.taint;

public class ReturnEdgeMerge {

	public Class<?> foo(String name, boolean random1, boolean random2) throws ClassNotFoundException {
		Class<?> result = Class.forName(name);

		if (random1) {
			result = foo(name, true, true);
		} else if (random2) {
			result = bar(result);
		}
		return result;
	}

	private Class<?> bar(Class<?> c) {
		return c;
	}
}
