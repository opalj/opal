/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.thrown_exceptions;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state the thrown exception types.
 *
 * @author Andreas Muttscheller
 */
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface Types {

    Class<? extends Throwable>[] concrete() default  {};
    Class<? extends Throwable>[] upperBound() default  {Throwable.class};
}