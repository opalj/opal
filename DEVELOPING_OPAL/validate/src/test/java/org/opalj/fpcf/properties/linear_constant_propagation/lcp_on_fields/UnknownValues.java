/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.UnknownValueMatcherLCP;

import java.lang.annotation.*;

/**
 * Container annotation for {@link UnknownValue} annotations.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LCPOnFieldsProperty.KEY, validator = UnknownValueMatcherLCP.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface UnknownValues {
    UnknownValue[] value();
}
