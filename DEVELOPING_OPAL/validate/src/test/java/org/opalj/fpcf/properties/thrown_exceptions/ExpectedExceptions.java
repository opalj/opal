/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.thrown_exceptions;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state the (transitivley) thrown exceptions.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
@PropertyValidator(key = "ExpectedExceptions",validator = ExpectedExceptionsMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ExpectedExceptions {

    /**
     * A short reasoning of this property.
     */
    String reason() default "";

    Types value() default @Types;

    /** The (set of) analyses that strictly need to be executed before the test makes sense. */
    Class<? extends FPCFAnalysis>[] requires() default {};
}
