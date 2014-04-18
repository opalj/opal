package jdkBugsTest;

public class ConcreteHelper implements InterfaceHelper {

	@Override
	public Object getClass(String s) {
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
