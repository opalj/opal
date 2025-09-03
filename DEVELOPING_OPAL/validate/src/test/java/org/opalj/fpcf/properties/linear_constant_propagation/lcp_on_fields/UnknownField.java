/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to state that a field value is unknown.
 *
 * @author Robin Körkemeier
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
public @interface UnknownField {
    /**
     * The name of the field
     */
    String field();
}
