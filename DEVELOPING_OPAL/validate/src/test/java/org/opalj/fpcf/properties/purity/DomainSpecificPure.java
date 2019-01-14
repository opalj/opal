/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.purity;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated method is domain specific pure.
 *
 * @author Dominik Helm
 */
@PropertyValidator(key = "Purity", validator = DomainSpecificPureMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface DomainSpecificPure {

    /**
     * A short reasoning of this property.
     */
    String value(); // default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default { L1PurityAnalysis.class,
            L2PurityAnalysis.class };

    EP[] eps() default {};

    boolean negate() default false;
}
