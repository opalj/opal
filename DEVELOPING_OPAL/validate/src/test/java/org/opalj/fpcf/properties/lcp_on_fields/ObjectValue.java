/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.UnknownValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;

import java.lang.annotation.*;

/**
 * Annotation to state that an object has been identified and has certain constant and non-constant values
 */
@PropertyValidator(key = LCPOnFieldsProperty.KEY, validator = ObjectValueMatcher.class)
@Repeatable(ObjectValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ObjectValue {
    /**
     * The name of the variable the object is stored in
     */
    String variable();

    /**
     * The constant fields of the object
     */
    ConstantValue[] constantValues() default {};

    /**
     * The non-constant fields of the object
     */
    VariableValue[] variableValues() default {};

    /**
     * The fields of the object with unknown value
     */
    UnknownValue[] unknownValues() default {};
}
