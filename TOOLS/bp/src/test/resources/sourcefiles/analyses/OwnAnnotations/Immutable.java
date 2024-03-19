/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package OwnAnnotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Our own Immutable annotation for test purposes.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Immutable {

}
