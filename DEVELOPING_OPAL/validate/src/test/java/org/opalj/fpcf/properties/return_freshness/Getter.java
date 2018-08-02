/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.return_freshness;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;


@PropertyValidator(key= "ReturnValueFreshness", validator = GetterMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface Getter {
    String value();
}
