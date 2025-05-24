/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValueMatcher;

import java.lang.annotation.*;

/**
 * Annotation to state that a variable has a constant value.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LinearConstantPropagationProperty.KEY, validator = ConstantValueMatcher.class)
@Repeatable(ConstantValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ConstantValue {
    /**
     * The PC of this variable in the TAC
     */
    int pc();

    /**
     * The constant value of the variable
     */
    int value();
}
