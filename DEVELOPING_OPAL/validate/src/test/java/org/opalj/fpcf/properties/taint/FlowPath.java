/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * @author Mario Trageser
 */
@PropertyValidator(key = FlowPath.PROPERTY_VALIDATOR_KEY, validator = FlowPathMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface FlowPath {

    String PROPERTY_VALIDATOR_KEY = "FlowPath";

    String[] value();
}
