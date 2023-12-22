/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage3;

/**
 * 
 * This class is for test purpose only. It shall reflect the case of an
 * incomplete type hierarchy.
 * 
 * IMPORTANT: This class is only used at compile time, it's class file has to be
 * deleted later on. The test will otherwise fail.
 * 
 * @note The method names refer to the closed packages assumption.
 * 
 * @author Michael Reif
 */
public class DELETETHISCLASSFILE {

	public DELETETHISCLASSFILE(){
		privateMethodWithoutSuperclassLeakege(); // suppress warning of unused private method.
	}
	
	public void publicMethodWithSuperclassLeakage() {

	}

	protected void protectedMethodWithSuperclassLeakage() {

	}

	void packagePrivateMethodWithoutSuperclassLeakage() {

	}

	private void privateMethodWithoutSuperclassLeakege() {

	}
}
