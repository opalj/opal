/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.field_assignability;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.fieldassignability.L2FieldAssignabilityAnalysis;

/**
 * Annotation to state that the annotated field is lazily initialized.
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "FieldAssignability",validator = LazilyInitializedFieldMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface LazilyInitializedField {

    /**
     * A short reasoning of this property.
     */
    String value();

    Class<? extends FPCFAnalysis>[] analyses() default {L2FieldAssignabilityAnalysis.class};

}
