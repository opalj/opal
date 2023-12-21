/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage3;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

/**
 * 
 * This class is for test purpose only. It shall reflect the case of a leakage
 * though the superclass.
 * 
 * @note The method names refer to the closed packages assumption.
 * 
 * @author Michael Reif
 */
public class CompleteTypeHierarchyLeakage extends SuperclassLeakage {

	public CompleteTypeHierarchyLeakage() {
		privateMethodWithoutSuperclassLeakege(); // suppress warning of unused
													// private method.
	}

	@CallabilityProperty
	public void publicMethodWithSuperclassLeakage() {

	}

	@CallabilityProperty
	protected void protectedMethodWithSuperclassLeakage() {

	}

	@CallabilityProperty(cpa = CallabilityKeys.NotCallable)
	void packagePrivateMethodWithoutSuperclassLeakage() {

	}

	@CallabilityProperty(opa = CallabilityKeys.NotCallable, cpa = CallabilityKeys.NotCallable)
	private void privateMethodWithoutSuperclassLeakege() {

	}
}
