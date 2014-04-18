package jdkBugsTest;

public class ConcreteHelper2 implements InterfaceHelper {

	@Override
	public Object getClass(String s) {
		Object o = method1(s);
		return o;
	}

	public static Object method1(String s) {
		Class c = null;
		try {
			c = Class.forName(s);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return c;
	}
}
