/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ref_fields;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public abstract class A {

	private final Object o = "String"; // the refined type is String

	private static Object x = new ArrayList<Object>(); // the refined type is List!

	private List<Object> l = new ArrayList<>();

	public void updateX(List<?> x) {
		A.x = x;
	}

	public Object getX () { // can be refined once the information about the field is available
		return (List<?>) x;
	}

	@Override
	public String toString() {
		if(x == null)
			x = new Vector<>();

		return "The String: "+ o + x + l;
	}
}
