/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_analysis;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

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
