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
public class Assertions {

	private static void doIt() {
		System.out.println("Great!");
	}

	public static void main1(String[] args) {

		assert (args != null);

		doIt();
	}

	public static void main2(String[] args) {

		assert args.length > 0 : "Some parameters are required...";

		doIt();

	}

	public static void main3(String[] args) {

		int i = Integer.parseInt(args[0]);

		if (i < 1 || i > 2)
			throw new IllegalArgumentException("i (" + i + ") is not valid");

		assert (i < 3 && i - 4 < 1) : "that's strange...";

	}

	public static void main4(String[] args) {

		int i = Integer.parseInt(args[0]);

		if (i < 1 || i > 2)
			throw new IllegalArgumentException("i (" + i + ") is not valid");

		switch (i) {
		case 1:
			System.out.println("1");
		case 2:
			System.out.println("2");
		default:
			throw new AssertionError("should never be reached...");
		}

	}

}
