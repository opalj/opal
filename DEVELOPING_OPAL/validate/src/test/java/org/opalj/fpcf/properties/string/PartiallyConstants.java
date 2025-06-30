/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import java.lang.annotation.*;

/**
 * The given string variables contain strings which have some constant part concatenated with some dynamic part. Their
 * sets of possible values are constrained but not enumerable within finite time.
 *
 * @author Maximilian Rüsch
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface PartiallyConstants {

    PartiallyConstant[] value();
}
