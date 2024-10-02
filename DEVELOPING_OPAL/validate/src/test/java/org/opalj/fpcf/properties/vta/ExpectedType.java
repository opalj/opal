/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.vta;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.vta.ExpectedTypeMatcher;

import java.lang.annotation.*;

/**
 * Denotes the expected type class
 * Can be used several times using @List{[...]}
 *
 * @author Marc Clement
 */
public @interface ExpectedType {

    String PROPERTY_VALIDATOR_KEY = "ExpectedType";

    int lineNumber();

    String value();

    boolean upperBound();

    @PropertyValidator(key = ExpectedType.PROPERTY_VALIDATOR_KEY, validator = ExpectedTypeMatcher.class)
    @Target(ElementType.METHOD)
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @interface List {
        ExpectedType[] value();
    }
}
