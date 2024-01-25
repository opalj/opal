/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Thomas Schlosser
 */
public class InstructionsTestClass {
	public Object field;
	public static InputStream staticField;

	public void method() {
		// NEW and INVOKESPECIAL (constructor call)
		Object obj = new Object();
		FilterInputStream stream = null;
		// ANEWARRAY
		obj = new Long[1];
		// MULTIANEWARRAY
		obj = new Integer[1][];

		// PUTFIELD
		field = obj;
		// GETFIELD
		obj = field;
		// INSTANCEOF
		if (obj instanceof ZipInputStream) {
			// CHECKCAST
			stream = (InflaterInputStream) obj;
			// PUTSTATIC
			staticField = stream;
			// GETSTATIC
			obj = staticField;
		}

		// INVOKESTATIC
		System.currentTimeMillis();

		TestInterface ti = new TestClass();
		// INVOKEINTERFACE
		ti.testMethod();

		// INVOKEVIRTUAL
		obj.equals(stream);
	}
}
