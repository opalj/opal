/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package overriding;

import org.opalj.fpa.test.annotations.OverriddenKeys;
import org.opalj.fpa.test.annotations.OverriddenProperty;

public class SubclassSamePackage extends ASuperclass {
	
	@OverriddenProperty(
			opa=OverriddenKeys.CantNotBeOverridden,
			cpa=OverriddenKeys.CantNotBeOverridden)
	private void privateMethod(){
		System.out.println("private");
	}
	
	@OverriddenProperty
	public void publicMethod(){
		privateMethod();
		protectedMethod();
		publicFinalMethod();
		System.out.println("public");
	}
	
	@OverriddenProperty
	void packagePrivateMethod(){
		System.out.println("package private");
	}
	
	
	@OverriddenProperty
	protected void protectedMethod(){
		System.out.println("protected");
	}
}
