/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java8methodresolution;

import static java.lang.System.out;

public class SuperWithCompute {

	
	public int compute(int a, int b) {
		out.println("SuperWithCompute.compute");
		return a ^ b;
	}

}
