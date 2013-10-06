package ai.taint;

public class PrivateMethod {
	
	public Class<?> leakingMethod3(String name) throws ClassNotFoundException {
		return privateMethod(name);
	}
	
	private Class<?> privateMethod(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

}
