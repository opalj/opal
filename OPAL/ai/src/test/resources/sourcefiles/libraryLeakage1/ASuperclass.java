/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage1;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

public abstract class ASuperclass {
	
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
			cpa=CallabilityKeys.Callable)
	protected void protectedMethod(){
		System.out.println("protected");
	}
	
	@CallabilityProperty(
			cpa=CallabilityKeys.NotCallable)
	void packagePrivateMethod(){
		System.out.println("package private");
	}
	
	@CallabilityProperty(
			opa=CallabilityKeys.Callable,
			cpa=CallabilityKeys.Callable)
	public final void publicFinalMethod(){
		System.out.println("public and final");
	}
	
	@CallabilityProperty(
			cpa=CallabilityKeys.Callable)
	public native void nativeLeak();
}
