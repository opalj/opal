/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package type_annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class was used to create a class file with some well defined signatures. The
 * created class is subsequently used by several tests.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
@Target({ ElementType.TYPE_USE /*Subsumes TYPE_PARAMETER and TYPE */ })
@Retention(RetentionPolicy.CLASS)
public @interface RITypeAnnotations { // RI = Runtime Invisible
     RITypeAnnotation[] value();
}
