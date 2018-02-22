package org.opalj.fpcf.properties.field_locality;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.return_freshness.FreshReturnValueMatcher;
import org.opalj.fpcf.properties.return_freshness.LocalFieldMatcher;

import java.lang.annotation.*;

/**
 * Annotation to state that the annotated field is local (if a proper analysis
 * was scheduled).
 *
 * @author Florian Kuebler
 */
@PropertyValidator(key= "FieldLocality", validator = LocalFieldMatcher.class)
@Target(ElementType.FIELD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface LocalField {
    
    /**
     * Short reasoning of this property
     */
    String value();
}
