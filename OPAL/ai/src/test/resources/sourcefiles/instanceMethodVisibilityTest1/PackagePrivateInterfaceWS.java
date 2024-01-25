/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * This class is for test purposes only.
 * WS indicates that this class is subclassed.
 * 
 * subclass: InheritFromPPInterface
 * 
 * @author Michael Reif
 *
 */
interface PackagePrivateInterfaceWS {

	@ProjectAccessibilityProperty
	public default void publicMethod() {
	}
}
