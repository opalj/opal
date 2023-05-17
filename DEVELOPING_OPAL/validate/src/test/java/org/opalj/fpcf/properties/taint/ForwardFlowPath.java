/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Array of called methods in a Taint flow, excluding the source and the sink
 *
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
