/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.field_mutability;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that the annotated field is not final.
 *
 * @author Michael Eichberg
 */
@PropertyValidator(key = "FieldMutability",validator = NonFinalMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface NonFinal{

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";

    /**
     * True if the field is non-final because it is read prematurely.
     * Tests may ignore @NonFinal annotations if the FieldPrematurelyRead property for the field
     * did not identify the premature read.
     */
    boolean prematurelyRead() default false;
}
