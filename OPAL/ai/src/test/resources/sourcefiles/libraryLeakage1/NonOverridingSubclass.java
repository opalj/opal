/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage1;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

public class NonOverridingSubclass extends Superclass {

	{
		privateMethod();
	}
	
	@CallabilityProperty(
			opa=CallabilityKeys.NotCallable,
			cpa=CallabilityKeys.NotCallable)
	private void privateMethod(){
		privateMethod();
	}
}
