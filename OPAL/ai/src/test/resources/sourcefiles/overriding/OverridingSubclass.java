/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package overriding;

import org.opalj.fpa.test.annotations.OverriddenKeys;
import org.opalj.fpa.test.annotations.OverriddenProperty;

public class OverridingSubclass extends Superclass{

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
		privteMethod();
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	private void privteMethod(){
		publicFinalMethod();
	}
}