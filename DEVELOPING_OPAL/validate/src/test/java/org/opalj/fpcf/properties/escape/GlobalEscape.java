/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.escape;

import org.opalj.si.FPCFAnalysis;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

/**
 * Annotation to state that the allocation site or parameter escapes globally (if a proper analysis
 * was scheduled).
 *
 * @author Florian Kuebler
 */
@PropertyValidator(key = "EscapeProperty", validator = GlobalEscapeMatcher.class)
@Target({ TYPE_USE, PARAMETER })
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface GlobalEscape {

    /**
     * A short reasoning of this property.
     */
    String value();

    Class<? extends FPCFAnalysis>[] analyses() default { SimpleEscapeAnalysis.class,
            InterProceduralEscapeAnalysis.class };

    boolean requiresPerformInvokationsDomain() default false;
}
