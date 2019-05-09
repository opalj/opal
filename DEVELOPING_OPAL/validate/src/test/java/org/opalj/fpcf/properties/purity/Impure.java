/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.purity;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated method is impure.
 *
 * @author Dominik Helm
 */
@PropertyValidator(key = "Purity", validator = ImpureMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface Impure {

    /**
     * A short reasoning of this property.
     */
    String value(); // default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default { L0PurityAnalysis.class,
            L1PurityAnalysis.class, L2PurityAnalysis.class };

    EP[] eps() default {};

    boolean negate() default false;
}
