/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.field_mutability;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.immutability.fields.NonTransitiveImmutableFieldMatcher;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated field is effectively final (if a proper analysis
 * was scheduled).
 *
 * @author Michael Eichberg
 */
@PropertyValidator(key="FieldImmutability",validator= NonTransitiveImmutableFieldMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface _EffectivelyFinal{

    /**
     * A short reasoning of this property.
     */
    String value() ; // default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default {
        L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class
    };
}
