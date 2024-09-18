/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import java.lang.annotation.*;

/**
 * Note that this annotation will be rewritten into {@link Invalid} or {@link Dynamic} depending on the soundness mode.
 */
@Documented
@Repeatable(Failures.class)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Failure {

    int n();

    Level[] levels();

    String reason() default "N/A";

    DomainLevel[] domains() default { DomainLevel.L1, DomainLevel.L2 };
}
