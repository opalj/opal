package org.opalj.fpcf.properties.return_freshness;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

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
