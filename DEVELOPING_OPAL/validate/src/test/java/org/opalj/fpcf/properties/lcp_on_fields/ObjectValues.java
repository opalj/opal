/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.lcp_on_fields;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Container annotation for {@link ObjectValue} annotations
 */
@PropertyValidator(key = LCPOnFieldsProperty.KEY, validator = ObjectValueMatcher.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ObjectValues {
    ObjectValue[] value();
}
