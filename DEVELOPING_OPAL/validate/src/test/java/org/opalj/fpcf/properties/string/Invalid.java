/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * The given string variable does not contain any string analyzable by the string analysis. Usually used as a fallback
 * in low-soundness mode.
 * @author Maximilian Rüsch
 */
@PropertyValidator(key = "StringConstancy", validator = InvalidStringMatcher.class)
@Documented
@Repeatable(Invalids.class)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Invalid {

    int sinkIndex();

    String reason() default "N/A";

    Level[] levels();

    DomainLevel[] domains() default { DomainLevel.L1, DomainLevel.L2 };

    SoundnessMode[] soundness() default { SoundnessMode.LOW, SoundnessMode.HIGH };
}
