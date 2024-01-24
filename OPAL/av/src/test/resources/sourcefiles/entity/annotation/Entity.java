/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package entity.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Marco Torsello
 */
@Target({METHOD, FIELD}) 
@Retention(RUNTIME)
public @interface Entity {
	
}
