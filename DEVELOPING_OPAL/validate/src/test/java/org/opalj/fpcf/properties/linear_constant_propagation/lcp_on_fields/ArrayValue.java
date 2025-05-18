/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.ArrayValueMatcher;

import java.lang.annotation.*;

/**
 * Annotation to state that an array has been identified and has certain constant and non-constant elements.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LCPOnFieldsProperty.KEY, validator = ArrayValueMatcher.class)
@Repeatable(ArrayValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ArrayValue {
    /**
     * The name of the variable the array is stored in
     */
    String variable();

    /**
     * The constant elements of the array
     */
    ConstantArrayElement[] constantElements() default {};

    /**
     * The non-constant elements of the array
     */
    VariableArrayElement[] variableElements() default {};

    /**
     * The elements of the array with unknown value
     */
    UnknownArrayElement[] unknownElements() default {};
}
