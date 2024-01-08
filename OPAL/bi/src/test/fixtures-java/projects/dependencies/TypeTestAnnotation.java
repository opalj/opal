/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
@Repeatable(TypeTestAnnotations.class)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD,
        ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PACKAGE,
        ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_PARAMETER,
        ElementType.TYPE_USE })
public @interface TypeTestAnnotation {

}
