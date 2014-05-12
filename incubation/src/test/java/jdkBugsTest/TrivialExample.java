package jdkBugsTest;

public class TrivialExample {

	public static Class callerMethod(String s) {
		return methodA(s);
	}

	static Class methodA(String s) {
		return methodB(s);
	}

	static Class methodB(String s) {
		try {
			return Class.forName(s);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}	
}
