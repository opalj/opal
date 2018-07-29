/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage1;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

public class OverridingSubclass extends Superclass{

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
		privteMethod();
	}
	
	@CallabilityProperty(
			opa=CallabilityKeys.NotCallable,
			cpa=CallabilityKeys.NotCallable)
	private void privteMethod(){
		publicFinalMethod();
	}
}