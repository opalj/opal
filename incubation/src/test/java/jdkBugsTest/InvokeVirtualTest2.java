package jdkBugsTest;

public class InvokeVirtualTest2 {

	public Class method1(String s) {
		return method2(s);
	}

	public Class method2(String s) {
		InvokeVirtualHelper2 ivh2 = new InvokeVirtualHelper2();
		return ivh2.getClass(s);
	}

}
