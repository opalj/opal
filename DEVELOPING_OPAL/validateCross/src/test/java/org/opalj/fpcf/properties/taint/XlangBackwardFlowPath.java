/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.XlangBackwardFlowPathMatcher;

import java.lang.annotation.*;

/**
 * Array of called methods in a cross language taint flow, excluding the source but including the sink.
 *
 * @author Nicolas Gross
 */
@PropertyValidator(key = XlangBackwardFlowPath.PROPERTY_VALIDATOR_KEY, validator = XlangBackwardFlowPathMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface XlangBackwardFlowPath {

    String PROPERTY_VALIDATOR_KEY = "XlangBackwardFlowPath";

    String[] value();
}
