/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * The given string variable contains strings which have some constant part concatenated with some dynamic part. Its set
 * of possible values is constrained but not enumerable within finite time.
 *
 * @author Maximilian Rüsch
 */
@PropertyValidator(key = "StringConstancy", validator = PartiallyConstantStringMatcher.class)
@Documented
@Repeatable(PartiallyConstants.class)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface PartiallyConstant {

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
