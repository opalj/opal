/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.util.FormatterClosedException;

import javax.naming.OperationNotSupportedException;

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
public class ExceptionTestClass {

	public void testMethod() throws IllegalStateException, OperationNotSupportedException {
		throw new FormatterClosedException();
	}

	public void catchMethod() {
		try {
			try {
				testMethod();
			} catch (IllegalStateException e) {
			}
		} catch (Exception e) {
		} finally {
			Integer.valueOf(42);
		}
	}
}
