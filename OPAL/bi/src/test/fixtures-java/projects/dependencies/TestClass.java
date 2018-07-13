/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.util.ArrayList;
import java.util.List;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Thomas Schlosser
 */
public class TestClass implements TestInterface {
    public void testMethod() {
	List<? extends CharSequence> list = new ArrayList<String>();
	list.add(null);
    }

    public String testMethod(Integer i, int j) {
	if (i != null && i.intValue() > j) {
	    return i.toString();
	}
	return String.valueOf(j);
    }
}
