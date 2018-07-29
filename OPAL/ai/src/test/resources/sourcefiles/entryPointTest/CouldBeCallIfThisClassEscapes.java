/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package entryPointTest;

import org.opalj.fpcf.test.annotations.EntryPointKeys;
import org.opalj.fpcf.test.annotations.EntryPointProperty;

/**
 * 
 * This class also tests the case of an escape analysis. The declared methods
 * could be visible over a superclass if an instance escapes its scope casted to InterfaceWithEntryPoint.
 * 
 * 
 * @author Michael Reif
 *
 */
class CouldBeCallIfThisClassEscapes implements InterfaceWithEntryPoint {
	
	@@EntryPointProperty(
			cpa=EntryPointKeys.IsEntryPoint)
	public void defaultMethodAsEntryPoint() {
		doesNotEscape();
	}
	
	private void doesNotEscape(){
		new CouldBeCallIfThisClassEscapes();
	}
}
