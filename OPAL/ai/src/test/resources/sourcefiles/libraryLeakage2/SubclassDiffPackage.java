/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage2;


import libraryLeakage1.ASuperclass;

/**
 * 
 * This class inherits from ASuperclass and serves as test that package private methods
 * can not be inherited in subclasses of other packages. Hence, these methods have to be
 * considered as overridden even if there is a subclass.
 * 
 * @author Michael Reif
 *
 */
public class SubclassDiffPackage extends ASuperclass {
	
	public void publicMethod(){
		protectedMethod();
		publicFinalMethod();
		System.out.println("public");
	}
	
	protected void protectedMethod(){
		System.out.println("protected");
	}
}
