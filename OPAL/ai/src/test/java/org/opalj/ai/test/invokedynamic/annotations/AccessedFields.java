/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.test.invokedynamic.annotations;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

/**
 * Wrapper annotation to allow several AccessedField annotations on the same method.
 * 
 * @author Arne Lottmann
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AccessedFields {

    AccessedField[] value();
}
