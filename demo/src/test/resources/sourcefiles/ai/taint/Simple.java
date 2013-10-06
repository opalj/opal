package ai.taint;

public class Simple {
	
	private Class<?> nameInput(String name) throws ClassNotFoundException {
		String n = name;
		Class<?> clazz = Class.forName(n);
		return clazz;
	}

	public Class<?> wrapper(String name2) throws ClassNotFoundException {
		return nameInput(name2);
	}
}
