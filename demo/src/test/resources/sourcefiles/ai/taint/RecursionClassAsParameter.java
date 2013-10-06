package ai.taint;

public class RecursionClassAsParameter {

	public Class<?> foo(String name) throws ClassNotFoundException {
		Class<?> c = Class.forName(name);
		recursive(c);
		return c;
	}

	private void recursive(Class<?> c) {
		if (c.getName().length() > 10)
			recursive(c);

	}
}
