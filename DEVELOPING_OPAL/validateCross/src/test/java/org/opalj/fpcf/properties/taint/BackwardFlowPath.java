/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint;

import org.opalj.fpcf.properties.BackwardFlowPathMatcher;
import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;


/**
 * Array of called methods in a Taint flow, excluding the source but including the sink.
 *
 * @author Mario Trageser
 */
@PropertyValidator(key = BackwardFlowPath.PROPERTY_VALIDATOR_KEY, validator = BackwardFlowPathMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface BackwardFlowPath {

    String PROPERTY_VALIDATOR_KEY = "BackwardFlowPath";

    String[] value();
}
