/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package staticMethodVisibilityTest2;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * This class is for test purpose only. It only has one method with the visibility
 * modifier protected. However, this implies that the protected method can't be
 * exposed to the client (under CPA) because this class is final which prevents
 * any subclass.
 * 
 * @author Michael Reif
 *
 */
public final class FinalClassCantExposeProtectedMethods {

	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	protected static void cantBeGlobal(){
		
	}
}
