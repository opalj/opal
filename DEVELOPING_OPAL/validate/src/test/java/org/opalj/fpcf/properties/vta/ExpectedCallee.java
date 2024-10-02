/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.vta;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.vta.ExpectedCalleeMatcher;

import java.lang.annotation.*;

/**
 * Denotes the expected callee class.
 * Can be used several times using @List{[...]}
 *
 * @author Marc Clement
 */
public @interface ExpectedCallee {

    String PROPERTY_VALIDATOR_KEY = "ExpectedCallee";

    int lineNumber();

    String value();

    boolean upperBound();

    @PropertyValidator(key = ExpectedCallee.PROPERTY_VALIDATOR_KEY, validator = ExpectedCalleeMatcher.class)
    @Target(ElementType.METHOD)
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @interface List {
        ExpectedCallee[] value();
    }
}
