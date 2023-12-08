/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 *
 * This class is for test purposes only.
 * It is package private which implies that the methods are not considered
 * visible to the client if there is no subclass which inherits certain methods.
 * In this test code, there is no subclass of this class.
 * 
 * @author Michael Reif
 *
 */
class PPClass {

	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	public void publicMethod(){
	}
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	void packagePrivateMethod(){
	}
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	protected void protectedMethod(){
	}
	
	@ProjectAccessibilityProperty(
			opa=ProjectAccessibilityKeys.ClassLocal,
			cpa=ProjectAccessibilityKeys.ClassLocal)
	private void privateMethod(){
	}
}
