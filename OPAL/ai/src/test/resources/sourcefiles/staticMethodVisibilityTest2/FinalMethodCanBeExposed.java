/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package staticMethodVisibilityTest2;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * Inherits from the class defined below and make this methods visible to the client.
 * 
 * @author Michael Reif
 *
 */
public class FinalMethodCanBeExposed extends IDontWantToShowEveryoneWhatIHave {

}

class IDontWantToShowEveryoneWhatIHave {
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.Global)
	public final static void finalPublicMethod(){
	}
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.Global)
	protected final static void finalProtectedMethod(){
	}
}