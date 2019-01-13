/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.test.invokedynamic.annotations;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

/**
 * Describes a field access made by an invokedynamic instruction or through use of the Java
 * reflection API.
 * 
 * @author Arne Lottmann
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AccessedField {

    Class<?> declaringType();

    String name();

    Class<?> fieldType();

    boolean isStatic() default false;

    int line() default -1;
}
