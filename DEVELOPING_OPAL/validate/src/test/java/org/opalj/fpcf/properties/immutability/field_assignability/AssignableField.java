/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.field_assignability;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.fieldassignability.L2FieldAssignabilityAnalysis;

/**
 * Annotation to state that the annotated field is assignable.
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "FieldAssignability",validator = AssignableFieldMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface AssignableField {

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

    Class<? extends FPCFAnalysis>[] analyses() default {L2FieldAssignabilityAnalysis.class};

}
