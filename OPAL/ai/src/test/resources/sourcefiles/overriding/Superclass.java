/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package overriding;

import org.opalj.fpa.test.annotations.OverriddenKeys;
import org.opalj.fpa.test.annotations.OverriddenProperty;

public class Superclass {
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	private void privateMethod(){
		System.out.println("private");
	}
	
	@OverriddenProperty
	void packagePrivateMethod(){
		publicFinalMethod();
	}
	
	@OverriddenProperty
	public void publicMethod(){
		privateMethod();
		protectedMethod();
		publicFinalMethod();
		System.out.println("public");
	}
	
	@OverriddenProperty
	protected void protectedMethod(){
		System.out.println("protected");
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	public final void publicFinalMethod(){
		System.out.println("public and final");
	}
}
