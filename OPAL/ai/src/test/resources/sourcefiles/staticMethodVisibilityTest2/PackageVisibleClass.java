/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package staticMethodVisibilityTest2;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * This class used for test purpose only. The annotations are only valid under
 * the closed packages assumption.
 * 
 * @author Michael Reif
 *
 */

class PackageVisibleClass {

	@ProjectAccessibilityProperty(cpa=ProjectAccessibilityKeys.PackageLocal)
	static public void publicMethod() {
	}

	@ProjectAccessibilityProperty
	static protected void protectedMethod() {
	}

	@ProjectAccessibilityProperty(cpa=ProjectAccessibilityKeys.PackageLocal)
	static void packageVisibleMethod() {
	}

	@ProjectAccessibilityProperty(
			opa=ProjectAccessibilityKeys.ClassLocal,
			cpa=ProjectAccessibilityKeys.ClassLocal)
	static private void privateMethod() {
	}
}
