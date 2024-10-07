/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that a variable has a non-constant value
 */
@PropertyValidator(key = LCPOnFieldsProperty.KEY, validator = VariableValueMatcher.class)
@Repeatable(VariableValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface VariableValue {
    /**
     * The name of the variable
     */
    String variable() default "";
}
