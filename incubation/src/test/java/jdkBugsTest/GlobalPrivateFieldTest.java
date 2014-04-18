package jdkBugsTest;

public class GlobalPrivateFieldTest {

	private String taintField;

	public void setterMethod(String s) {
		taintField = s;
	}

	public void loopTest(String s) {
		taintField = s;
	}

	public Object getterMethod() {
		try {
			return Class.forName(taintField);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
