/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.ObjectValueMatcher;

import java.lang.annotation.*;

/**
 * Annotation to state that an object has been identified and has certain constant and non-constant values.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LCPOnFieldsProperty.KEY, validator = ObjectValueMatcher.class)
@Repeatable(ObjectValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ObjectValue {
    /**
     * The index in the TAC of the variable the object is stored in
     */
    int tacIndex();

    /**
     * The constant fields of the object
     */
    ConstantField[] constantValues() default {};

    /**
     * The non-constant fields of the object
     */
    VariableField[] variableValues() default {};

    /**
     * The fields of the object with unknown value
     */
    UnknownField[] unknownValues() default {};
}
