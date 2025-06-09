/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.UnknownValueMatcherLCP;

import java.lang.annotation.*;

/**
 * Annotation to state that a variables value is unknown.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LCPOnFieldsProperty.KEY, validator = UnknownValueMatcherLCP.class)
@Repeatable(UnknownValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface UnknownValue {
    /**
     * The PC of this variable in the bytecode
     */
    int pc();
}
