/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.thrown_exceptions;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated method throws no exception.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
@PropertyValidator(key = "ExpectedExceptions",validator = DoesNotThrowExceptionMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface DoesNotThrowException {

    /**
     * A short reasoning of this property.
     */
    String reason();

    /** The (set of) analyses that strictly need to be executed before the test makes sense. */
    Class<? extends FPCFAnalysis>[] requires() default {};
}
