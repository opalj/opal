/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Thomas Schlosser
 */
@SuppressWarnings("all")
public class EnclosingMethodClass {

	public Object enclosingField = new Object() {
	};

	public static Object staticEnclosingField = new Object() {
	};

	public void enclosingMethod() {
		new Object() {
			public void innerMethod() {
			}
		}.innerMethod();
	}
}
