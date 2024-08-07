/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that a variables value is unknown
 */
@PropertyValidator(key = LinearConstantPropagationProperty.KEY, validator = UnknownValueMatcher.class)
@Repeatable(UnknownValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface UnknownValue {
    /**
     * The name of the variable
     */
    String variable() default "";
}
