/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that instances of the annotated class are mutable.
 * 
 * @author Andre Pacak
 * @author Michael Eichberg
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface Mutable {

	/** A short explanation. */
    String value();
}
