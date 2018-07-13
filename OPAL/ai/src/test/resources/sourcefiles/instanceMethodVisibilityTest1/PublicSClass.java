/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

public class PublicSClass extends PPAbstractClassWS {
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.Global)
	public void publicMethod(){
	}
	
	@ProjectAccessibilityProperty(cpa=ProjectAccessibilityKeys.PackageLocal)
	void packagePrivateMethod(){
	}
	
	
	@ProjectAccessibilityProperty(
			opa=ProjectAccessibilityKeys.ClassLocal,
			cpa=ProjectAccessibilityKeys.ClassLocal)
	private void privateMethod(){
	}
}
