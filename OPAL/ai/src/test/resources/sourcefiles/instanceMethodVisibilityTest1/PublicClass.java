/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
*
* This class is for test purposes only.
* It is public which implies that all public and protected methods
* are visible to the client. (regarding the closed packages assumption)
* 
* It implements the PackagePrivateInterface which defines a default method.
* The interface method is overwridden by publicMethod. Hence, it is not inherited
* and due to that not exposed to the client.
* 
* @author Michael Reif
*
*/
public class PublicClass implements PackagePrivateInterface{

	@ProjectAccessibilityProperty
	public void publicMethod(){
	}
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
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
