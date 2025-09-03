/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import java.lang.annotation.*;

/**
 * Annotation to state that a field has a constant value.
 *
 * @author Robin Körkemeier
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
public @interface ConstantField {
    /**
     * The name of the field
     */
    String field();

    /**
     * The constant value of the field
     */
    int value();
}
