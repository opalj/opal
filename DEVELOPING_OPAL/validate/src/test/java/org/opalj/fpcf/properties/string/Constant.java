/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * The given string variable contains only constant strings and its set of possible values is thus enumerable within
 * finite time.
 *
 * @author Maximilian Rüsch
 */
@PropertyValidator(key = "StringConstancy", validator = ConstantStringMatcher.class)
@Documented
@Repeatable(Constants.class)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.LOCAL_VARIABLE })
public @interface Constant {

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
