/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

public interface PublicInterface {
	
	@ProjectAccessibilityProperty
	public default void publicMethod() {
	}
}
