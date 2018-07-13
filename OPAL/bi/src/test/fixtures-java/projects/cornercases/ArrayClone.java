/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package cornercases;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public class ArrayClone {

	public static void main(String[] args) {
		// calling clone on an array results in code where the receiver of
		// the method is actually the array and not a class type.
		String[] clone = args.clone();
		System.out.println(args.toString() + " " + clone.toString());
	}

}
