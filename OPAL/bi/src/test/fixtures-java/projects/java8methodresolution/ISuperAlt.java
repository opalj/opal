/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java8methodresolution;

import static java.lang.System.out;

public interface ISuperAlt {

	int magic(int a);
	
	default int compute(int a , int b){
		out.println("ISuperAlt.compute");
		return magic(ISuper.add(a,b));
	}
	
	static int multiply(int a , int b) {
		return a * b;
	}
}
