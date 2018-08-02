/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.field_locality;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that the annotated field is extensible local (if a proper analysis
 * was scheduled).
 *
 * @author Dominik Helm
 */
@PropertyValidator(key= "FieldLocality", validator = ExtensibleLocalFieldMatcher.class)
@Target(ElementType.FIELD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ExtensibleLocalField {
    
    /**
     * Short reasoning of this property
     */
    String value();
}
