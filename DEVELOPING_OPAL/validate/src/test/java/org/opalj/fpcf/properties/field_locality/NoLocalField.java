/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.field_locality;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that the annotated field is not local (if a proper analysis
 * was scheduled).
 *
 * @author Florian Kuebler
 */
@PropertyValidator(key= "FieldLocality", validator = NoLocalFieldMatcher.class)
@Target(ElementType.FIELD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface NoLocalField {
    
    /**
     * Short reasoning of this property
     */
    String value();
}
