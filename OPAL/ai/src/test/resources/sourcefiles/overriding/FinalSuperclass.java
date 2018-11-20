/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package overriding;

import org.opalj.fpa.test.annotations.OverriddenKeys;
import org.opalj.fpa.test.annotations.OverriddenProperty;


/*
 * 
 * This class is cannot be overridden because it is final. Hence,
 * all contained methods must have the property NonOverridden.
 * 
 * @author Michael Reif
 */
public final class FinalSuperclass {
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	private void privateMethod(){
		System.out.println("private");
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	public void publicMethod(){
		privateMethod();
		protectedMethod();
		publicFinalMethod();
		System.out.println("public");
	}
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
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
