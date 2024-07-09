/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package annotations.target;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

/**
 * Wrapper annotation that allows several InvokedMethod annotations on the same method.
 * 
 * @author Arne Lottmann
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface InvokedMethods {

    InvokedMethod[] value();
}
