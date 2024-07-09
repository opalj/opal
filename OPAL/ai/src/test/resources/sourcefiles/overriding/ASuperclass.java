/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package overriding;

import org.opalj.fpa.test.annotations.OverriddenKeys;
import org.opalj.fpa.test.annotations.OverriddenProperty;

public abstract class ASuperclass {
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	private void privateMethod(){
		System.out.println("private");
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.IsOverridden,
			cpa=OverriddenKeys.IsOverridden)
	public void publicMethod(){
		privateMethod();
		protectedMethod();
		publicFinalMethod();
		System.out.println("public");
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.IsOverridden,
			cpa=OverriddenKeys.IsOverridden)
	protected void protectedMethod(){
		System.out.println("protected");
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.IsOverridden,
			cpa=OverriddenKeys.IsOverridden)
	void packagePrivateMethod(){
		System.out.println("package private");
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	public final void publicFinalMethod(){
		System.out.println("public and final");
	}
}
