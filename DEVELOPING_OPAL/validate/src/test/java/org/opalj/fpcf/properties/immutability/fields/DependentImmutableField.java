/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.fields;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated field is dependent immutable.
 */
@PropertyValidator(key="FieldImmutability",validator= DependentImmutableFieldMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface DependentImmutableField {

    /**
     * A short reasoning of this property.
     */
    String value();

    Class<? extends FPCFAnalysis>[] analyses() default {L3FieldImmutabilityAnalysis.class};
}
