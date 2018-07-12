/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package methods.a;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("all")
public class SubIndirectSub extends IndirectSub {

	void defaultVisibilityMethod(){
		// here we cannot call the super method!  
		System.out.println("SubIndirectSub.defaultVisibilityMethod()");
	}
	
	private void privateMethod() {
		// empty
	}
	
	public void publicMethod(){
		super.publicMethod();
	}
}
