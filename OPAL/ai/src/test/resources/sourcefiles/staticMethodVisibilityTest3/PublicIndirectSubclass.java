/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package staticMethodVisibilityTest3;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

/**
 * 
 * This class used for test purpose only. The annotations are only valid under
 * the closed packages assumption.
 * 
 * @author Michael Reif
 *
 */
public class PublicIndirectSubclass extends IndirectSubclass {
	
	@ProjectAccessibilityProperty
	static public void publicMethod(){}
}
