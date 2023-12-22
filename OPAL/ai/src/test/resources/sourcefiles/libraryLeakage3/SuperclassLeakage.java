/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage3;

/**
 * 
 * This class is for test purpose only. It shall reflect the case of a
 * Callable though the superclass.
 * 
 * @note The method names refer to the closed packages assumption.
 * 
 * @author Michael Reif
 */
public class SuperclassLeakage {

	public SuperclassLeakage(){
		privateMethodWithoutSuperclassLeakege(); // suppress warning of unused private method.
	}
	
	public void publicMethodWithSuperclassCallable() {

	}

	protected void protectedMethodWithSuperclassCallable() {

	}

	void packagePrivateMethodWithoutSuperclassCallable() {

	}

	private void privateMethodWithoutSuperclassLeakege() {

	}
}
