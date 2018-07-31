/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.static_data_usage;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that a method uses compile-time varying static data.
 *
 * @author Dominik Helm
 */
@PropertyValidator(key = "StaticDataUsage", validator = UsesVaryingDataMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface UsesVaryingData {

    /**
     * A short reasoning of this property.
     */
    String value(); // default = "N/A";
}
