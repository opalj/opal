/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.field_locality;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

@PropertyValidator(key= "FieldLocality", validator = LocalFieldWithGetterMatcher.class)
@Target(ElementType.FIELD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface LocalFieldWithGetter {
    
    /**
     * Short reasoning of this property
     */
    String value();
}
