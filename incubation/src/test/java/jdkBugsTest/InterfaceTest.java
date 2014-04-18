package jdkBugsTest;

public class InterfaceTest {

	public static Object method1(String s) throws ClassNotFoundException {
		InterfaceHelper interfaceHelper = new ConcreteHelper();
		Object o = interfaceHelper.getClass(s);
		return o;
	}
}
