/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import java.lang.annotation.*;

/**
 * @see org.opalj.fpcf.fixtures.string.SimpleStringOps
 * @author Maximilian Rüsch
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Failures {

    Failure[] value();
}
