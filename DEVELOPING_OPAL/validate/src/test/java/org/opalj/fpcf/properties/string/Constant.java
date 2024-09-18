/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * @see org.opalj.fpcf.fixtures.string.SimpleStringOps
 * @author Maximilian Rüsch
 */
@PropertyValidator(key = "StringConstancy", validator = ConstantStringMatcher.class)
@Documented
@Repeatable(Constants.class)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.LOCAL_VARIABLE })
public @interface Constant {

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
