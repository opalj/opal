/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.reference_immutability_lazy_initialization;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedReferenceMatcher;
import org.opalj.tac.fpcf.analyses.L0ReferenceImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L0ReferenceImmutabilityLazyInitializationAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated field is lazy initialized
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "ReferenceImmutabilityLazyInitialization",validator = LazyInitializedReferenceMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface LazyInitializationAnnotation {

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default {L0ReferenceImmutabilityLazyInitializationAnalysis.class};

}
