/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * Since this class is abstract it is not possible to create an instance of it.
 * But via inheritance through a subclass is it possible to get access to the methods
 * when there are not overridden. This class is package private, that implies that a client
 * that is not able to contribute to this given package can not access the already implemented
 * methods, no matter what visibility modifier they have. This class has no public subclass,
 * hence, all methods should be maximal package local under the closed packages assumption.
 * 
 * @note This comments refers to the use of the closed packages assumption (cpa)
 *
 */
abstract class PPAbstractClass {
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	public void publicMethod(){
	}
	
	@ProjectAccessibilityProperty(cpa=ProjectAccessibilityKeys.PackageLocal)
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
