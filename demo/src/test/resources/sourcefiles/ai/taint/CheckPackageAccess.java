package ai.taint;

import static sun.reflect.misc.ReflectUtil.checkPackageAccess;

public class CheckPackageAccess {
	
	public static Class<?> wrapperWithCheck(String n) throws ClassNotFoundException {
		String name = getNameWithCheck(n); 
		return loadIt(name);
	}

	public static Class<?> wrapperWithoutCheck(String n) throws ClassNotFoundException {
		String name = getNameWithoutCheck(n); 
		return loadIt(name);
	}

	@SuppressWarnings("restriction")
	public static String getNameWithCheck(String className) {
		checkPackageAccess(className);
		return className;
	}

	private static String getNameWithoutCheck(String className) {
		return className;
	}

	private static Class<?> loadIt(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

	public static Class<?> wrapperWithCheck() throws ClassNotFoundException {
		String name = getNameWithCheck(); 
		return loadIt(name);
	}

	public static Class<?> wrapperWithoutCheck() throws ClassNotFoundException {
		String name = getNameWithoutCheck(); 
		return loadIt(name);
	}

	@SuppressWarnings("restriction")
	private static String getNameWithCheck() {
		String className = "foo";  //this is constant, so don't bother
		checkPackageAccess(className);
		return className;
	}

	private static String getNameWithoutCheck() {
		String className = "foo";  //this is constant, so don't bother
		return className;
	}

	
	
}
