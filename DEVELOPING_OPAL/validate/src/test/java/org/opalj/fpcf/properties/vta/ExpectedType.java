package org.opalj.fpcf.properties.vta;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.vta.ExpectedTypeMatcher;

import java.lang.annotation.*;

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
