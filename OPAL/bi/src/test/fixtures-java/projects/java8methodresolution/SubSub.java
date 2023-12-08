/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java8methodresolution;

import static java.lang.System.out;

public class SubSub implements ISuper,ISuperAlt {

	public static void main(String[] args) {
		new SubSub().compute(11,23);
	}

	@Override
	public int magic(int a) {
		return 101;
	}

	@Override
	public int compute(int a, int b) {
		out.println("SubSub.compute");
		return ISuperAlt.super.compute(a, b);
	}
	

}
