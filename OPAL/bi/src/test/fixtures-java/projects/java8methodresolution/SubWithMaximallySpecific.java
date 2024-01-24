/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java8methodresolution;

public class SubWithMaximallySpecific extends Super implements ISuper, ISuperAlt, ISub {

	public static void main(String[] args) {
		new SubWithMaximallySpecific().compute(11,23); // => ISub.compute!
	}
	

}
