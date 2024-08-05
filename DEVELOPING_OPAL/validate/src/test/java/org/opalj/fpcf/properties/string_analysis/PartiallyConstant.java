/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_analysis;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

@PropertyValidator(key = "StringConstancy", validator = PartiallyConstantStringMatcher.class)
@Documented
@Repeatable(PartiallyConstants.class)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface PartiallyConstant {

    int n();

    String reason() default "N/A";

    /**
     * A regexp like string that describes the element(s) that are expected.
     */
    String value();

    Level[] levels();

    DomainLevel[] domains() default { DomainLevel.L1, DomainLevel.L2 };

    SoundnessMode[] soundness() default { SoundnessMode.LOW, SoundnessMode.HIGH };
}
