/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.purity;

import org.opalj.si.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated method is domain specific contextually side effect free.
 *
 * @author Dominik Helm
 */
@PropertyValidator(key = "Purity",
        validator = DomainSpecificContextuallySideEffectFreeMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface DomainSpecificContextuallySideEffectFree {

    /**
     * A short reasoning of this property.
     */
    String value(); // default = "N/A";

    int[] modifies();

    Class<? extends FPCFAnalysis>[] analyses() default { L2PurityAnalysis.class };

    EP[] eps() default {};

    boolean negate() default false;
}
