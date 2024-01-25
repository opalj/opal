/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Specifies that the annotated class is conditionally immutable. I.e., the fields of the
 * class are not mutable, but the referenced objects/arrays are.
 * 
 * @author Michael Eichberg
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface ConditionallyImmutable {

    String value();

}
