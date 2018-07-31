/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package entryPointTest;

import org.opalj.fpcf.test.annotations.EntryPointKeys;
import org.opalj.fpcf.test.annotations.EntryPointProperty;

/**
 * 
 * @author Michael Reif
 */
interface InterfaceWithoutEntryPoint {

	@EntryPointProperty(cpa=EntryPointKeys.NoEntryPoint)
	default void notAnEntryPoint() {
	}
}