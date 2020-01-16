/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.reference_immutability_lazy_initialization;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceMatcher;
import org.opalj.tac.fpcf.analyses.L0ReferenceImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L0ReferenceImmutabilityLazyInitializationAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated reference is mutable
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "ReferenceImmutabilityLazyInitialization",validator = NotThreadSafeInitializationMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface NotThreadSafeLazyInitializationAnnotation {


    /**
     * True if the field is non-final because it is read prematurely.
     * Tests may ignore @NonFinal annotations if the FieldPrematurelyRead property for the field
     * did not identify the premature read.
     */
    boolean prematurelyRead() default false;
    /**
     * A short reasoning of this property.
     */
    String value()  default "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default {L0ReferenceImmutabilityLazyInitializationAnalysis.class};

}
