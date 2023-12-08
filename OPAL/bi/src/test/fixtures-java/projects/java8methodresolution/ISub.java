/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java8methodresolution;

import static java.lang.System.out;

public interface ISub extends ISuper, ISuperAlt{

	
	// we have to override!
	@Override
	default int compute(int a, int b) {
		out.println("ISub.compute");
		return ISuperAlt.super.compute(a,ISuper.super.compute(a, b));
	}
	
}
