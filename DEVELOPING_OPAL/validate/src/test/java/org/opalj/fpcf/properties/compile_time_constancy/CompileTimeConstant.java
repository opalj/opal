/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.compile_time_constancy;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that a field is a compile time constant.
 *
 * @author Dominik Helm
 */
@PropertyValidator(key = "CompileTimeConstancy", validator = CompileTimeConstantMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface CompileTimeConstant {

    /**
     * A short reasoning of this property.
     */
    String value(); // default = "N/A";
}
