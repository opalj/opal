/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValueMatcher;

import java.lang.annotation.*;

/**
 * Annotation to state that a variable has a non-constant value.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LinearConstantPropagationProperty.KEY, validator = VariableValueMatcher.class)
@Repeatable(VariableValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface VariableValue {
    /**
     * The index of this variable in the TAC
     */
    int tacIndex();
}
