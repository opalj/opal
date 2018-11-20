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
public class SuperA {

	private int unused = -1;

	public SuperA(int i) {
		System.out.println(i);
	}

	public void finalize() {
		System.out.println("Nothing to do...");
		System.runFinalizersOnExit(true);
	}
}
