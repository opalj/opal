package org.opalj.fpcf.properties.vta;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.vta.ExpectedCalleeMatcher;

import java.lang.annotation.*;

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
