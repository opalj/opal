/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage1;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

public class Superclass {
	
	@CallabilityProperty(
			opa=CallabilityKeys.NotCallable,
			cpa=CallabilityKeys.NotCallable)
	private void privateMethod(){
		System.out.println("private");
	}
	
	@CallabilityProperty(
			cpa=CallabilityKeys.NotCallable)
	void packagePrivateMethod(){
		publicFinalMethod();
	}
	
	@CallabilityProperty
	public void publicMethod(){
		privateMethod();
		protectedMethod();
		publicFinalMethod();
		System.out.println("public");
	}
	
	@CallabilityProperty
	protected void protectedMethod(){
		System.out.println("protected");
	}
	
	@CallabilityProperty
	public final void publicFinalMethod(){
		System.out.println("public and final");
	}
}
