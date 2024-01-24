/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.lang.annotation.ElementType;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Thomas Schlosser
 */
@TestAnnotation
public class AnnotationDefaultAttributeTestClass {

	@TestAnnotation(stringValue = "noDefault",
			classValue = Integer.class,
			enumValue = ElementType.METHOD,
			annotationValue = @SuppressWarnings("noDefault"),
			arrayClassValue = {Long.class, Boolean.class })
	public void testMethod() {
	    // deliberately left empty
	}
}
