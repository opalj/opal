/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import java.lang.annotation.*;

/**
 * The given string variables do not contain any string analyzable by the string analysis. Usually used as a fallback in
 * low-soundness mode.
 *
 * @author Maximilian Rüsch
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Invalids {

    Invalid[] value();
}
