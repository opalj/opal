/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage1;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

public class SubclassSamePackage extends ASuperclass {
	
	@CallabilityProperty(
			opa=CallabilityKeys.NotCallable,
			cpa=CallabilityKeys.NotCallable)
	private void privateMethod(){
		System.out.println("private");
	}
	
	@CallabilityProperty
	public void publicMethod(){
		privateMethod();
		protectedMethod();
		publicFinalMethod();
		System.out.println("public");
	}
	
	@CallabilityProperty(
			cpa=CallabilityKeys.NotCallable)
	void packagePrivateMethod(){
		System.out.println("package private");
	}
	
	
	@CallabilityProperty
	protected void protectedMethod(){
		System.out.println("protected");
	}
}
