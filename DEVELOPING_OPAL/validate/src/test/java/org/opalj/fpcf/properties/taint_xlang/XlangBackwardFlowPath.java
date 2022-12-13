/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint_xlang;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

@PropertyValidator(key = XlangBackwardFlowPath.PROPERTY_VALIDATOR_KEY, validator = XlangBackwardFlowPathMatcher.class)
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface XlangBackwardFlowPath {

    String PROPERTY_VALIDATOR_KEY = "XlangBackwardFlowPath";

    String[] value();
}
