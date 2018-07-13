/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package entryPointTest;

import org.opalj.fpcf.test.annotations.EntryPointKeys;
import org.opalj.fpcf.test.annotations.EntryPointProperty;

/**

 * @author Michael Reif
 */
class ClassWithDeadMethod implements InterfaceWithEntryPoint,
		InterfaceWithoutEntryPoint {

	@EntryPointProperty(
			cpa=EntryPointKeys.NoEntryPoint)
	public void n(){
	}
}