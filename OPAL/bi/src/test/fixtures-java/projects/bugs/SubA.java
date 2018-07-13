/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package bugs;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("all")
final public class SubA extends A {

	protected int ANSWER = 42;

	public SubA() {
		System.out.println("subA");
	}

	public void finalize() {
		Runtime.getRuntime().gc();
	}
}
