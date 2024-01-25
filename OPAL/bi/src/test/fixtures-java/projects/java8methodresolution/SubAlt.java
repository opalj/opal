/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java8methodresolution;

import static java.lang.System.out;

public class SubAlt extends Super implements ISub {

	public int compute(int a, int b) {
		out.println("SubAlt.compute");
		return magic(ISuper.add(a, b));
	}

	public static void main(String[] args) {
		ISuperAlt t = new SubAlt();
		t.compute(11, 23);
	}

}
