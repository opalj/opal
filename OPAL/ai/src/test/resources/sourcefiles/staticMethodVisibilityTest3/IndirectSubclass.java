/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package staticMethodVisibilityTest3;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

import staticMethodVisibilityTest2.DirectPublicSubclass;

/**
 * 
 * This class used for test purpose only. The annotations are only valid under
 * the closed packages assumption.
 * 
 * @author Michael Reif
 *
 */

class IndirectSubclass extends DirectPublicSubclass {

	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	static public void publicMethod() {
	}

	@ProjectAccessibilityProperty
	static public void publicMethod_Visible() {
	}

	@ProjectAccessibilityProperty
	static protected void protectedMethod() {
	}
}
