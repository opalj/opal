/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java8methodresolution;

public class SubWithInheritedCompute extends SuperWithCompute implements ISuperAlt {

	@Override
	public int magic(int a) {
		return 42;
	}

	public static void main(String[] args) {
		new SubWithInheritedCompute().compute(1001, 123);
	}

}
