/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * Thus this interface is package visible, it cannot be seen by a client
 * if the closed packages assumption is applied and there exists no class
 * that implements the interface which is the case in this test code.
 * 
 * This class has one subclass which overwrites the available method. Hence,
 * the method not becomes accessible to the client.
 * 
 * @author Michael Reif
 *
 */
interface PackagePrivateInterface {

	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	public default void publicMethod() {
		
	}
}
