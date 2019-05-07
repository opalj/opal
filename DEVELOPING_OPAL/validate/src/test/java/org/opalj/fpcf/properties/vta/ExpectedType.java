package org.opalj.fpcf.properties.vta;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

@PropertyValidator(key = ExpectedType.PROPERTY_VALIDATOR_KEY, validator = VariableTypeMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ExpectedType {

    String PROPERTY_VALIDATOR_KEY = "ExpectedType";

    String[] value();
}
