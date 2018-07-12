/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package overriding;

import org.opalj.fpa.test.annotations.OverriddenProperty;

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
	
	@OverriddenProperty
	public void publicMethod(){
		protectedMethod();
	}
	
	@OverriddenProperty
	protected void protectedMethod(){
		publicMethod();
	}
	
	@OverriddenProperty
	void packagePrivateMethod(){
		publicFinalMethod();
	}
}
