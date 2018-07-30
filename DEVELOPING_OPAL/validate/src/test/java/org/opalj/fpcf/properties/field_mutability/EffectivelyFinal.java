/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.field_mutability;

import org.opalj.fpcf.FPCFAnalysis;
import org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis;
import org.opalj.fpcf.analyses.L0FieldMutabilityAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that the annotated field is effectively final (if a proper analysis
 * was scheduled).
 *
 * @author Michael Eichberg
 */
@PropertyValidator(key="FieldMutability",validator=EffectivelyFinalMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface EffectivelyFinal{

    /**
     * A short reasoning of this property.
     */
    String value() ; // default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default {L0FieldMutabilityAnalysis.class, L1FieldMutabilityAnalysis.class};
}
