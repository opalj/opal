/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package entryPointTest;

import org.opalj.fpcf.test.annotations.EntryPointProperty;


/**
 * 
 * @author Michael Reif
 */
public interface InterfaceWithEntryPoint {
	
	@EntryPointProperty
	default void defaultMethodAsEntryPoint(){
	}
}
