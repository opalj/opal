/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.return_freshness;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that the annotated method has a primitive return type.
 *
 * @author Florian Kuebler
 */
@PropertyValidator(key= "ReturnValueFreshness", validator = PrimitiveReturnValueMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface PrimitiveReturnValue {

    /**
     * Short reasoning of this property
     */
    String value();
}
