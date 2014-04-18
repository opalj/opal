package jdkBugsTest;

class InvokeVirtualTest extends InvokeVirtualHelper {

	public Object method1(String s) {
		return method2(s);
	}

	private Object method2(String s) {
		Class c = null;
		try {
			c = Class.forName(s);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return method3(c);
	}

	public Object method3(Class c) {
		try {
			return c.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}