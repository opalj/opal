/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import java.lang.annotation.*;

/**
 * Note that this annotation will be rewritten into {@link Invalid} or {@link Dynamic} depending on the soundness mode.
 *
 * @see org.opalj.fpcf.fixtures.string.SimpleStringOps
 * @author Maximilian Rüsch
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Failures {

    Failure[] value();
}
