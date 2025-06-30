/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * The given string variable contains strings which only consist of dynamic information.
 * Its set of possible values may be constrained but is definitely not enumerable within finite time.
 * Usually used as a fallback in high-soundness mode.
 *
 * @author Maximilian Rüsch
 */
@PropertyValidator(key = "StringConstancy", validator = DynamicStringMatcher.class)
@Documented
@Repeatable(Dynamics.class)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Dynamic {

    int sinkIndex();

    String reason() default "N/A";

    /**
     * A regexp like string that describes the element(s) that are expected.
     */
    String value();

    Level[] levels();

    DomainLevel[] domains() default { DomainLevel.L1, DomainLevel.L2 };

    SoundnessMode[] soundness() default { SoundnessMode.LOW, SoundnessMode.HIGH };
}
