/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.StaticValuesMatcher;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.UnknownValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue;

import java.lang.annotation.*;

/**
 * Annotation to state that an object has certain constant and non-constant static values.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LCPOnFieldsProperty.KEY, validator = StaticValuesMatcher.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface StaticValues {
    /**
     * The constant static fields
     */
    ConstantValue[] constantValues() default {};

    /**
     * The non-constant static fields
     */
    VariableValue[] variableValues() default {};

    /**
     * The static fields with unknown value
     */
    UnknownValue[] unknownValues() default {};
}
