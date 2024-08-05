/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_analysis;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

@PropertyValidator(key = "StringConstancy", validator = InvalidStringMatcher.class)
@Documented
@Repeatable(Invalids.class)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Invalid {

    int n();

    String reason() default "N/A";

    Level[] levels();

    DomainLevel[] domains() default { DomainLevel.L1, DomainLevel.L2 };

    SoundnessMode[] soundness() default { SoundnessMode.LOW, SoundnessMode.HIGH };
}
