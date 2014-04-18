package jdkBugsTest;

public class InvokeVirtualHelper2 {

	public Class getClass(String s) {
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
