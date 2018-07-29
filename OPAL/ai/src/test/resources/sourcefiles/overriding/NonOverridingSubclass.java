/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package overriding;

import org.opalj.fpa.test.annotations.OverriddenKeys;
import org.opalj.fpa.test.annotations.OverriddenProperty;

public class NonOverridingSubclass extends Superclass {

	{
		privateMethod();
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	private void privateMethod(){
		privateMethod();
	}
}
