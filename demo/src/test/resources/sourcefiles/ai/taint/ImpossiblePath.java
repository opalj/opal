package ai.taint;


public class ImpossiblePath {

	public Class<?> foo(String name, boolean random) throws ClassNotFoundException {
		Class<?> c = Class.forName(name);
		Class<?> a = null;
		Class<?> b = null;
		if (random) {
			a = c;
		} else {
			b = c;
		}
		b = a;
		return b;
	}
}
