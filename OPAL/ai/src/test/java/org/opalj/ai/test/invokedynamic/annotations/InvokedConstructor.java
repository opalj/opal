/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.test.invokedynamic.annotations;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

/**
 * Describes a constructor call made by an invokedynamic instruction or through use of the Java
 * reflection API.
 * 
 * @author Arne Lottmann
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface InvokedConstructor {

    /**
     * The type name of the receiver using JVM notation (e.g.,
     * "java/lang/Object").
     */
    String receiverType();

    Class<?>[] parameterTypes() default {};

    int line() default -1;
    
    boolean isReflective() default false;
}
