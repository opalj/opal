/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_analysis;

import java.lang.annotation.*;

/**
 * A test method can contain > 1 triggers for analyzing a variable. Thus, multiple results are
 * expected. This annotation is a wrapper for these expected results. For further information see
 * {@link StringDefinitions}.
 *
 * @author Patrick Mell
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface StringDefinitionsCollection {

    /**
     * A short reasoning of this property.
     */
    String value() default "N/A";

    /**
     * The expected results in the correct order.
     */
    StringDefinitions[] stringDefinitions();

}
