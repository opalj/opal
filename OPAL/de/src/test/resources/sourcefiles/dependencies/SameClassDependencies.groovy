/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

/**
 * Test class for invokedynamic dependency extraction.
 * 
 * @author Arne Lottmann
 */
public class SameClassDependencies {

	public void noArgumentsMethod() {}
	
	public void dependencies() {
		noArgumentsMethod();
	}
	
}