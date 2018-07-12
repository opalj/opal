/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package libraryLeakage1;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;


/*
 * 
 * This class is cannot be overridden because it is final. Hence,
 * all contained methods must have the property NonOverridden.
 * 
 * @author Michael Reif
 */
public final class FinalSuperclass {
	
	@CallabilityProperty(
			opa=CallabilityKeys.NotCallable,
			cpa=CallabilityKeys.NotCallable)
	private void privateMethod(){
		System.out.println("private");
	}
	
	@CallabilityProperty(
			opa=CallabilityKeys.Callable,
			cpa=CallabilityKeys.Callable)
	public void publicMethod(){
		privateMethod();
		protectedMethod();
		publicFinalMethod();
		System.out.println("public");
	}
	
	@CallabilityProperty(
			opa=CallabilityKeys.Callable,
			cpa=CallabilityKeys.NotCallable)
	protected void protectedMethod(){
		System.out.println("protected");
	}
	
	@CallabilityProperty(
			opa=CallabilityKeys.Callable,
			cpa=CallabilityKeys.Callable)
	public final void publicFinalMethod(){
		System.out.println("public and final");
	}
}
