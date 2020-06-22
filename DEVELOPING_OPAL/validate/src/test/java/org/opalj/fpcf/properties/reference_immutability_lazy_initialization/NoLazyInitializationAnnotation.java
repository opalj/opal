/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.reference_immutability_lazy_initialization;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.L0ReferenceImmutabilityLazyInitializationAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated reference is immutable
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "ReferenceImmutabilityLazyInitialization",validator = NoLazyInitializationMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface NoLazyInitializationAnnotation {

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default {L0ReferenceImmutabilityLazyInitializationAnalysis.class};

}
