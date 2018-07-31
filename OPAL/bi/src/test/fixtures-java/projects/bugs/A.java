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
public class A extends SuperA implements java.io.Serializable {

	public A() {
		super(100);
		try {
			System.gc();
		} catch (IllegalMonitorStateException ims) {
			ims.printStackTrace();
		}
	}

	public void finalize() {
		System.out.println("Nothing to do...");
		Runtime.runFinalizersOnExit(true);
	}
}
