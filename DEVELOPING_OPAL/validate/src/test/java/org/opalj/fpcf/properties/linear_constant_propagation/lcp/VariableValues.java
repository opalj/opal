/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValueMatcher;

import java.lang.annotation.*;

/**
 * Container annotation for {@link VariableValue} annotations.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LinearConstantPropagationProperty.KEY, validator = VariableValueMatcher.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface VariableValues {
    VariableValue[] value();
}

