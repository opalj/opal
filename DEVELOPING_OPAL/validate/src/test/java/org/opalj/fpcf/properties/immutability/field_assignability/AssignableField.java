/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.field_assignability;

import java.lang.annotation.*;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.fieldassignability.L0FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.fieldassignability.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.fieldassignability.L2FieldAssignabilityAnalysis;

/**
 * Annotation to state that the annotated field is assignable.
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "FieldAssignability", validator = AssignableFieldMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD })
public @interface AssignableField {
    /**
     * A short reasoning of this property.
     */
    String value()  default "N/A";

    /**
     * Which analyses should recognize this annotation instance.
     */
    Class<? extends FPCFAnalysis>[] analyses() default {
            L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class,
    };
}
