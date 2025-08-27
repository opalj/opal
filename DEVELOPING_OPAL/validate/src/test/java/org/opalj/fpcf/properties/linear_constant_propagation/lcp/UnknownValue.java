/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.linear_constant_propagation.UnknownValueMatcher;

import java.lang.annotation.*;

/**
 * Annotation to state that a variables value is unknown.
 *
 * @author Robin Körkemeier
 */
@PropertyValidator(key = LinearConstantPropagationProperty.KEY, validator = UnknownValueMatcher.class)
@Repeatable(UnknownValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface UnknownValue {
    /**
     * The index of this variable in the TAC
     */
    int tacIndex();
}
