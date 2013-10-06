package ai.taint;

public class Switch {

	public static Class<?> foo(String className) throws ClassNotFoundException {
		while (className.length() == 0) {
			switch (className.length()) {
			case 0:
				className += "0";
				break;
			case 1:
				className += "1";
				break;
			case 2:
			case 3:
			case 4:
				break;
			default:
				break;
			}
		}
		return Class.forName(className);
	}
}
