/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.UnknownValueMatcher;

import java.lang.annotation.*;

/**
 * Container annotation for {@link UnknownValue} annotations.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LinearConstantPropagationProperty.KEY, validator = UnknownValueMatcher.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface UnknownValues {
    UnknownValue[] value();
}
