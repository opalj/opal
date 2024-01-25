/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java8methodresolution;

import static java.lang.System.out;

public interface ISuper {

	int magic(int a);
	
	default int compute(int a , int b){
		out.println("ISuper.compute");
		return magic(add(a,b));
	}
	
	static int add(int a , int b) {
		return a + b;
	}
}
