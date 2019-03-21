package org.opalj.fpcf.properties.taint;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

@PropertyValidator(key = TaintedFlow.PROPERTY_VALIDATOR_KEY, validator = TaintedFlowMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface TaintedFlow {

    String PROPERTY_VALIDATOR_KEY = "TaintedFlow";

    String value();
}
