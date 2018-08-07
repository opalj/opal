/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.return_freshness;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that the annotated method has no fresh return value (if a proper analysis
 * was scheduled).
 *
 * @author Florian Kuebler
 */
@PropertyValidator(key= "ReturnValueFreshness", validator = NoFreshReturnValueMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface NoFreshReturnValue {
    
    /**
     * Short reasoning of this property
     */
    String value();
}
