/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.static_data_usage;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.compile_time_constancy.CompileTimeConstantMatcher;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that a method uses no static data.
 *
 * @author Dominik Helm
 */
@PropertyValidator(key = "StaticDataUsage", validator = UsesNoStaticDataMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface UsesNoStaticData {

    /**
     * A short reasoning of this property.
     */
    String value(); // default = "N/A";
}
