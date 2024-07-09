/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.taint;

public class PrimTypes {
	
	public Class<?> publicMethod() throws ClassNotFoundException {
		@SuppressWarnings("unused")
		int x = 5;
		return Class.forName("soso");
	}

}
