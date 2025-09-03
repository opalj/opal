/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.StaticValuesMatcher;

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
    ConstantField[] constantValues() default {};

    /**
     * The non-constant static fields
     */
    VariableField[] variableValues() default {};

    /**
     * The static fields with unknown value
     */
    UnknownField[] unknownValues() default {};
}
