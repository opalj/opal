/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage2;

import org.opalj.fpcf.test.annotations.CallabilityProperty;

/**
 * 
 * @author Michael Reif
 */
public interface LeakFromInterface {

	@CallabilityProperty
	default void leakedMethod(){
		
	}
}
