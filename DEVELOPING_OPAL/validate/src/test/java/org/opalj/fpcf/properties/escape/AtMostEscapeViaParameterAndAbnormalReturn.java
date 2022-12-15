/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.escape;

import org.opalj.si.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

/**
 * Annotation to state that the allocation site or parameter escapes via a parameter and an
 * exception, but it  could escape any further (if a proper analysis was scheduled).
 *
 * @author Florian Kuebler
 */
@PropertyValidator(key = "EscapeProperty",
        validator = AtMostEscapeViaParameterAndAbnormalReturnMatcher.class)
@Target({ TYPE_USE, PARAMETER })
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface AtMostEscapeViaParameterAndAbnormalReturn {

    /**
     * A short reasoning of this property.
     */
    String value();

    Class<? extends FPCFAnalysis>[] analyses() default { SimpleEscapeAnalysis.class,
            InterProceduralEscapeAnalysis.class };

    boolean performInvokationsDomain() default true;
}
