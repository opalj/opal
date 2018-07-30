/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * Since this class is abstract it is not possible to create an instance of it.
 * But via inheritance through a subclass is it possible to get access to the methods
 * when there are not overridden. This class is public, that implies that a client
 * can create a arbitrary number of subclasses which not override the according
 * methods.
 * 
 * @note This comments refers to the use of the closed packages assumption (cpa)
 *
 */
public abstract class PublicAbstractClass {
	
	@ProjectAccessibilityProperty
	public void publicMethod(){
	}
	
	@ProjectAccessibilityProperty(cpa=ProjectAccessibilityKeys.PackageLocal)
	void packagePrivateMethod(){
	}
	
	@ProjectAccessibilityProperty
	protected void protectedMethod(){
	}
	
	@ProjectAccessibilityProperty(
			opa=ProjectAccessibilityKeys.ClassLocal,
			cpa=ProjectAccessibilityKeys.ClassLocal)
	private void privateMethod(){
	}
}
