/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package libraryLeakage1;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

/**
 * 
 * This class overrides transitive reachable methods of the superclass which
 * otherwise would be disclosed to the client user if met. Thus, most methods
 * must have the IsOverridden property
 * 
 * @author Michael Reif
 *
 */
public class TransitiveOverridingSubclass extends LayerSubclass {
	
	@CallabilityProperty
	public void publicMethod(){
		protectedMethod();
	}
	
	@CallabilityProperty
	protected void protectedMethod(){
		publicMethod();
	}
	
	@CallabilityProperty(
			cpa=CallabilityKeys.NotCallable)
	void packagePrivateMethod(){
		publicFinalMethod();
	}
}
