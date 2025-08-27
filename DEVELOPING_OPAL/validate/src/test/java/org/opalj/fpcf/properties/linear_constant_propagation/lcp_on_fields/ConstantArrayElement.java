/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import java.lang.annotation.*;

/**
 * Annotation to state that an array element has a constant value.
 *
 * @author Robin Körkemeier
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
public @interface ConstantArrayElement {
    /**
     * The index of the element
     */
    int index();

    /**
     * The constant value of the element
     */
    int value();
}
