/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import java.lang.annotation.*;

/**
 * The given string variables contain strings which only consist of dynamic information.
 * Their sets of possible values may be constrained but are definitely not enumerable within finite time.
 * Usually used as a fallback in high-soundness mode.
 *
 * @author Maximilian Rüsch
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Dynamics {

    Dynamic[] value();
}
