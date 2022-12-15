/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.taint.ForwardFlowPathMatcher;

import java.lang.annotation.*;

/**
 * @author Mario Trageser
 */
@PropertyValidator(key = ForwardFlowPath.PROPERTY_VALIDATOR_KEY, validator = ForwardFlowPathMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ForwardFlowPath {

    String PROPERTY_VALIDATOR_KEY = "ForwardFlowPath";

    String[] value();
}
